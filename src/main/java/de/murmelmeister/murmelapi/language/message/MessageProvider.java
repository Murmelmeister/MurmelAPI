package de.murmelmeister.murmelapi.language.message;

import java.util.List;

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
}
