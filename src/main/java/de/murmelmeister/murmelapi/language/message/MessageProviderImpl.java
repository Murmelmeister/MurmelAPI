package de.murmelmeister.murmelapi.language.message;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

public final class MessageProviderImpl implements MessageProvider {
    private static final String TABLE_NAME = "messages";

    private final Database database;
    private final MessageCache cache;
    private final RefreshType all = RefreshType.MESSAGES;
    private final RefreshType single = RefreshType.SINGLE_MESSAGE;

    public MessageProviderImpl(Database database, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.cache = new MessageCache(database, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, " +
                "tag_id VARCHAR(255) NOT NULL, " +
                "language_id INT NOT NULL, " +
                "UNIQUE (tag_id, language_id), " +
                "message TEXT NOT NULL, " +
                "FOREIGN KEY (language_id) REFERENCES languages(id) ON DELETE CASCADE");
    }

    @Override
    public void closeCache() {
        cache.close();
    }

    @Override
    public void refreshCache() {
        RefreshUtil.fireCache(all);
    }

    @Override
    public Message get(int messageId) {
        return cache.getById(messageId);
    }

    @Override
    public Message get(String tagId, int languageId) {
        return cache.getByTag(tagId, languageId);
    }

    @Override
    public List<Message> getAllMessages(int languageId) {
        return cache.getByLanguage(languageId);
    }

    @Override
    public Message create(String tagId, int languageId, String message) {
        if (tagId == null || languageId < 1 || (message == null || message.isBlank()))
            return null;

        tagId = tagId.strip();
        if (tagId.isEmpty()) return null;

        String sql = "INSERT INTO " + TABLE_NAME + " (tag_id, language_id, message) VALUES (?, ?, ?)";
        String finalTagId = tagId;
        int id = (int) database.updateAndGetGeneratedKeys(sql, stmt -> {
            stmt.setString(1, finalTagId);
            stmt.setInt(2, languageId);
            stmt.setString(3, message);
        });
        if (id < 1) return null;

        Message msg = new Message(id, tagId, languageId, message);
        RefreshUtil.fireSingle(single, msg.id());
        return msg;
    }

    @Override
    public int[] createAll(Properties properties) {
        if (properties == null || properties.isEmpty())
            throw new IllegalArgumentException("missing properties file");

        String language = properties.getProperty("language.id");
        if (language == null || language.isBlank())
            throw new IllegalArgumentException("missing language.id property");

        int languageId;
        try {
            languageId = Integer.parseInt(language);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid language.id property is not a integer", e);
        }

        String sql = "INSERT IGNORE INTO " + TABLE_NAME + " (tag_id, language_id, message) VALUES (?, ?, ?)";
        int[] result = database.updateBatch(sql, stmt -> {
            for (String tagId : properties.stringPropertyNames()) {
                if (tagId.isBlank() || tagId.startsWith("#") || tagId.equals("language.id")) continue;
                String msg = properties.getProperty(tagId);
                if (msg == null || msg.isBlank()) continue;

                stmt.setString(1, tagId);
                stmt.setInt(2, languageId);
                stmt.setString(3, msg);
                stmt.addBatch();
            }
        });

        if (result != null && result.length > 0)
            RefreshUtil.fireCache(all);
        return result;
    }

    @Override
    public int delete(int id) {
        if (id < 1) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        int row = database.update(sql,
                stmt -> stmt.setInt(1, id));
        if (row < 1) return 0;

        RefreshUtil.fireSingle(single, id);
        return row;
    }

    @Override
    public int delete(String tagId, int languageId) {
        if (tagId == null || languageId < 1) return 0;

        tagId = tagId.strip();
        if (tagId.isEmpty()) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE tag_id = ? AND language_id = ?";
        String finalTagId = tagId;
        int row = database.update(sql, stmt -> {
            stmt.setString(1, finalTagId);
            stmt.setInt(2, languageId);
        });
        if (row < 1) return 0;

        RefreshUtil.fireSingle(single, new MessageCache.TagKey(tagId, languageId));
        return row;
    }

    @Override
    public int deleteAll(int languageId) {
        if (languageId < 1) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE language_id = ?";
        int row = database.update(sql,
                stmt -> stmt.setInt(1, languageId));
        if (row < 1) return 0;

        RefreshUtil.fireSingle(single, new MessageCache.LanguageKey(languageId));
        return row;
    }

    @Override
    public Message update(int id, String tagId, int languageId, String message) {
        if (id < 1 || tagId == null || languageId < 1 || (message == null || message.isBlank()))
            return null;

        tagId = tagId.strip();
        if (tagId.isEmpty()) return null;

        Message existing = cache.getById(id);
        if (existing == null) return null;

        if (Objects.equals(tagId, existing.tagId()) &&
                languageId == existing.languageId() &&
                Objects.equals(message, existing.message()))
            return existing; // No changes, return existing

        String sql = "UPDATE " + TABLE_NAME + " SET tag_id = ?, language_id = ?, message = ? WHERE id = ?";
        String finalTagId = tagId;
        int rows = database.update(sql, stmt -> {
            stmt.setString(1, finalTagId);
            stmt.setInt(2, languageId);
            stmt.setString(3, message);
            stmt.setInt(4, id);
        });
        if (rows < 1) return null;

        Message msg = existing.withUpdateMeta(tagId, languageId, message);
        RefreshUtil.fireSingle(single, id);
        return msg;
    }

    @Override
    public int[] updateAll(Properties properties) {
        if (properties == null || properties.isEmpty())
            throw new IllegalArgumentException("missing properties file");

        String language = properties.getProperty("language.id");
        if (language == null || language.isBlank())
            throw new IllegalArgumentException("missing language.id property");

        int languageId;
        try {
            languageId = Integer.parseInt(language);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid language.id property is not a integer", e);
        }

        String sql = "UPDATE " + TABLE_NAME + " SET message = ? WHERE tag_id = ? AND language_id = ?";
        int[] result = database.updateBatch(sql, stmt -> {
            for (String tagId : properties.stringPropertyNames()) {
                if (tagId.isBlank() || tagId.startsWith("#") || tagId.equals("language.id")) continue;
                String msg = properties.getProperty(tagId);
                if (msg == null || msg.isBlank()) continue;

                stmt.setString(1, msg);
                stmt.setString(2, tagId);
                stmt.setInt(3, languageId);
                stmt.addBatch();
            }
        });

        if (result != null && result.length > 0)
            RefreshUtil.fireCache(all);
        return result;
    }
}
