package de.murmelmeister.murmelapi.user.inventory;

import org.jetbrains.annotations.NotNull;

public record UserInventory(int userId, int inventoryId, @NotNull String value) {
    public UserInventory withValue(String value) {
        return new UserInventory(userId, inventoryId, value != null ? value : this.value);
    }
}
