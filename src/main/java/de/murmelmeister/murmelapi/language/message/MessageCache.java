package de.murmelmeister.murmelapi.language.message;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.CacheUtil;
import de.murmelmeister.murmelapi.utils.MurmelCache;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * MessageCache is a thread-safe cache for storing messages by their ID and tag.
 * It allows for quick retrieval and management of messages based on their unique identifiers.
 */
final class MessageCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageCache.class);

    @Language("MariaDB")
    private static final String SELECT_BY_ID = "SELECT * FROM %s WHERE id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_LANGUAGE = "SELECT * FROM %s WHERE language_id = ?";
    @Language("MariaDB")
    private static final String SELECT_BY_TAG = "SELECT * FROM %s WHERE tag_id = ? AND language_id = ?";

    private final Database database;
    private final Gson gson;
    private final RefreshProvider refreshProvider;
    private final String tableName;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull Integer, Optional<Message>> cacheById;
    private final LoadingCache<@NotNull TagKey, Optional<Message>> cacheByTag;
    private final LoadingCache<@NotNull LanguageKey, List<Message>> cacheByLanguage;

    public MessageCache(Database database, Gson gson, RefreshProvider refreshProvider, String tableName, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.gson = gson;
        this.refreshProvider = refreshProvider;
        this.tableName = tableName;
        this.fetchLimit = fetchLimit;
        this.cacheById = CacheUtil.buildCacheRefresh(this::loadById, cacheCapacity, refreshInterval);
        this.cacheByTag = CacheUtil.buildCacheRefresh(this::loadByTag, cacheCapacity, refreshInterval);
        this.cacheByLanguage = CacheUtil.buildCacheRefresh(this::loadByLanguage, cacheCapacity, refreshInterval);
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
            if (key instanceof MessageKey messageKey)
                remove(messageKey);
            else if (key instanceof String json) {
                try {
                    final MessageKey messageKey = gson.fromJson(json, MessageKey.class);

                    if (messageKey == null) {
                        LOGGER.warn("Failed to parse JSON for single to null: {}", json);
                        return;
                    }

                    remove(messageKey);
                } catch (JsonSyntaxException e) {
                    LOGGER.warn("Failed to parse JSON for single refresh: {}", json, e);
                }
            }
        }
    }

    @Override
    public void close() {
        refreshProvider.unregister(this);
        clear();
    }

    private @NotNull List<Message> loadByLanguage(LanguageKey key) {
        String sql = SELECT_BY_LANGUAGE.formatted(tableName);
        return CacheUtil.loadList(database, sql, fetchLimit, MessageRowMapper::resultSet,
                stmt -> stmt.setInt(1, key.languageId()));
    }

    private @NotNull Optional<Message> loadByTag(TagKey key) {
        String sql = SELECT_BY_TAG.formatted(tableName);
        Message message = CacheUtil.loadSingle(database, sql, fetchLimit, MessageRowMapper::resultSet,
                stmt -> {
                    stmt.setString(1, key.tagId());
                    stmt.setInt(2, key.languageId());
                });

        return Optional.ofNullable(message);
    }

    private @NotNull Optional<Message> loadById(int id) {
        String sql = SELECT_BY_ID.formatted(tableName);
        Message message = CacheUtil.loadSingle(database, sql, fetchLimit, MessageRowMapper::resultSet,
                stmt -> stmt.setInt(1, id));

        return Optional.ofNullable(message);
    }

    public @NotNull Optional<Message> getById(int id) {
        return cacheById.get(id);
    }

    public @NotNull Optional<Message> getByTag(@NotNull String tagId, int languageId) {
        return cacheByTag.get(new TagKey(tagId, languageId));
    }

    public @NotNull @Unmodifiable List<Message> getByLanguage(int languageId) {
        List<Message> messages = cacheByLanguage.get(new LanguageKey(languageId));
        if (messages == null || messages.isEmpty())
            return Collections.emptyList();
        return List.copyOf(messages);
    }

    public void remove(@NotNull MessageKey messageKey) {
        cacheById.invalidate(messageKey.languageId());
        if (messageKey.tagId() != null) cacheByTag.invalidate(new TagKey(messageKey.tagId(), messageKey.languageId()));
        cacheByLanguage.invalidate(new LanguageKey(messageKey.languageId()));
    }

    public void clear() {
        cacheById.invalidateAll();
        cacheByTag.invalidateAll();
        cacheByLanguage.invalidateAll();
    }

    record MessageKey(int languageId, @Nullable String tagId) {
    }

    private record TagKey(@NotNull String tagId, int languageId) {
    }

    private record LanguageKey(int languageId) {
    }
}
