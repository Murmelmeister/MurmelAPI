package de.murmelmeister.murmelapi.user.inventory;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;

final class UserInventoryRowMapper {
    static UserInventory resultSet(@NotNull ResultSet resultSet) throws SQLException {
        int userId = resultSet.getInt("user_id");
        int inventoryId = resultSet.getInt("inventory_id");
        String value = resultSet.getString("inventory_value");
        return new UserInventoryImpl(userId, inventoryId, value);
    }
}
