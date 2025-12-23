package de.murmelmeister.murmelapi.inventory;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;
import java.util.List;

public final class InventoryTypeProviderImpl implements InventoryTypeProvider {
    private static final String TABLE_NAME = "inventory_type";

    private final Database database;
    private final InventoryTypeCache cache;
    private final RefreshType all = RefreshType.INVENTORY_TYPES;
    private final RefreshType single = RefreshType.SINGLE_INVENTORY_TYPE;

    public InventoryTypeProviderImpl(Database database, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.cache = new InventoryTypeCache(database, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        RefreshUtil.fireCache(all);
    }

    @Override
    public InventoryType findById(int id) {
        return cache.getById(id);
    }

    @Override
    public List<InventoryType> findAll() {
        return cache.getAll();
    }

    @Override
    public InventoryType create(String name) {
        String normalizedName = StringUtil.normalize(name);
        if (normalizedName == null)
            return null;

        String sql = "INSERT INTO " + TABLE_NAME + " (inventory_name) VALUES (?)";
        int id = (int) database.updateAndGetGeneratedKeys(sql, stmt -> stmt.setString(1, normalizedName));
        if (id < 1) return null;

        InventoryType type = new InventoryType(id, normalizedName);
        RefreshUtil.fireSingle(single, type.id());
        return type;
    }

    @Override
    public int delete(int id) {
        if (id < 1) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        int row = database.update(sql, stmt -> stmt.setInt(1, id));
        if (row < 1) return 0;

        RefreshUtil.fireSingle(single, id);
        return row;
    }
}
