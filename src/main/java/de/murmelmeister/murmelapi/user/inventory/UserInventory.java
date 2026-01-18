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

    public static @NotNull Builder builder(@NotNull UserInventory userInventory) {
        return new Builder(userInventory);
    }

    public static class Builder {
        private final int userId;
        private final int inventoryId;

        private String value;

        private Builder(@NotNull UserInventory userInventory) {
            this.userId = userInventory.userId();
            this.inventoryId = userInventory.inventoryId();
            this.value = userInventory.value();
        }

        public Builder value(@NotNull String value) {
            this.value = value;
            return this;
        }

        public @NotNull UserInventory build() {
            return new UserInventory(userId, inventoryId, value);
        }
    }
}
