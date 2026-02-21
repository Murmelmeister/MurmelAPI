package de.murmelmeister.murmelapi.inventory;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record InventoryType(int id, @NotNull String name) {
    public InventoryType {
        Objects.requireNonNull(name, "name cannot be null");
        if (name.length() > 100) throw new IllegalArgumentException("name cannot be longer than 100 characters");
    }
}
