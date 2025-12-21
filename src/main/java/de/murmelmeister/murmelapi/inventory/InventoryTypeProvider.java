package de.murmelmeister.murmelapi.inventory;

import java.util.List;

public interface InventoryTypeProvider {
    void refreshCache();

    InventoryType findById(int id);

    List<InventoryType> findAll();

    InventoryType create(String name);

    int delete(int id);
}
