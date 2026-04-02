package de.murmelmeister.murmelapi.user.inventory;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public interface UserInventory {
    int userId();

    int inventoryId();

    @NotNull String value();

    @NotNull Builder builder();

    @NotNull UserInventory with(@NotNull Consumer<Builder> consumer);

    static @NotNull UserInventory of(int userId, int inventoryId, @NotNull String value) {
        return new UserInventoryImpl(userId, inventoryId, value);
    }

    interface Builder {
        @NotNull Builder value(@NotNull String value);

        @NotNull UserInventory build();
    }
}
