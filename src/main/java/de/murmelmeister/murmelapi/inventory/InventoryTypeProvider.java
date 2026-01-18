package de.murmelmeister.murmelapi.inventory;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface InventoryTypeProvider {
    void refreshCache();

    @Nullable InventoryType findById(int id);

    @NotNull List<InventoryType> findAll();

    @Nullable InventoryType create(@NotNull String name);

    int delete(int id);
}
