package de.murmelmeister.murmelapi.user.inventory;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.user.UserInventoryException;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class UserInventoryProviderImpl implements UserInventoryProvider {
    private static final String TABLE_NAME = "user_inventory";

    @Language("MariaDB")
    private static final String UPSERT_SQL = """
            INSERT INTO %s (user_id, inventory_id, inventory_value)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE
                inventory_value = VALUES(inventory_value)
            RETURNING user_id, inventory_id, inventory_value
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String DELETE_SQL = "DELETE FROM %s WHERE user_id = ? AND inventory_id = ?".formatted(TABLE_NAME);

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final UserInventoryCache cache;
    private final RefreshType all = RefreshType.USER_INVENTORIES;
    private final RefreshType single = RefreshType.SINGLE_USER_INVENTORY;

    public UserInventoryProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new UserInventoryCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @NotNull Optional<UserInventory> findById(int userId, int inventoryId) {
        return cache.get(userId, inventoryId);
    }

    @Override
    public @NotNull @Unmodifiable List<UserInventory> findAll() {
        return cache.getAll();
    }

    @Override
    public @NotNull Optional<UserInventory> upsert(int userId, int inventoryId, @NotNull String value) {
        Objects.requireNonNull(value, "value cannot be null");
        if (userId < 1) throw new IllegalArgumentException("userId cannot be >= 1");
        if (inventoryId < 1) throw new IllegalArgumentException("inventoryId cannot be >= 1");
        if (value.isBlank()) throw new IllegalArgumentException("value cannot be blank");

        Optional<UserInventory> existingOpt = cache.get(userId, inventoryId);
        if (existingOpt.isPresent())
            if (Objects.equals(value, existingOpt.get().value()))
                return existingOpt;

        UserInventory saved = MurmelExceptionWrapper.dbWrap(
                "Failed to upsert UserInventory (userId=" + userId + ", inventoryId=" + inventoryId + ")",
                () -> database.query(UPSERT_SQL, null, UserInventoryRowMapper::resultSet, stmt -> {
                    stmt.setInt(1, userId);
                    stmt.setInt(2, inventoryId);
                    stmt.setString(3, value);
                }),
                UserInventoryException::new
        );

        if (saved == null) return Optional.empty();
        refreshProvider.fireSingle(single, new UserInventoryCache.InventoryKey(saved.userId(), saved.inventoryId()));
        return Optional.of(saved);
    }

    @Override
    public int delete(int userId, int inventoryId) {
        if (userId < 1) throw new IllegalArgumentException("userId cannot be >= 1");
        if (inventoryId < 1) throw new IllegalArgumentException("inventoryId cannot be >= 1");

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete UserInventory (userId=" + userId + ", inventoryId=" + inventoryId + ")",
                () -> database.update(DELETE_SQL, stmt -> {
                    stmt.setInt(1, userId);
                    stmt.setInt(2, inventoryId);
                }),
                UserInventoryException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(single, new UserInventoryCache.InventoryKey(userId, inventoryId));
        return row;
    }
}
