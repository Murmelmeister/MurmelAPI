package de.murmelmeister.murmelapi.inventory;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;

final class InventoryTypeRowMapper {
    static InventoryType resultSet(@NotNull ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        String name = resultSet.getString("inventory_name");
        return new InventoryTypeImpl(id, name);
    }
}
