package de.murmelmeister.murmelapi.inventory;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record InventoryType(int id, @NotNull String name) {
    public InventoryType {
        Objects.requireNonNull(name, "name cannot be null");
    }
}
