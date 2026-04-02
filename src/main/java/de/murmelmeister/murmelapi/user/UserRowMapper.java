package de.murmelmeister.murmelapi.user;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

final class UserRowMapper {
    static User resultSet(@NotNull ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        UUID mojangId = UUID.fromString(resultSet.getString("mojang_id"));
        String username = resultSet.getString("username");
        LocalDateTime firstJoin = resultSet.getObject("first_login", LocalDateTime.class);
        boolean isSystemUser = resultSet.getBoolean("system_user");
        boolean isDebugUser = resultSet.getBoolean("debug_user");
        boolean isDebugActive = resultSet.getBoolean("debug_enabled");
        int languageId = resultSet.getInt("language_id");
        return new UserImpl(id, mojangId, username, firstJoin, isSystemUser, isDebugUser, isDebugActive, languageId);
    }
}
