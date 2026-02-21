package de.murmelmeister.murmelapi.language.message;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.language.MessageException;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.util.*;

public final class MessageProviderImpl implements MessageProvider {
    private static final String TABLE_NAME = "messages";

    @Language("MariaDB")
    private static final String CREATE_SQL = """
            INSERT INTO %s (tag_id, language_id, message)
            VALUES (?, ?, ?)
            RETURNING id, tag_id, language_id, message
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String DELETE_SQL = "DELETE FROM %s WHERE id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String DELETE_TAG_SQL = "DELETE FROM %s WHERE tag_id = ? AND language_id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String DELETE_LANGUAGE_SQL = "DELETE FROM %s WHERE language_id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPDATE_SQL = "UPDATE %s SET tag_id = ?, language_id = ?, message = ? WHERE id = ?".formatted(TABLE_NAME);

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
    public @Nullable Message get(int messageId) {
        return cache.getById(messageId);
    }

    @Override
    public @Nullable Message get(@NotNull String tagId, int languageId) {
        return cache.getByTag(tagId, languageId);
    }

    @Override
    public @NotNull @Unmodifiable List<Message> getAllMessages(int languageId) {
        return cache.getByLanguage(languageId);
    }

    @Override
    public @Nullable Message create(@NotNull String tagId, int languageId, @NotNull String message) {
        Objects.requireNonNull(tagId, "tagId cannot be null");
        Objects.requireNonNull(message, "message cannot be null");
        String normalizedTagId = StringUtil.normalize(tagId);
        if (normalizedTagId == null || normalizedTagId.isBlank())
            throw new IllegalArgumentException("tagId cannot be blank");
        if (message.isBlank()) throw new IllegalArgumentException("message cannot be blank");

        Message msg = MurmelExceptionWrapper.dbWrap(
                "Failed to create Message (tagId=" + normalizedTagId + ", languageId=" + languageId + ")",
                () -> database.query(CREATE_SQL, null, ResultSetUtil.message(), stmt -> {
                    stmt.setString(1, normalizedTagId);
                    stmt.setInt(2, languageId);
                    stmt.setString(3, message);
                }),
                MessageException::new
        );

        if (msg == null) return null;
        refreshProvider.fireSingle(single, new MessageCache.MessageKey(msg.languageId(), msg.tagId()));
        return msg;
    }

    @Override
    public int delete(int id) {
        Message existing = cache.getById(id);
        if (existing == null) return 0;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete Message (id=" + id + ")",
                () -> database.update(DELETE_SQL, stmt -> {
                    stmt.setInt(1, id);
                }),
                MessageException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(single, new MessageCache.MessageKey(existing.languageId(), existing.tagId()));
        return row;
    }

    @Override
    public int delete(@NotNull String tagId, int languageId) {
        Objects.requireNonNull(tagId, "tagId cannot be null");
        String normalizedTagId = StringUtil.normalize(tagId);
        if (normalizedTagId == null || normalizedTagId.isBlank())
            throw new IllegalArgumentException("tagId cannot be blank");

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete Message (tagId=" + normalizedTagId + ", languageId=" + languageId + ")",
                () -> database.update(DELETE_TAG_SQL, stmt -> {
                    stmt.setString(1, normalizedTagId);
                    stmt.setInt(2, languageId);
                }),
                MessageException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(single, new MessageCache.MessageKey(languageId, normalizedTagId));
        return row;
    }

    @Override
    public int deleteAll(int languageId) {
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
    public @Nullable Message update(int id, @NotNull String tagId, int languageId, @NotNull String message) {
        Objects.requireNonNull(tagId, "tagId cannot be null");
        Objects.requireNonNull(message, "message cannot be null");
        String normalizedTagId = StringUtil.normalize(tagId);
        if (normalizedTagId == null || normalizedTagId.isBlank())
            throw new IllegalArgumentException("tagId cannot be blank");
        if (message.isBlank()) throw new IllegalArgumentException("message cannot be blank");

        Message existing = cache.getById(id);
        if (existing == null) return null;

        if (Objects.equals(normalizedTagId, existing.tagId()) &&
                languageId == existing.languageId() &&
                Objects.equals(message, existing.message()))
            return existing;

        int rows = MurmelExceptionWrapper.dbWrap(
                "Failed to update Message (id=" + id + ", tagId=" + normalizedTagId + ", languageId=" + languageId + ")",
                () -> database.update(UPDATE_SQL, stmt -> {
                    stmt.setString(1, normalizedTagId);
                    stmt.setInt(2, languageId);
                    stmt.setString(3, message);
                    stmt.setInt(4, id);
                }),
                MessageException::new
        );
        if (rows < 1) return null;

        Message msg = Message.builder(existing)
                .tagId(tagId)
                .languageId(languageId)
                .message(message)
                .build();
        refreshProvider.fireSingle(single, new MessageCache.MessageKey(msg.languageId(), msg.tagId()));
        return msg;
    }

    @Override
    public int @NotNull [] upsertAll(@NotNull Properties properties) {
        return upsertInternal(properties, true);
    }

    @Override
    public int @NotNull [] upsertAll(@NotNull Collection<Properties> properties) {
        if (properties.isEmpty())
            throw new IllegalArgumentException("Missing properties collection");

        List<int[]> results = new ArrayList<>();
        boolean changed = false;
        for (Properties props : properties) {
            int[] result = upsertInternal(props, false);
            if (result.length > 0) {
                results.add(result);
                changed = true;
            }
        }

        if (changed)
            refreshProvider.fireCache(all);

        int total = results.stream().mapToInt(arr -> arr.length).sum();
        int[] merged = new int[total];
        int offset = 0;
        for (int[] arr : results) {
            System.arraycopy(arr, 0, merged, offset, arr.length);
            offset += arr.length;
        }
        return merged;
    }

    @Override
    public int @NotNull [] createOrUpdateAll(@NotNull Properties properties) {
        return upsertInternal(properties, true);
    }

    private int @NotNull [] upsertInternal(@NotNull Properties properties, boolean fireCache) {
        Objects.requireNonNull(properties, "properties cannot be null");
        if (properties.isEmpty())
            throw new IllegalArgumentException("Missing properties file");

        String language = properties.getProperty("language.id");
        if (language == null || language.isBlank())
            throw new IllegalArgumentException("Missing language.id property");

        int languageId;
        try {
            languageId = Integer.parseInt(language);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid language.id property is not a integer", e);
        }

        // Load existing messages for the language at once
        @Language("MariaDB")
        String selectSql = "SELECT tag_id, message FROM %s WHERE language_id = ?".formatted(TABLE_NAME);
        Map<String, String> existing = new HashMap<>();
        database.queryList(selectSql, rs -> Map.entry(rs.getString(1), rs.getString(2)), stmt -> stmt.setInt(1, languageId))
                .forEach(entry -> existing.put(entry.getKey(), entry.getValue()));

        // Batch only changed rows for update
        @Language("MariaDB")
        String updateSql = "UPDATE %s SET message = ? WHERE tag_id = ? AND language_id = ?".formatted(TABLE_NAME);
        int[] updated = database.updateBatch(updateSql, stmt -> {
            for (String tagId : properties.stringPropertyNames()) {
                if (tagId.isBlank() || tagId.startsWith("#") || tagId.equals("language.id")) continue;
                String msg = properties.getProperty(tagId);
                if (msg == null || msg.isBlank()) continue;

                String current = existing.get(tagId);
                if (current == null) continue; // missing -> insert later
                if (current.equals(msg)) continue; // no change

                stmt.setString(1, msg);
                stmt.setString(2, tagId);
                stmt.setInt(3, languageId);
                stmt.addBatch();
            }
        });

        // Batch only truly new rows for insert
        @Language("MariaDB")
        String insertSql = "INSERT INTO %s (tag_id, language_id, message) VALUES (?, ?, ?)".formatted(TABLE_NAME);
        int[] inserted = database.updateBatch(insertSql, stmt -> {
            for (String tagId : properties.stringPropertyNames()) {
                if (tagId.isBlank() || tagId.startsWith("#") || tagId.equals("language.id")) continue;
                String msg = properties.getProperty(tagId);
                if (msg == null || msg.isBlank()) continue;

                if (existing.containsKey(tagId)) continue; // already present

                stmt.setString(1, tagId);
                stmt.setInt(2, languageId);
                stmt.setString(3, msg);
                stmt.addBatch();
            }
        });

        int totalOps = (updated == null ? 0 : updated.length) + (inserted == null ? 0 : inserted.length);
        if (fireCache && totalOps > 0)
            refreshProvider.fireCache(all);

        // Merge counts for compatibility
        int updateLen = updated == null ? 0 : updated.length;
        int insertLen = inserted == null ? 0 : inserted.length;
        int[] result = new int[updateLen + insertLen];
        if (updated != null) System.arraycopy(updated, 0, result, 0, updateLen);
        if (inserted != null) System.arraycopy(inserted, 0, result, updateLen, insertLen);
        return result;
    }
}
