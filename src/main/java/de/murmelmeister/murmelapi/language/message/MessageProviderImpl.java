package de.murmelmeister.murmelapi.language.message;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

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
        database.createTable(TABLE_NAME, "id INT PRIMARY KEY AUTO_INCREMENT, " +
                                         "tag_id VARCHAR(255), " +
                                         "language_id INT, " +
                                         "UNIQUE (tag, language_id), " +
                                         "message TEXT, " +
                                         "FOREIGN KEY (language_id) REFERENCES languages(id)");
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
        if (tagId == null || languageId < 1 || (message == null || message.isEmpty()))
            return null;

        tagId = tagId.strip();
        if (tagId.isEmpty()) return null;

        String sql = "INSERT INTO " + TABLE_NAME + " (tag_id, language_id, message) VALUES (?, ?, ?)";
        int id = (int) database.updateWithGeneratedKeys(sql, tagId, languageId, message);
        if (id < 1) return null;

        Message msg = new Message(id, tagId, languageId, message);
        RefreshUtil.fireSingle(single, msg.id());
        cache.put(msg);
        return msg;
    }

    @Override
    public int delete(int id) {
        if (id < 1) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        int row = database.update(sql, id);
        if (row < 1) return 0;

        cache.remove(id);
        RefreshUtil.fireSingle(single, id);
        return row;
    }

    @Override
    public int delete(String tagId, int languageId) {
        if (tagId == null || languageId < 1) return 0;

        tagId = tagId.strip();
        if (tagId.isEmpty()) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE tag_id = ? AND language_id = ?";
        int row = database.update(sql, tagId, languageId);
        if (row < 1) return 0;

        cache.removeByTag(tagId, languageId);
        RefreshUtil.fireSingle(single, new MessageCache.TagKey(tagId, languageId));
        return row;
    }

    @Override
    public int deleteAll(int languageId) {
        if (languageId < 1) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE language_id = ?";
        int row = database.update(sql, languageId);
        if (row < 1) return 0;

        cache.removeByLanguage(languageId);
        RefreshUtil.fireSingle(single, new MessageCache.LanguageKey(languageId));
        return row;
    }

    @Override
    public Message update(int id, String tagId, int languageId, String message) {
        if (id < 1 || tagId == null || languageId < 1 || (message == null || message.isEmpty()))
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
        int rows = database.update(sql, tagId, languageId, message, id);
        if (rows < 1) return null;

        Message msg = existing.withUpdateMeta(tagId, languageId, message);
        cache.put(msg);
        RefreshUtil.fireSingle(single, id);
        return msg;
    }
}
