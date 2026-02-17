package de.murmelmeister.murmelapi.clan;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public final class ClanProviderImpl implements ClanProvider {
    private static final String TABLE_NAME = "clans";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final ClanCache cache;
    private final RefreshType all = RefreshType.CLANS;
    private final RefreshType single = RefreshType.SINGLE_CLAN;

    public ClanProviderImpl(Database database, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new ClanCache(database, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
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
    public @NotNull List<Clan> findAll() {
        return cache.getAll();
    }

    @Override
    public @Nullable Clan create(@NotNull String name, @NotNull String tag, @NotNull String sign, @NotNull String description, int ownerId, int createdBy) {
        String normalizedName = StringUtil.normalize(name);
        if (normalizedName == null || normalizedName.length() > 100
                || tag.length() > 25 || sign.length() > 25
                || ownerId < 1 || createdBy < CONSOLE_USER_ID)
            return null;

        UUID id = UUID.randomUUID();
        @Language("MariaDB")
        String sql = """
                INSERT INTO %s (id, name, tag, sign, description, owner_id, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """.formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> {
            stmt.setString(1, id.toString());
            stmt.setString(2, normalizedName);
            stmt.setString(3, tag);
            stmt.setString(4, sign);
            stmt.setString(5, description);
            stmt.setInt(6, ownerId);
            stmt.setInt(7, createdBy);
        });
        if (row < 1) return null;

        @Language("MariaDB")
        String createdAtSql = "SELECT created_at FROM %s WHERE id = ?".formatted(TABLE_NAME);
        LocalDateTime createdAt = database.query(createdAtSql, null,
                resultSet -> resultSet.getTimestamp("created_at").toLocalDateTime(),
                stmt -> stmt.setString(1, id.toString()));
        if (createdAt == null) return null;

        Clan clan = new Clan(id, normalizedName, tag, sign, description, ownerId, createdBy, createdAt, null, null);
        refreshProvider.fireSingle(single, clan);
        return clan;
    }

    @Override
    public int delete(@NotNull UUID id) {
        Clan existing = cache.getById(id);
        if (existing == null) return 0;

        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE id = ?".formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> stmt.setString(1, id.toString()));
        if (row < 1) return 0;

        refreshProvider.fireSingle(single, existing);
        return row;
    }

    @Override
    public @Nullable Clan update(@NotNull UUID id, @NotNull String name, @NotNull String tag, @NotNull String sign, @NotNull String description, int ownerId, int changedBy) {
        String normalizedName = StringUtil.normalize(name);
        if (normalizedName == null || normalizedName.length() > 100 || tag.length() > 25 || sign.length() > 25 || ownerId < 1 || changedBy < CONSOLE_USER_ID)
            return null;

        Clan existing = cache.getById(id);
        if (existing == null) return null;

        if (Objects.equals(normalizedName, existing.name())
                && Objects.equals(tag, existing.tag())
                && Objects.equals(sign, existing.sign())
                && Objects.equals(description, existing.description())
                && ownerId == existing.ownerId())
            return existing;

        @Language("MariaDB")
        String sql = """
                UPDATE %s SET
                    name = ?,
                    tag = ?,
                    sign = ?,
                    description = ?,
                    owner_id = ?,
                    changed_by = ?
                WHERE id = ?
                """.formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> {
            stmt.setString(1, normalizedName);
            stmt.setString(2, tag);
            stmt.setString(3, sign);
            stmt.setString(4, description);
            stmt.setInt(5, ownerId);
            stmt.setInt(6, changedBy);
            stmt.setString(7, id.toString());
        });
        if (row < 1) return null;

        @Language("MariaDB")
        String changedAtSql = "SELECT changed_at FROM %s WHERE id = ?".formatted(TABLE_NAME);
        LocalDateTime changedAt = database.query(changedAtSql, null,
                resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(),
                stmt -> stmt.setString(1, id.toString()));
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
}
