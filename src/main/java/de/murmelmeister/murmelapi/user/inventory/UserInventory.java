package de.murmelmeister.murmelapi.user.inventory;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record UserInventory(
        int userId,
        int inventoryId,
        @NotNull String value
) {
    public UserInventory {
        Objects.requireNonNull(value, "value cannot be null");
    }

    public UserInventory withValue(String value) {
        return new UserInventory(userId, inventoryId, value != null ? value : this.value);
    }
}
