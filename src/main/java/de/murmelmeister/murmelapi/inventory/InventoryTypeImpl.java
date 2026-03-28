package de.murmelmeister.murmelapi.inventory;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

record InventoryTypeImpl(
        int id,
        @NotNull String name
) implements InventoryType {
    public InventoryTypeImpl {
        Objects.requireNonNull(name, "name cannot be null");
        if (name.length() > 100) throw new IllegalArgumentException("name cannot be longer than 100 characters");
    }
}
