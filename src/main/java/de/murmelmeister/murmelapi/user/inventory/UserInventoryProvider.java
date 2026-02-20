package de.murmelmeister.murmelapi.user.inventory;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public interface UserInventoryProvider {
    void refreshCache();

    @Nullable UserInventory findById(int userId, int inventoryId);

    @NotNull
    @Unmodifiable
    List<UserInventory> findAll();

    @Nullable UserInventory create(int userId, int inventoryId, @NotNull String value);

    int delete(int userId, int inventoryId);

    @Nullable UserInventory update(int userId, int inventoryId, @NotNull String value);
}
