package de.murmelmeister.murmelapi.inventory;

import org.jetbrains.annotations.NotNull;

public interface InventoryType {
    int id();

    @NotNull String name();
}
