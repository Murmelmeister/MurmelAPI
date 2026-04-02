package de.murmelmeister.murmelapi.user.inventory;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;

record UserInventoryImpl(
        int userId,
        int inventoryId,
        @NotNull String value
) implements UserInventory {

    public UserInventoryImpl {
        Objects.requireNonNull(value, "value cannot be null");
        if (userId < 1) throw new IllegalArgumentException("userId cannot be >= 1");
    }

    public @NotNull UserInventory.Builder builder() {
        return new Builder(this);
    }

    public @NotNull UserInventory with(@NotNull Consumer<UserInventory.Builder> consumer) {
        UserInventory.Builder builder = this.builder();
        consumer.accept(builder);
        return builder.build();
    }

    static final class Builder implements UserInventory.Builder {
        private final int userId;
        private final int inventoryId;

        private String value;

        private Builder(@NotNull UserInventory userInventory) {
            this.userId = userInventory.userId();
            this.inventoryId = userInventory.inventoryId();
            this.value = userInventory.value();
        }

        public @NotNull UserInventory.Builder value(@NotNull String value) {
            this.value = value;
            return this;
        }

        public @NotNull UserInventory build() {
            return new UserInventoryImpl(userId, inventoryId, value);
        }
    }
}
