package de.murmelmeister.murmelapi.inventory;

import org.jetbrains.annotations.NotNull;

public interface InventoryType {
    int id();

    @NotNull String name();

    static @NotNull InventoryType of(int id, @NotNull String name) {
        return new InventoryTypeImpl(id, name);
    }
}
