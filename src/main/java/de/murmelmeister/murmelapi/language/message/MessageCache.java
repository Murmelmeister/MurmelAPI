package de.murmelmeister.murmelapi.language.message;

import com.github.benmanes.caffeine.cache.LoadingCache;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.CacheUtil;
import de.murmelmeister.murmelapi.utils.MurmelCache;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MessageCache is a thread-safe cache for storing messages by their ID and tag.
 * It allows for quick retrieval and management of messages based on their unique identifiers.
 */
public class MessageCache implements MurmelCache {
    @Language("MariaDB")
    private static final String SELECT_ALL = "SELECT * FROM %s";
    @Language("MariaDB")
    private static final String SELECT_BY_ID = "SELECT * FROM %s WHERE id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_LANGUAGE = "SELECT * FROM %s WHERE language_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_TAG = "SELECT * FROM %s WHERE tag_id = ? AND language_id = ?";

    private static final Pattern LANGUAGE_KEY_PATTERN = Pattern.compile("^LanguageKey\\[languageId=(\\d+)]$");
    private static final Pattern TAG_KEY_PATTERN = Pattern.compile("^TagKey\\[tagId=(\\w+), languageId=(\\d+)]$");

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull Integer, Optional<Message>> cacheById;
    private final LoadingCache<@NotNull TagKey, Optional<Message>> cacheByTag;
    private final LoadingCache<@NotNull LanguageKey, List<Message>> cacheByLanguage;

    public MessageCache(Database database, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheById = CacheUtil.buildCacheRefresh(this::loadById, cacheCapacity, refreshInterval);
        this.cacheByTag = CacheUtil.buildCacheRefresh(this::loadByTag, cacheCapacity, refreshInterval);
        this.cacheByLanguage = CacheUtil.buildCacheRefresh(key -> loadByLanguage(key.languageId()), cacheCapacity, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();
        if (RefreshType.MESSAGES.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        if (RefreshType.SINGLE_MESSAGE.getName().equalsIgnoreCase(cacheName)) {
            Object key = event.key();
            if (!(key instanceof String)) {
                if (key instanceof Integer id)
                    remove(id);
                else if (key instanceof TagKey(String tagId, int languageId))
                    removeByTag(tagId, languageId);
                else if (key instanceof LanguageKey(int languageId))
                    removeByLanguage(languageId);
            } else {
                Matcher languageMatcher = LANGUAGE_KEY_PATTERN.matcher((String) key);
                Matcher tagMatcher = TAG_KEY_PATTERN.matcher((String) key);
                if (languageMatcher.matches()) {
                    int languageId = Integer.parseInt(languageMatcher.group(1));
                    removeByLanguage(languageId);
                } else if (tagMatcher.matches()) {
                    String tagId = tagMatcher.group(1);
                    int languageId = Integer.parseInt(tagMatcher.group(2));
                    removeByTag(tagId, languageId);
                } else {
                    int id = Integer.parseInt((String) key);
                    remove(id);
                }
            }
        }
    }

    @Override
    public void close() {
        refreshProvider.unregister(this);
        clear();
    }

    private @NotNull List<Message> loadAllFromDatabase() {
        String sql = SELECT_ALL.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.message());
    }

    private @NotNull List<Message> loadByLanguage(int languageId) {
        String sql = SELECT_BY_LANGUAGE.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, ResultSetUtil.message(),
                stmt -> stmt.setInt(1, languageId));
    }

    private @NotNull Optional<Message> loadByTag(TagKey key) {
        String sql = SELECT_BY_TAG.formatted(tableName);
        Message message = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.message(),
                stmt -> {
                    stmt.setString(1, key.tagId());
                    stmt.setInt(2, key.languageId());
                });

        return Optional.ofNullable(message);
    }

    private @NotNull Optional<Message> loadById(int id) {
        String sql = SELECT_BY_ID.formatted(tableName);
        Message message = CacheUtil.loadSingle(database, sql, fetchLimit, ResultSetUtil.message(),
                stmt -> stmt.setInt(1, id));

        return Optional.ofNullable(message);
    }

    public @Nullable Message getById(int id) {
        Optional<Message> optMessage = cacheById.get(id);
        return optMessage != null && optMessage.isPresent() ? optMessage.orElse(null) : null;
    }

    public @Nullable Message getByTag(@NotNull String tagId, int languageId) {
        Optional<Message> optMessage = cacheByTag.get(new TagKey(tagId, languageId));
        return optMessage != null && optMessage.isPresent() ? optMessage.orElse(null) : null;
    }

    public @Nullable List<Message> getByLanguage(int languageId) {
        return cacheByLanguage.get(new LanguageKey(languageId));
    }

    public void put(@Nullable Message message) {
        if (message == null) return;
        cacheById.put(message.id(), Optional.of(message));
        cacheByTag.put(new TagKey(message.tagId(), message.languageId()), Optional.of(message));
        CacheUtil.put(cacheByLanguage, new LanguageKey(message.languageId()), message,
                v -> v.id() == message.id());
    }

    public void remove(int id) {
        Optional<Message> optMessage = cacheById.getIfPresent(id);
        cacheById.invalidate(id);

        if (optMessage != null && optMessage.isPresent()) {
            Message message = optMessage.get();
            cacheByTag.invalidate(new TagKey(message.tagId(), message.languageId()));
            CacheUtil.remove(cacheByLanguage, new LanguageKey(message.languageId()),
                    v -> v.id() == message.id());
        }
    }

    public void removeByTag(@NotNull String tag, int languageId) {
        TagKey key = new TagKey(tag, languageId);
        Optional<Message> optMessage = cacheByTag.getIfPresent(key);
        cacheByTag.invalidate(key);

        if (optMessage != null && optMessage.isPresent()) {
            Message message = optMessage.get();
            cacheById.invalidate(message.id());
            CacheUtil.remove(cacheByLanguage, new LanguageKey(languageId),
                    v -> v.id() == message.id());
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

    protected record TagKey(@NotNull String tagId, int languageId) {
    }

    protected record LanguageKey(int languageId) {
    }
}
