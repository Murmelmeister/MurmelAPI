package de.murmelmeister.murmelapi.user.inventory;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public final class UserInventoryProviderImpl implements UserInventoryProvider {
    private static final String TABLE_NAME = "user_inventory";

    private final Database database;
    private final UserInventoryCache cache;
    private final RefreshType all = RefreshType.USER_INVENTORIES;
    private final RefreshType single = RefreshType.SINGLE_USER_INVENTORY;

    public UserInventoryProviderImpl(Database database, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.cache = new UserInventoryCache(database, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        RefreshUtil.fireCache(all);
    }

    @Override
    public UserInventory findById(int userId, int inventoryId) {
        return cache.get(userId, inventoryId);
    }

    @Override
    public List<UserInventory> findAll() {
        return cache.getAll();
    }

    @Override
    public UserInventory create(int userId, int inventoryId, String value) {
        if (userId < 1 || inventoryId < 1 || value == null) return null;

        String sql = "INSERT INTO " + TABLE_NAME + " (user_id, inventory_id, inventory_value) VALUES (?, ?, ?)";
        int row = database.update(sql, stmt -> {
            stmt.setInt(1, userId);
            stmt.setInt(2, inventoryId);
            stmt.setString(3, value);
        });
        if (row < 1) return null;

        UserInventory inventory = new UserInventory(userId, inventoryId, value);
        RefreshUtil.fireSingle(single, new UserInventoryCache.InventoryKey(userId, inventoryId));
        return inventory;
    }

    @Override
    public int delete(int userId, int inventoryId) {
        if (userId < 1 || inventoryId < 1) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE user_id = ? AND inventory_id = ?";
        int row = database.update(sql, stmt -> {
            stmt.setInt(1, userId);
            stmt.setInt(2, inventoryId);
        });
        if (row < 1) return 0;

        RefreshUtil.fireSingle(single, new UserInventoryCache.InventoryKey(userId, inventoryId));
        return row;
    }

    @Override
    public UserInventory update(int userId, int inventoryId, String value) {
        if (userId < 1 || inventoryId < 1 || value == null) return null;

        UserInventory existing = cache.get(userId, inventoryId);
        if (existing == null) return null;

        if (Objects.equals(value, existing.value())) return existing;

        String sql = "UPDATE " + TABLE_NAME + " SET inventory_value = ? WHERE user_id = ? AND inventory_id = ?";
        int row = database.update(sql, stmt -> {
            stmt.setString(1, value);
            stmt.setInt(2, userId);
            stmt.setInt(3, inventoryId);
        });
        if (row < 1) return null;

        UserInventory inventory = existing.withValue(value);
        RefreshUtil.fireSingle(single, new UserInventoryCache.InventoryKey(userId, inventoryId));
        return inventory;
    }
}
