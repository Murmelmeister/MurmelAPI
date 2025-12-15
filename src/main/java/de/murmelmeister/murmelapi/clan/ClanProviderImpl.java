package de.murmelmeister.murmelapi.clan;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public final class ClanProviderImpl implements ClanProvider {
    private static final String TABLE_NAME = "clans";

    private final Database database;
    private final ClanCache cache;
    private final RefreshType all = RefreshType.CLANS;
    private final RefreshType single = RefreshType.SINGLE_CLAN;

    public ClanProviderImpl(Database database, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.cache = new ClanCache(database, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        RefreshUtil.fireCache(all);
    }

    @Override
    public Clan findById(UUID id) {
        return cache.getById(id);
    }

    @Override
    public Clan findByName(String name) {
        return cache.getByName(name);
    }

    @Override
    public Clan findByOwner(int ownerId) {
        return cache.getByOwner(ownerId);
    }

    @Override
    public List<Clan> findAll() {
        return cache.getAll();
    }

    @Override
    public Clan create(String name, String tag, String sign, String description, int ownerId, int createdBy) {
        String normalizedName = StringUtil.normalize(name);
        if (normalizedName == null || normalizedName.length() > 100
                || tag.length() > 25 || sign.length() > 25
                || ownerId < 1 || createdBy < CONSOLE_USER_ID)
            return null;

        UUID id = UUID.randomUUID();
        String sql = "INSERT INTO " + TABLE_NAME + " (id, name, tag, sign, description, owner_id, created_by) VALUES (?, ?, ?, ?, ?, ?, ?)";
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

        String createdAtSql = "SELECT created_at FROM " + TABLE_NAME + " WHERE id = ?";
        LocalDateTime createdAt = database.query(createdAtSql, null,
                resultSet -> resultSet.getTimestamp("created_at").toLocalDateTime(),
                stmt -> stmt.setString(1, id.toString()));
        if (createdAt == null) return null;

        Clan clan = new Clan(id, normalizedName, tag, sign, description, ownerId, createdBy, createdAt, null, null);
        RefreshUtil.fireSingle(single, id);
        return clan;
    }

    @Override
    public int delete(UUID id) {
        if (id == null) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        int row = database.update(sql, stmt -> stmt.setString(1, id.toString()));
        if (row < 1) return 0;

        RefreshUtil.fireSingle(single, id);
        return row;
    }

    @Override
    public Clan update(UUID id, String name, String tag, String sign, String description, int ownerId, int changedBy) {
        String normalizedName = StringUtil.normalize(name);
        if (id == null || normalizedName == null || normalizedName.length() > 100
                || tag.length() > 25 || sign.length() > 25
                || ownerId < 1 || changedBy < CONSOLE_USER_ID)
            return null;

        Clan existing = cache.getById(id);
        if (existing == null) return null;

        if (Objects.equals(normalizedName, existing.name())
                && Objects.equals(tag, existing.tag())
                && Objects.equals(sign, existing.sign())
                && Objects.equals(description, existing.description())
                && ownerId == existing.ownerId())
            return existing;

        String sql = "UPDATE " + TABLE_NAME + " SET name = ?, tag = ?, sign = ?, description = ?, owner_id = ?, changed_by = ? WHERE id = ?";
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

        String changedAtSql = "SELECT changed_at FROM " + TABLE_NAME + " WHERE id = ?";
        LocalDateTime changedAt = database.query(changedAtSql, null,
                resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(),
                stmt -> stmt.setString(1, id.toString()));
        if (changedAt == null) return null;

        Clan clan = existing.withUpdateMeta(normalizedName, tag, sign, description, ownerId, changedBy, changedAt);
        RefreshUtil.fireSingle(single, id);
        return clan;
    }
}
