package de.murmelmeister.murmelapi.user.inventory;

import java.util.List;

public interface UserInventoryProvider {
    void refreshCache();

    UserInventory findById(int userId, int inventoryId);

    List<UserInventory> findAll();

    UserInventory create(int userId, int inventoryId, String value);

    int delete(int userId, int inventoryId);

    UserInventory update(int userId, int inventoryId, String value);
}
