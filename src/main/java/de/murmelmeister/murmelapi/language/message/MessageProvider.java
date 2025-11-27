package de.murmelmeister.murmelapi.language.message;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

public interface MessageProvider {
    void closeCache();

    void refreshCache();

    Message get(int messageId);

    Message get(String tagId, int languageId);

    List<Message> getAllMessages(int languageId);

    Message create(String tagId, int languageId, String message);

    int delete(int id);

    int delete(String tagId, int languageId);

    int deleteAll(int languageId);

    Message update(int id, String tagId, int languageId, String message);

    /**
     * Upserts all entries from a single properties file.
     */
    int[] upsertAll(Properties properties);

    /**
     * Upserts all entries from multiple properties files (e.g., multiple languages).
     */
    int[] upsertAll(Collection<Properties> properties);

    /**
     * Backwards-compatible alias.
     */
    default int[] createOrUpdateAll(Properties properties) {
        return upsertAll(properties);
    }
}
