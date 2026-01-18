package de.murmelmeister.murmelapi.user.inventory;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public final class UserInventoryProviderImpl implements UserInventoryProvider {
    private static final String TABLE_NAME = "user_inventory";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final UserInventoryCache cache;
    private final RefreshType all = RefreshType.USER_INVENTORIES;
    private final RefreshType single = RefreshType.SINGLE_USER_INVENTORY;

    public UserInventoryProviderImpl(Database database, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new UserInventoryCache(database, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @Nullable UserInventory findById(int userId, int inventoryId) {
        return cache.get(userId, inventoryId);
    }

    @Override
    public @NotNull List<UserInventory> findAll() {
        return cache.getAll();
    }

    @Override
    public @Nullable UserInventory create(int userId, int inventoryId, @NotNull String value) {
        if (userId < 1 || inventoryId < 1) return null;

        @Language("MariaDB")
        String sql = """
                INSERT INTO %s (user_id, inventory_id, inventory_value)
                VALUES (?, ?, ?)
                """.formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> {
            stmt.setInt(1, userId);
            stmt.setInt(2, inventoryId);
            stmt.setString(3, value);
        });
        if (row < 1) return null;

        UserInventory inventory = new UserInventory(userId, inventoryId, value);
        refreshProvider.fireSingle(single, new UserInventoryCache.InventoryKey(userId, inventoryId));
        return inventory;
    }

    @Override
    public int delete(int userId, int inventoryId) {
        if (userId < 1 || inventoryId < 1) return 0;

        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE user_id = ? AND inventory_id = ?".formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> {
            stmt.setInt(1, userId);
            stmt.setInt(2, inventoryId);
        });
        if (row < 1) return 0;

        refreshProvider.fireSingle(single, new UserInventoryCache.InventoryKey(userId, inventoryId));
        return row;
    }

    @Override
    public @Nullable UserInventory update(int userId, int inventoryId, @NotNull String value) {
        if (userId < 1 || inventoryId < 1) return null;

        UserInventory existing = cache.get(userId, inventoryId);
        if (existing == null) return null;

        if (Objects.equals(value, existing.value())) return existing;

        @Language("MariaDB")
        String sql = "UPDATE %s SET inventory_value = ? WHERE user_id = ? AND inventory_id = ?".formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> {
            stmt.setString(1, value);
            stmt.setInt(2, userId);
            stmt.setInt(3, inventoryId);
        });
        if (row < 1) return null;

        UserInventory inventory = UserInventory.builder(existing)
                .value(value)
                .build();
        refreshProvider.fireSingle(single, new UserInventoryCache.InventoryKey(userId, inventoryId));
        return inventory;
    }
}
