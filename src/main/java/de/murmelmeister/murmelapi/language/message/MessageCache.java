package de.murmelmeister.murmelapi.language.message;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MessageCache is a thread-safe cache for storing messages by their ID and tag.
 * It allows for quick retrieval and management of messages based on their unique identifiers.
 */
public final class MessageCache {
    private final ConcurrentHashMap<Integer, Message> byId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<Integer, Message>> byTag = new ConcurrentHashMap<>();

    public boolean containsKeyById(int id) {
        return byId.containsKey(id);
    }

    public boolean containsKeyByTag(String tag, int languageId) {
        return byTag.containsKey(tag) && byTag.get(tag).containsKey(languageId);
    }

    public Message getById(int id) {
        return byId.get(id);
    }

    public Message getByTag(String tag, int languageId) {
        ConcurrentHashMap<Integer, Message> messages = byTag.get(tag);
        return messages != null ? messages.get(languageId) : null;
    }

    public void put(Message message) {
        byId.put(message.getId(), message);
        byTag.computeIfAbsent(message.getTag(), k -> new ConcurrentHashMap<>())
                .put(message.getLanguageId(), message);
    }

    public void remove(int id) {
        Message removed = byId.remove(id);
        if (removed != null) {
            String tag = removed.getTag();
            removeByTag(tag, removed.getLanguageId());
        }
    }

    public void removeByTag(String tag, int languageId) {
        ConcurrentHashMap<Integer, Message> messages = byTag.get(tag);
        if (messages != null) {
            messages.remove(languageId);
            if (messages.isEmpty())
                byTag.remove(tag);
        }
    }

    public void clear() {
        byId.clear();
        byTag.clear();
    }

    public List<Message> getMessages() {
        return List.copyOf(byId.values());
    }
}
