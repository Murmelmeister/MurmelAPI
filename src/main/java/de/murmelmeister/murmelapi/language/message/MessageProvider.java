package de.murmelmeister.murmelapi.language.message;

import de.murmelmeister.murmelapi.database.Database;

import java.util.List;

/**
 * Provides CRUD operations for {@link Message} objects backed by a relational database
 * and an in-memory cache.
 * <p>
 * This class encapsulates all SQL interactions for the “messages” table, maintains
 * a {@link MessageCache} for fast lookups, and exposes methods to create, read,
 * update, and delete message records. It also offers a convenience method to
 * initialize the table schema.
 * </p>
 */
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

    /**
     * Loads all messages from the database into the cache.
     * <p>
     * Clears any existing cache state, fetches every row from the "messages" table,
     * constructs a {@link Message} for each record, populates the cache, and returns
     * the complete list.
     * </p>
     *
     * @return A list of all {@link Message} instances currently persisted
     */
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

    /**
     * Retrieves a message by its unique database ID from the cache.
     *
     * @param id The primary key of the message
     * @return The cached {@link Message}, or {@code null} if not found
     */
    public Message getMessage(int id) {
        return cache.getById(id);
    }

    /**
     * Retrieves a message by its logical tag and language ID from the cache.
     *
     * @param tag        The message tag (e.g. "PLAY_TIME_COMMAND_USE")
     * @param languageId The numerical language identifier
     * @return The cached {@link Message}, or {@code null} if {@code tag} is null/empty or not present in cache
     */
    public Message getMessage(String tag, int languageId) {
        if (tag == null || tag.isEmpty()) return null;
        return cache.getByTag(tag, languageId);
    }

    /**
     * Checks whether a message with the given ID exists in the cache.
     *
     * @param id The primary key to check
     * @return {@code true} if present, {@code false} otherwise
     */
    public boolean existsMessage(int id) {
        return cache.containsKeyById(id);
    }

    /**
     * Checks whether a message with the given tag and language ID exists in the cache.
     *
     * @param tag        The message tag to check
     * @param languageId The language identifier to check
     * @return {@code true} if present and {@code tag} is non-null/non-empty; {@code false} otherwise
     */
    public boolean existsMessage(String tag, int languageId) {
        if (tag == null || tag.isEmpty()) return false;
        return cache.containsKeyByTag(tag, languageId);
    }

    /**
     * Inserts a new message record into the database and updates the cache.
     * <p>
     * Validates that {@code tag} and {@code message} are non-null and non-empty.
     * Upon successful insertion, returns the newly created {@link Message} with
     * its generated ID, and stores it in the cache.
     * </p>
     *
     * @param tag        The message tag (must not be null or empty)
     * @param languageId The language identifier
     * @param message    The localized message text (must not be null or empty)
     * @return The newly created {@link Message}, or {@code null} if validation fails or insertion did not succeed
     */
    public Message createMessage(String tag, int languageId, String message) {
        if (tag == null || tag.isEmpty() || message == null || message.isEmpty()) return null;
        String sql = "INSERT INTO " + TABLE_NAME + " (tag, languageId, message) VALUES (?, ?, ?)";
        int id = database.updateAndGetAutoIncrement(sql, tag, languageId, message);
        if (id < 1) return null;
        Message msg = new Message(id, tag, languageId, message);
        cache.put(msg);
        return msg;
    }

    /**
     * Deletes a message by its ID from both the database and the cache.
     *
     * @param id The primary key of the message to delete
     * @return The number of rows affected (0 if {@code id < 1} or no record was deleted)
     */
    public int deleteMessage(int id) {
        if (id < 1) return 0;
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id=?";
        int affectedRow = database.update(sql, id);
        cache.remove(id);
        return affectedRow;
    }

    /**
     * Updates an existing message record in the database and refreshes the cache entry.
     * <p>
     * Validates all inputs, performs the SQL update, and if successful,
     * updates the cached {@link Message} instance (or creates a new one in cache
     * if it was not already present).
     * </p>
     *
     * @param id         The primary key of the message to update (must be ≥ 1)
     * @param tag        The new message tag (must not be null or empty)
     * @param languageId The new language identifier (must be ≥ 1)
     * @param message    The new localized text (must not be null or empty)
     * @return The updated {@link Message} from cache, or {@code null} if validation fails
     */
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
