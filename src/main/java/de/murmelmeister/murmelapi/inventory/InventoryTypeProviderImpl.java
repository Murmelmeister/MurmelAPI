package de.murmelmeister.murmelapi.inventory;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;

public final class InventoryTypeProviderImpl implements InventoryTypeProvider {
    private static final String TABLE_NAME = "inventory_type";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final InventoryTypeCache cache;
    private final RefreshType all = RefreshType.INVENTORY_TYPES;
    private final RefreshType single = RefreshType.SINGLE_INVENTORY_TYPE;

    public InventoryTypeProviderImpl(Database database, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new InventoryTypeCache(database, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @Nullable InventoryType findById(int id) {
        return cache.getById(id);
    }

    @Override
    public @NotNull List<InventoryType> findAll() {
        return cache.getAll();
    }

    @Override
    public @Nullable InventoryType create(@NotNull String name) {
        String normalizedName = StringUtil.normalize(name);
        if (normalizedName == null)
            return null;

        @Language("MariaDB")
        String sql = "INSERT INTO %s (inventory_name) VALUES (?)".formatted(TABLE_NAME);
        int id = (int) database.updateAndGetGeneratedKeys(sql, stmt -> stmt.setString(1, normalizedName));
        if (id < 1) return null;

        InventoryType type = new InventoryType(id, normalizedName);
        refreshProvider.fireSingle(single, type);
        return type;
    }

    @Override
    public int delete(int id) {
        if (id < 1) return 0;
        InventoryType existing = cache.getById(id);
        if (existing == null) return 0;

        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE id = ?".formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> stmt.setInt(1, id));
        if (row < 1) return 0;

        refreshProvider.fireSingle(single, existing);
        return row;
    }
}
