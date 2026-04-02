package de.murmelmeister.murmelapi.language.message;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.language.MessageException;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.util.*;

final class MessageProviderImpl implements MessageProvider {
    private static final String TABLE_NAME = "messages";

    @Language("MariaDB")
    private static final String UPSERT_SQL = """
            INSERT INTO %s (tag_id, language_id, message)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE
                message = VALUES(message)
            RETURNING id, tag_id, language_id, message
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPSERT_ALL_SQL = """
            INSERT INTO %s (tag_id, language_id, message)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE
                message = VALUES(message)
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String DELETE_TAG_SQL = "DELETE FROM %s WHERE tag_id = ? AND language_id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String DELETE_LANGUAGE_SQL = "DELETE FROM %s WHERE language_id = ?".formatted(TABLE_NAME);

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final MessageCache cache;
    private final RefreshType all = RefreshType.MESSAGES;
    private final RefreshType single = RefreshType.SINGLE_MESSAGE;

    public MessageProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new MessageCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @NotNull Optional<Message> findMessage(int messageId) {
        return cache.getById(messageId);
    }

    @Override
    public @NotNull Optional<Message> findMessage(@NotNull String tagId, int languageId) {
        return cache.getByTag(tagId, languageId);
    }

    @Override
    public @NotNull @Unmodifiable List<Message> findMessages(int languageId) {
        return cache.getByLanguage(languageId);
    }

    @Override
    public @NotNull Optional<Message> upsert(@NotNull String tagId, int languageId, @NotNull String message) {
        Objects.requireNonNull(tagId, "tagId cannot be null");
        Objects.requireNonNull(message, "message cannot be null");
        String normalizedTagId = StringUtil.normalize(tagId);
        if (normalizedTagId == null || normalizedTagId.isBlank())
            throw new IllegalArgumentException("tagId cannot be blank");
        if (message.isBlank()) throw new IllegalArgumentException("message cannot be blank");

        Optional<Message> existingOpt = cache.getByTag(tagId, languageId);
        if (existingOpt.isPresent()) {
            Message existing = existingOpt.get();
            if (Objects.equals(existing.message(), message))
                return existingOpt;
        }

        Message saved = MurmelExceptionWrapper.dbWrap(
                "Failed to upsert Message (tagId=" + normalizedTagId + ", languageId=" + languageId + ")",
                () -> database.query(UPSERT_SQL, null, MessageRowMapper::resultSet, stmt -> {
                    stmt.setString(1, normalizedTagId);
                    stmt.setInt(2, languageId);
                    stmt.setString(3, message);
                }),
                MessageException::new
        );

        if (saved == null) return Optional.empty();
        refreshProvider.fireSingle(single, new MessageCache.MessageKey(saved.languageId(), saved.tagId()));
        return Optional.of(saved);
    }

    @Override
    public int delete(@NotNull String tagId, int languageId) {
        Objects.requireNonNull(tagId, "tagId cannot be null");
        String normalizedTagId = StringUtil.normalize(tagId);
        if (normalizedTagId == null || normalizedTagId.isBlank())
            throw new IllegalArgumentException("tagId cannot be blank");

        Optional<Message> existingOpt = cache.getByTag(tagId, languageId);
        if (existingOpt.isEmpty()) return 0;
        Message existing = existingOpt.get();

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete Message (tagId=" + normalizedTagId + ", languageId=" + languageId + ")",
                () -> database.update(DELETE_TAG_SQL, stmt -> {
                    stmt.setString(1, normalizedTagId);
                    stmt.setInt(2, languageId);
                }),
                MessageException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(single, new MessageCache.MessageKey(existing.languageId(), existing.tagId()));
        return row;
    }

    @Override
    public int delete(int languageId) {
        List<Message> existing = cache.getByLanguage(languageId);
        if (existing.isEmpty()) return 0;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete all Messages for language (id=" + languageId + ")",
                () -> database.update(DELETE_LANGUAGE_SQL, stmt -> stmt.setInt(1, languageId)),
                MessageException::new
        );

        if (row < 1) return 0;
        refreshProvider.fireSingle(single, new MessageCache.MessageKey(languageId, null));
        return row;
    }

    @Override
    public int @NotNull [] upsertAll(@NotNull Collection<Message> messages) {
        Objects.requireNonNull(messages, "messages cannot be null");
        if (messages.isEmpty())
            throw new IllegalArgumentException("Missing messages");

        int[] result = database.updateBatch(UPSERT_ALL_SQL, stmt -> {
            for (Message message : messages) {
                stmt.setString(1, message.tagId());
                stmt.setInt(2, message.languageId());
                stmt.setString(3, message.message());
                stmt.addBatch();
            }
        });

        if (result == null || result.length == 0)
            return new int[0];

        refreshProvider.fireCache(all);
        return result;
    }
}
