package de.murmelmeister.murmelapi.inventory;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.inventory.InventoryException;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class InventoryTypeProviderImpl implements InventoryTypeProvider {
    private static final String TABLE_NAME = "inventory_type";

    @Language("MariaDB")
    private static final String CREATE_SQL = """
            INSERT INTO %s (inventory_name)
            VALUES (?)
            RETURNING id, inventory_name
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String DELETE_SQL = "DELETE FROM %s WHERE id = ?".formatted(TABLE_NAME);

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final InventoryTypeCache cache;
    private final RefreshType all = RefreshType.INVENTORY_TYPES;
    private final RefreshType single = RefreshType.SINGLE_INVENTORY_TYPE;

    public InventoryTypeProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new InventoryTypeCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @NotNull Optional<InventoryType> findById(int id) {
        return cache.getById(id);
    }

    @Override
    public @NotNull @Unmodifiable List<InventoryType> findAll() {
        return cache.getAll();
    }

    @Override
    public @NotNull Optional<InventoryType> create(@NotNull String name) {
        Objects.requireNonNull(name, "name cannot be null");
        String normalizedName = StringUtil.normalize(name);
        if (normalizedName == null || normalizedName.isBlank())
            throw new IllegalArgumentException("name cannot be blank");

        InventoryType type = MurmelExceptionWrapper.dbWrap(
                "Failed to create InventoryType (name=" + normalizedName + ")",
                () -> database.query(CREATE_SQL, null, InventoryTypeAdapter::resultSet,
                        stmt -> stmt.setString(1, normalizedName)),
                InventoryException::new
        );

        if (type == null) return Optional.empty();
        refreshProvider.fireSingle(single, type);
        return Optional.of(type);
    }

    @Override
    public int delete(int id) {
        Optional<InventoryType> existing = cache.getById(id);
        if (existing.isEmpty()) return 0;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete InventoryType (id=" + id + ")",
                () -> database.update(DELETE_SQL, stmt -> stmt.setInt(1, id)),
                InventoryException::new
        );

        if (row < 1) return 0;
        refreshProvider.fireSingle(single, existing.get());
        return row;
    }
}
