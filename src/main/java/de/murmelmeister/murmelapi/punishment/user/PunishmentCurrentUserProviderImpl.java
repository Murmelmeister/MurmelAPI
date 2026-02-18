package de.murmelmeister.murmelapi.punishment.user;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class PunishmentCurrentUserProviderImpl implements PunishmentCurrentUserProvider {
    private static final String TABLE_NAME = "punishment_current_user";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final PunishmentCurrentUserCache cache;
    private final RefreshType all = RefreshType.PUNISHMENT_USERS;
    private final RefreshType single = RefreshType.SINGLE_PUNISHMENT_USER;

    public PunishmentCurrentUserProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new PunishmentCurrentUserCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @NotNull @Unmodifiable List<Integer> getAllPunishedUserIds(int typeId) {
        return cache.getCachedPunishUsers().stream()
                .filter(punish -> punish.typeId() == typeId)
                .map(PunishmentCurrentUser::userId)
                .toList();
    }

    @Override
    public @Nullable PunishmentCurrentUser getPunishedUser(int userId, int typeId) {
        return cache.get(userId, typeId);
    }

    @Override
    public @Nullable PunishmentCurrentUser create(int userId, int typeId, @NotNull UUID logId) {
        if (userId < 1 || typeId < 1)
            return null;

        @Language("MariaDB")
        String sql = """
                INSERT INTO %s (user_id, type_id, log_id)
                VALUES (?, ?, ?)
                """.formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> {
            stmt.setInt(1, userId);
            stmt.setInt(2, typeId);
            stmt.setString(3, logId.toString());
        });
        if (row < 1) return null;

        PunishmentCurrentUser punish = new PunishmentCurrentUser(userId, typeId, logId);
        refreshProvider.fireSingle(single, new PunishmentCurrentUserCache.UserTypeKey(userId, typeId));
        return punish;
    }

    @Override
    public int delete(int userId, int typeId) {
        if (userId < 1 || typeId < 1)
            return 0;

        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE user_id = ? AND type_id = ?".formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> {
            stmt.setInt(1, userId);
            stmt.setInt(2, typeId);
        });
        if (row < 1) return 0;

        refreshProvider.fireSingle(single, new PunishmentCurrentUserCache.UserTypeKey(userId, typeId));
        return row;
    }

    @Override
    public @Nullable PunishmentCurrentUser update(int userId, int typeId, @NotNull UUID logId) {
        if (userId < 1 || typeId < 1)
            return null;
        PunishmentCurrentUser existing = cache.get(userId, typeId);
        if (existing == null) return null;

        if (Objects.equals(logId, existing.logId()))
            return existing; // No change needed

        @Language("MariaDB")
        String sql = "UPDATE %s SET log_id = ? WHERE user_id = ? AND type_id = ?".formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> {
            stmt.setString(1, logId.toString());
            stmt.setInt(2, userId);
            stmt.setInt(3, typeId);
        });
        if (row < 1) return null;

        PunishmentCurrentUser punish = PunishmentCurrentUser.builder(existing)
                .logId(logId)
                .build();
        refreshProvider.fireSingle(single, new PunishmentCurrentUserCache.UserTypeKey(userId, typeId));
        return punish;
    }
}
