package de.murmelmeister.murmelapi.language;

import de.murmelmeister.murmelapi.database.Database;

import java.util.List;

public final class MessageProvider {
    private static final String TABLE_NAME = "messages";
    private final MessageCache cache = new MessageCache();
    private final Database database;

    public MessageProvider(Database database) {
        this.database = database;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "id INT PRIMARY KEY AUTO_INCREMENT, " +
                                         "tag VARCHAR(255), " +
                                         "languageId INT, FOREIGN KEY (languageId) REFERENCES languages(id), " +
                                         "UNIQUE (tag, languageId), " +
                                         "message TEXT");
    }

    public List<Message> loadData() {
        cache.clear();
        String sql = "SELECT id, tag, languageId, message FROM " + TABLE_NAME;
        List<Message> messages = database.queryList(sql, result -> {
            int id = result.getInt("id");
            String tag = result.getString("tag");
            int languageId = result.getInt("languageId");
            String message = result.getString("message");
            return new Message(id, tag, languageId, message);
        });

        messages.forEach(cache::put);
        return messages;
    }

    public Message getMessage(int id) {
        return cache.getById(id);
    }

    public Message getMessage(String tag, int languageId) {
        if (tag == null || tag.isEmpty()) return null;
        return cache.getByTag(tag, languageId);
    }

    public boolean existsMessage(int id) {
        return cache.containsKeyById(id);
    }

    public boolean existsMessage(String tag, int languageId) {
        if (tag == null || tag.isEmpty()) return false;
        return cache.containsKeyByTag(tag, languageId);
    }

    public Message createMessage(String tag, int languageId, String message) {
        if (tag == null || tag.isEmpty() || message == null || message.isEmpty()) return null;
        String sql = "INSERT INTO " + TABLE_NAME + " (tag, languageId, message) VALUES (?, ?, ?)";
        int id = database.updateAndGetAutoIncrement(sql, tag, languageId, message);
        if (id < 1) return null;
        Message msg = new Message(id, tag, languageId, message);
        cache.put(msg);
        return msg;
    }

    public int deleteMessage(int id) {
        if (id < 1) return 0;
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id=?";
        int affectedRow = database.update(sql, id);
        cache.remove(id);
        return affectedRow;
    }

    public Message updateMessage(int id, String tag, int languageId, String message) {
        if (id < 1 || tag == null || tag.isEmpty() || languageId < 1 || message == null || message.isEmpty())
            return null;
        String sql = "UPDATE " + TABLE_NAME + " SET tag=?, languageId=?, message=? WHERE id=?";
        int affectedRow = database.update(sql, tag, languageId, message, id);
        if (affectedRow > 0) {
            Message msg = cache.getById(id);
            if (msg != null) {
                msg.setTag(tag);
                msg.setLanguageId(languageId);
                msg.setMessage(message);
            } else {
                msg = new Message(id, tag, languageId, message);
            }
            cache.put(msg);
        }
        return cache.getById(id);
    }
}
