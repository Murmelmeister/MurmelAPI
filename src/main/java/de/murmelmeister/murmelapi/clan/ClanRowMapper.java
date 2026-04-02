package de.murmelmeister.murmelapi.clan;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

final class ClanRowMapper {
    static Clan resultSet(@NotNull ResultSet resultSet) throws SQLException {
        UUID id = UUID.fromString(resultSet.getString("id"));
        String clanName = resultSet.getString("name");
        String tag = resultSet.getString("tag");
        String sign = resultSet.getString("sign");
        String description = resultSet.getString("description");
        int ownerId = resultSet.getInt("owner_id");
        int createdBy = resultSet.getInt("created_by");
        LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
        Integer changedBy = resultSet.getObject("changed_by", Integer.class);
        LocalDateTime changedAt = resultSet.getObject("changed_at", LocalDateTime.class);
        return new ClanImpl(id, clanName, tag, sign, description, ownerId, createdBy, createdAt, changedBy, changedAt);
    }
}
