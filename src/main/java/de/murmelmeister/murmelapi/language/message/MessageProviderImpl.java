package de.murmelmeister.murmelapi.language.message;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;
import java.util.*;

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
        String normalizedTagId = StringUtil.normalize(tagId);
        if (normalizedTagId == null || languageId < 1 || (message == null || message.isBlank()))
            return null;

        String sql = "INSERT INTO " + TABLE_NAME + " (tag_id, language_id, message) VALUES (?, ?, ?)";
        int id = (int) database.updateAndGetGeneratedKeys(sql, stmt -> {
            stmt.setString(1, normalizedTagId);
            stmt.setInt(2, languageId);
            stmt.setString(3, message);
        });
        if (id < 1) return null;

        Message msg = new Message(id, normalizedTagId, languageId, message);
        RefreshUtil.fireSingle(single, msg.id());
        return msg;
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
        String normalizedTagId = StringUtil.normalize(tagId);
        if (normalizedTagId == null || languageId < 1) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE tag_id = ? AND language_id = ?";
        int row = database.update(sql, stmt -> {
            stmt.setString(1, normalizedTagId);
            stmt.setInt(2, languageId);
        });
        if (row < 1) return 0;

        RefreshUtil.fireSingle(single, new MessageCache.TagKey(normalizedTagId, languageId));
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
        String normalizedTagId = StringUtil.normalize(tagId);
        if (id < 1 || normalizedTagId == null || languageId < 1 || (message == null || message.isBlank()))
            return null;

        Message existing = cache.getById(id);
        if (existing == null) return null;

        if (Objects.equals(normalizedTagId, existing.tagId()) &&
                languageId == existing.languageId() &&
                Objects.equals(message, existing.message()))
            return existing; // No changes, return existing

        String sql = "UPDATE " + TABLE_NAME + " SET tag_id = ?, language_id = ?, message = ? WHERE id = ?";
        int rows = database.update(sql, stmt -> {
            stmt.setString(1, normalizedTagId);
            stmt.setInt(2, languageId);
            stmt.setString(3, message);
            stmt.setInt(4, id);
        });
        if (rows < 1) return null;

        Message msg = existing.withUpdateMeta(normalizedTagId, languageId, message);
        RefreshUtil.fireSingle(single, id);
        return msg;
    }

    @Override
    public int[] upsertAll(Properties properties) {
        return upsertInternal(properties, true);
    }

    @Override
    public int[] upsertAll(Collection<Properties> properties) {
        if (properties == null || properties.isEmpty())
            throw new IllegalArgumentException("Missing properties collection");

        List<int[]> results = new ArrayList<>();
        boolean changed = false;
        for (Properties props : properties) {
            int[] result = upsertInternal(props, false);
            if (result.length > 0) {
                results.add(result);
                changed = true;
            }
        }

        if (changed)
            RefreshUtil.fireCache(all);

        int total = results.stream().mapToInt(arr -> arr.length).sum();
        int[] merged = new int[total];
        int offset = 0;
        for (int[] arr : results) {
            System.arraycopy(arr, 0, merged, offset, arr.length);
            offset += arr.length;
        }
        return merged;
    }

    @Override
    public int[] createOrUpdateAll(Properties properties) {
        return upsertInternal(properties, true);
    }

    private int[] upsertInternal(Properties properties, boolean fireCache) {
        if (properties == null || properties.isEmpty())
            throw new IllegalArgumentException("Missing properties file");

        String language = properties.getProperty("language.id");
        if (language == null || language.isBlank())
            throw new IllegalArgumentException("Missing language.id property");

        int languageId;
        try {
            languageId = Integer.parseInt(language);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid language.id property is not a integer", e);
        }

        // Load existing messages for the language at once
        String selectSql = "SELECT tag_id, message FROM " + TABLE_NAME + " WHERE language_id = ?";
        Map<String, String> existing = new HashMap<>();
        database.queryList(selectSql, rs -> Map.entry(rs.getString(1), rs.getString(2)), stmt -> stmt.setInt(1, languageId))
                .forEach(entry -> existing.put(entry.getKey(), entry.getValue()));

        // Batch only changed rows for update
        String updateSql = "UPDATE " + TABLE_NAME + " SET message = ? WHERE tag_id = ? AND language_id = ?";
        int[] updated = database.updateBatch(updateSql, stmt -> {
            for (String tagId : properties.stringPropertyNames()) {
                if (tagId.isBlank() || tagId.startsWith("#") || tagId.equals("language.id")) continue;
                String msg = properties.getProperty(tagId);
                if (msg == null || msg.isBlank()) continue;

                String current = existing.get(tagId);
                if (current == null) continue; // missing -> insert later
                if (current.equals(msg)) continue; // no change

                stmt.setString(1, msg);
                stmt.setString(2, tagId);
                stmt.setInt(3, languageId);
                stmt.addBatch();
            }
        });

        // Batch only truly new rows for insert
        String insertSql = "INSERT INTO " + TABLE_NAME + " (tag_id, language_id, message) VALUES (?, ?, ?)";
        int[] inserted = database.updateBatch(insertSql, stmt -> {
            for (String tagId : properties.stringPropertyNames()) {
                if (tagId.isBlank() || tagId.startsWith("#") || tagId.equals("language.id")) continue;
                String msg = properties.getProperty(tagId);
                if (msg == null || msg.isBlank()) continue;

                if (existing.containsKey(tagId)) continue; // already present

                stmt.setString(1, tagId);
                stmt.setInt(2, languageId);
                stmt.setString(3, msg);
                stmt.addBatch();
            }
        });

        int totalOps = (updated == null ? 0 : updated.length) + (inserted == null ? 0 : inserted.length);
        if (fireCache && totalOps > 0)
            RefreshUtil.fireCache(all);

        // Merge counts for compatibility
        int updateLen = updated == null ? 0 : updated.length;
        int insertLen = inserted == null ? 0 : inserted.length;
        int[] result = new int[updateLen + insertLen];
        if (updated != null) System.arraycopy(updated, 0, result, 0, updateLen);
        if (inserted != null) System.arraycopy(inserted, 0, result, updateLen, insertLen);
        return result;
    }
}
