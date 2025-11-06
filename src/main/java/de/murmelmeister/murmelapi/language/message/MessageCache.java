package de.murmelmeister.murmelapi.language.message;

import com.github.benmanes.caffeine.cache.LoadingCache;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.CacheUtil;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshListener;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MessageCache is a thread-safe cache for storing messages by their ID and tag.
 * It allows for quick retrieval and management of messages based on their unique identifiers.
 */
public class MessageCache implements RefreshListener, AutoCloseable {
    private static final Pattern LANGUAGE_KEY_PATTERN = Pattern.compile("^LanguageKey\\[languageId=(\\d+)]$");
    private static final Pattern TAG_KEY_PATTERN = Pattern.compile("^TagKey\\[tagId=(\\w+), languageId=(\\d+)]$");

    private final Database database;
    private final String tableName;
    private final LoadingCache<Integer, Message> cacheById;
    private final LoadingCache<TagKey, Message> cacheByTag;
    private final LoadingCache<LanguageKey, List<Message>> cacheByLanguage;
    private final Long fetchLimit;

    public MessageCache(Database database, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheById = CacheUtil.buildCacheRefresh(this::loadById, cacheCapacity, refreshInterval);
        this.cacheByTag = CacheUtil.buildCacheRefresh(this::loadByTag, cacheCapacity, refreshInterval);
        this.cacheByLanguage = CacheUtil.buildCacheRefresh(key -> loadByLanguage(key.languageId()), cacheCapacity, refreshInterval);
        RefreshUtil.register(this);
    }

    @Override
    public void onRefresh(RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.MESSAGES.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName))
            refreshAll();
        else if (RefreshType.SINGLE_MESSAGE.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (!(key instanceof String)) {
                if (key instanceof Integer id)
                    refreshSingle(id);
                else if (key instanceof TagKey tagKey)
                    refreshSingle(tagKey);
                else if (key instanceof LanguageKey languageKey)
                    refreshSingle(languageKey);
            } else {
                Matcher languageMatcher = LANGUAGE_KEY_PATTERN.matcher((String) key);
                Matcher tagMatcher = TAG_KEY_PATTERN.matcher((String) key);
                if (languageMatcher.matches()) {
                    int languageId = Integer.parseInt(languageMatcher.group(1));
                    refreshSingle(new LanguageKey(languageId));
                } else if (tagMatcher.matches()) {
                    String tagId = tagMatcher.group(1);
                    int languageId = Integer.parseInt(tagMatcher.group(2));
                    refreshSingle(new TagKey(tagId, languageId));
                } else {
                    int id = Integer.parseInt((String) key);
                    refreshSingle(id);
                }
            }
        }
    }

    @Override
    public void close() {
        RefreshUtil.unregister(this);
        clear();
    }

    private void refreshAll() {
        clear();
        List<Message> messages = loadAllFromDatabase();
        if (messages.isEmpty())
            return;

        Map<LanguageKey, List<Message>> byLanguage = new HashMap<>();
        for (Message message : messages) {
            cacheById.put(message.id(), message);
            cacheByTag.put(new TagKey(message.tagId(), message.languageId()), message);
            LanguageKey languageKey = new LanguageKey(message.languageId());
            byLanguage.computeIfAbsent(languageKey, ignored -> new ArrayList<>()).add(message);
        }

        byLanguage.forEach((key, value) -> cacheByLanguage.put(key, List.copyOf(value)));
    }

    private void refreshSingle(LanguageKey key) {
        removeByLanguage(key.languageId());
        List<Message> messages = loadByLanguage(key.languageId());
        if (messages.isEmpty())
            return;

        for (Message message : messages) {
            cacheById.put(message.id(), message);
            cacheByTag.put(new TagKey(message.tagId(), message.languageId()), message);
        }
        cacheByLanguage.put(key, List.copyOf(messages));
    }

    private void refreshSingle(TagKey key) {
        removeByTag(key.tagId(), key.languageId());
        Message message = loadByTag(key);
        if (message != null)
            put(message);
    }

    private void refreshSingle(int id) {
        remove(id);
        Message message = loadById(id);
        if (message != null)
            put(message);
    }

    private List<Message> loadAllFromDatabase() {
        String sql = "SELECT * FROM " + tableName;
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.message());
    }

    private List<Message> loadByLanguage(int languageId) {
        String sql = "SELECT * FROM " + tableName + " WHERE language_id = ?";
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.message(),
                stmt -> stmt.setInt(1, languageId));
    }

    private Message loadByTag(TagKey key) {
        String sql = "SELECT * FROM " + tableName + " WHERE tag_id = ? AND language_id = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.message(),
                stmt -> {
                    stmt.setString(1, key.tagId());
                    stmt.setInt(2, key.languageId());
                });
    }

    private Message loadById(int id) {
        String sql = "SELECT * FROM " + tableName + " WHERE id = ?";
        return CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.message(),
                stmt -> stmt.setInt(1, id));
    }

    public Message getById(int id) {
        return cacheById.get(id);
    }

    public Message getByTag(String tagId, int languageId) {
        return cacheByTag.get(new TagKey(tagId, languageId));
    }

    public List<Message> getByLanguage(int languageId) {
        return cacheByLanguage.get(new LanguageKey(languageId));
    }

    public void put(Message message) {
        cacheById.put(message.id(), message);
        cacheByTag.put(new TagKey(message.tagId(), message.languageId()), message);
        CacheUtil.put(cacheByLanguage, new LanguageKey(message.languageId()), message,
                v -> v.id() == message.id());
    }

    public void remove(int id) {
        Message removed = cacheById.getIfPresent(id);
        if (removed != null) {
            cacheById.invalidate(id);
            cacheByTag.invalidate(new TagKey(removed.tagId(), removed.languageId()));
            CacheUtil.remove(cacheByLanguage, new LanguageKey(removed.languageId()),
                    v -> v.id() == removed.id());
        }
    }

    public void removeByTag(String tag, int languageId) {
        TagKey key = new TagKey(tag, languageId);
        Message removed = cacheByTag.getIfPresent(key);
        if (removed != null) {
            cacheByTag.invalidate(key);
            cacheById.invalidate(removed.id());
            CacheUtil.remove(cacheByLanguage, new LanguageKey(languageId),
                    v -> v.id() == removed.id());
        }
    }

    public void removeByLanguage(int languageId) {
        LanguageKey langKey = new LanguageKey(languageId);
        List<Message> removed = cacheByLanguage.getIfPresent(langKey);
        if (removed != null) {
            removed.forEach(message -> {
                cacheById.invalidate(message.id());
                cacheByTag.invalidate(new TagKey(message.tagId(), message.languageId()));
            });
            cacheByLanguage.invalidate(langKey);
        }
    }

    public void clear() {
        cacheById.invalidateAll();
        cacheByTag.invalidateAll();
        cacheByLanguage.invalidateAll();
    }

    protected record TagKey(String tagId, int languageId) {
    }

    protected record LanguageKey(int languageId) {
    }
}
