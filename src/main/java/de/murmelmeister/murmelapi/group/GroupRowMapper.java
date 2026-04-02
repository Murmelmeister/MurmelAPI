package de.murmelmeister.murmelapi.group;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

final class GroupRowMapper {
    static Group resultSet(@NotNull ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        String groupName = resultSet.getString("group_name");
        int priority = resultSet.getInt("priority");
        boolean isDefault = resultSet.getBoolean("is_default");
        int createdBy = resultSet.getInt("created_by");
        LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
        Integer changedBy = resultSet.getObject("changed_by", Integer.class);
        LocalDateTime changedAt = resultSet.getObject("changed_at", LocalDateTime.class);
        return new GroupImpl(id, groupName, priority, isDefault, createdBy, createdAt, changedBy, changedAt);
    }
}
