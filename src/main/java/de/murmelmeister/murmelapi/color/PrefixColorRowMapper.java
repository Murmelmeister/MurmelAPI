package de.murmelmeister.murmelapi.color;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

final class PrefixColorRowMapper {
    static PrefixColor resultSet(@NotNull ResultSet resultSet) throws SQLException {
        String id = resultSet.getString("id");
        String color = resultSet.getString("color");
        boolean animated = resultSet.getBoolean("animated");
        LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
        int createdBy = resultSet.getInt("created_by");
        LocalDateTime changedAt = resultSet.getObject("changed_at", LocalDateTime.class);
        Integer changedBy = resultSet.getObject("changed_by", Integer.class);
        return new PrefixColorImpl(id, color, animated, createdAt, createdBy, changedAt, changedBy);
    }
}
