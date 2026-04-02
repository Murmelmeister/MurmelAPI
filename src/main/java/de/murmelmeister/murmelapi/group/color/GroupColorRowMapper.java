package de.murmelmeister.murmelapi.group.color;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

final class GroupColorRowMapper {
    static GroupColor resultSet(@NotNull ResultSet resultSet) throws SQLException {
        int groupId = resultSet.getInt("group_id");
        int typeId = resultSet.getInt("type_id");
        String value = resultSet.getString("value");
        int createdBy = resultSet.getInt("created_by");
        LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
        Integer changedBy = resultSet.getObject("changed_by", Integer.class);
        LocalDateTime changedAt = resultSet.getObject("changed_at", LocalDateTime.class);
        return new GroupColorImpl(groupId, typeId, value, createdBy, createdAt, changedBy, changedAt);
    }
}
