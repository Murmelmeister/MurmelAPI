package de.murmelmeister.murmelapi.clan;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.clan.ClanException;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public final class ClanProviderImpl implements ClanProvider {
    private static final String TABLE_NAME = "clans";

    @Language("MariaDB")
    private static final String CREATE_SQL = """
            INSERT INTO %s (id, name, tag, sign, description, owner_id, created_by)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            RETURNING id, name, tag, sign, description, owner_id, created_by, created_at, changed_by, changed_at
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String DELETE_SQL = "DELETE FROM %s WHERE id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPDATE_SQL = """
            UPDATE %s SET
                name = ?,
                tag = ?,
                sign = ?,
                description = ?,
                owner_id = ?,
                changed_by = ?
            WHERE id = ?
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPDATE_SELECT_SQL = "SELECT changed_at FROM %s WHERE id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPSERT_SQL = """
                    INSERT INTO %s (id, name, tag, sign, description, owner_id, created_by)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        name = VALUES(name),
                        tag = VALUES(tag),
                        sign = VALUES(sign),
                        description = VALUES(description),
                        owner_id = VALUES(owner_id),
                        changed_by = ?
                    RETURNING id, name, tag, sign, description, owner_id, created_by, created_at, changed_by, changed_at
            """.formatted(TABLE_NAME);

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final ClanCache cache;
    private final RefreshType all = RefreshType.CLANS;
    private final RefreshType single = RefreshType.SINGLE_CLAN;

    public ClanProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new ClanCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @Nullable Clan findById(@Nullable UUID id) {
        return cache.getById(id);
    }

    @Override
    public @Nullable Clan findByName(@Nullable String name) {
        return cache.getByName(name);
    }

    @Override
    public @Nullable Clan findByOwner(int ownerId) {
        return cache.getByOwner(ownerId);
    }

    @Override
    public @NotNull @Unmodifiable List<Clan> findAll() {
        return cache.getAll();
    }

    @Override
    public @Nullable Clan create(@NotNull String name, @NotNull String tag, @NotNull String sign, @NotNull String description, int ownerId, int createdBy) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(tag, "tag cannot be null");
        Objects.requireNonNull(sign, "sign cannot be null");
        Objects.requireNonNull(description, "description cannot be null");
        if (ownerId < 1) throw new IllegalArgumentException("ownerId must be >= 1");
        if (tag.length() > 25) throw new IllegalArgumentException("tag must be <= 25 characters");
        if (sign.length() > 25) throw new IllegalArgumentException("sign must be <= 25 characters");
        if (createdBy < CONSOLE_USER_ID) throw new IllegalArgumentException("createdBy must be >= " + CONSOLE_USER_ID);
        String normalizedName = StringUtil.normalize(name);
        if (normalizedName == null || normalizedName.isBlank())
            throw new IllegalArgumentException("name cannot be blank");
        if (normalizedName.length() > 100) throw new IllegalArgumentException("name must be <= 100 characters");

        UUID id = UUID.randomUUID();
        Clan clan = MurmelExceptionWrapper.dbWrap(
                "Failed to create Clan (name=" + normalizedName + ")",
                () -> database.query(CREATE_SQL, null, ResultSetUtil.clan(), stmt -> {
                    stmt.setString(1, id.toString());
                    stmt.setString(2, normalizedName);
                    stmt.setString(3, tag);
                    stmt.setString(4, sign);
                    stmt.setString(5, description);
                    stmt.setInt(6, ownerId);
                    stmt.setInt(7, createdBy);
                }),
                ClanException::new
        );

        if (clan == null) return null;
        refreshProvider.fireSingle(single, clan);
        return clan;
    }

    @Override
    public int delete(@NotNull UUID id) {
        Objects.requireNonNull(id, "id cannot be null");

        Clan existing = cache.getById(id);
        if (existing == null) return 0;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete Clan (id=" + id + ")",
                () -> database.update(DELETE_SQL, stmt -> stmt.setString(1, id.toString())),
                ClanException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(single, existing);
        return row;
    }

    @Override
    public @Nullable Clan update(@NotNull UUID id, @NotNull String name, @NotNull String tag, @NotNull String sign, @NotNull String description, int ownerId, int changedBy) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(tag, "tag cannot be null");
        Objects.requireNonNull(sign, "sign cannot be null");
        Objects.requireNonNull(description, "description cannot be null");
        if (ownerId < 1) throw new IllegalArgumentException("ownerId must be >= 1");
        if (tag.length() > 25) throw new IllegalArgumentException("tag must be <= 25 characters");
        if (sign.length() > 25) throw new IllegalArgumentException("sign must be <= 25 characters");
        if (changedBy < CONSOLE_USER_ID) throw new IllegalArgumentException("changedBy must be >= " + CONSOLE_USER_ID);
        String normalizedName = StringUtil.normalize(name);
        if (normalizedName == null || normalizedName.isBlank())
            throw new IllegalArgumentException("name cannot be blank");
        if (normalizedName.length() > 100) throw new IllegalArgumentException("name must be <= 100 characters");

        Clan existing = cache.getById(id);
        if (existing == null) return null;

        if (Objects.equals(normalizedName, existing.name())
                && Objects.equals(tag, existing.tag())
                && Objects.equals(sign, existing.sign())
                && Objects.equals(description, existing.description())
                && ownerId == existing.ownerId())
            return existing;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to update Clan (id=" + id + ")",
                () -> database.update(UPDATE_SQL, stmt -> {
                    stmt.setString(1, normalizedName);
                    stmt.setString(2, tag);
                    stmt.setString(3, sign);
                    stmt.setString(4, description);
                    stmt.setInt(5, ownerId);
                    stmt.setInt(6, changedBy);
                    stmt.setString(7, id.toString());
                }),
                ClanException::new
        );
        if (row != 1) return null;

        LocalDateTime changedAt = MurmelExceptionWrapper.dbWrap(
                "Failed to get changed_at for Clan (id=" + id + ")",
                () -> database.query(UPDATE_SELECT_SQL, null,
                        resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(),
                        stmt -> stmt.setString(1, id.toString())),
                ClanException::new
        );
        if (changedAt == null) return null;

        Clan clan = Clan.builder(existing)
                .name(normalizedName)
                .tag(tag)
                .sign(sign)
                .description(description)
                .ownerId(ownerId)
                .changedBy(changedBy)
                .changedAt(changedAt)
                .build();
        refreshProvider.fireSingle(single, clan);
        return clan;
    }

    @Override
    public @Nullable Clan upsert(@NotNull UUID id, @NotNull String name, @NotNull String tag, @NotNull String sign, @NotNull String description, int ownerId, int executorId) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(tag, "tag cannot be null");
        Objects.requireNonNull(sign, "sign cannot be null");
        Objects.requireNonNull(description, "description cannot be null");
        if (ownerId < 1) throw new IllegalArgumentException("ownerId must be >= 1");
        if (tag.length() > 25) throw new IllegalArgumentException("tag must be <= 25 characters");
        if (sign.length() > 25) throw new IllegalArgumentException("sign must be <= 25 characters");
        if (executorId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("executorId must be >= " + CONSOLE_USER_ID);
        String normalizedName = StringUtil.normalize(name);
        if (normalizedName == null || normalizedName.isBlank())
            throw new IllegalArgumentException("name cannot be blank");
        if (normalizedName.length() > 100) throw new IllegalArgumentException("name must be <= 100 characters");

        Clan existing = cache.getById(id);
        if (existing != null
                && Objects.equals(normalizedName, existing.name())
                && Objects.equals(tag, existing.tag())
                && Objects.equals(sign, existing.sign())
                && Objects.equals(description, existing.description())
                && ownerId == existing.ownerId())
            return existing;

        Clan saved = MurmelExceptionWrapper.dbWrap(
                "Failed to upsert Clan (id=" + id + ")",
                () -> database.query(UPSERT_SQL, null, ResultSetUtil.clan(), stmt -> {
                    stmt.setString(1, id.toString());
                    stmt.setString(2, normalizedName);
                    stmt.setString(3, tag);
                    stmt.setString(4, sign);
                    stmt.setString(5, description);
                    stmt.setInt(6, ownerId);
                    stmt.setInt(7, executorId);
                    stmt.setInt(8, executorId);
                }),
                ClanException::new
        );

        if (saved == null) return null;
        refreshProvider.fireSingle(single, saved);
        return saved;
    }
}
