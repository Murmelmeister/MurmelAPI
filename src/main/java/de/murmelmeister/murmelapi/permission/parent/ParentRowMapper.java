package de.murmelmeister.murmelapi.permission.parent;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

final class ParentRowMapper {
    static Parent resultSet(@NotNull ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        Integer userId = resultSet.getObject("user_id", Integer.class);
        Integer groupId = resultSet.getObject("group_id", Integer.class);
        int parentId = resultSet.getInt("parent_id");
        LocalDateTime expiresAt = resultSet.getObject("expires_at", LocalDateTime.class);
        int createdBy = resultSet.getInt("created_by");
        LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
        Integer changedBy = resultSet.getObject("changed_by", Integer.class);
        LocalDateTime changedAt = resultSet.getObject("changed_at", LocalDateTime.class);
        return new ParentImpl(id, userId, groupId, parentId, expiresAt, createdBy, createdAt, changedBy, changedAt);
    }
}
