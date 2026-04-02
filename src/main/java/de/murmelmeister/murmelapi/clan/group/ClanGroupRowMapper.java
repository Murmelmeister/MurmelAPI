package de.murmelmeister.murmelapi.clan.group;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

final class ClanGroupRowMapper {
    static ClanGroup resultSet(@NotNull ResultSet resultSet) throws SQLException {
        UUID clanId = UUID.fromString(resultSet.getString("clan_id"));
        UUID groupId = UUID.fromString(resultSet.getString("group_id"));
        String groupName = resultSet.getString("group_name");
        int priority = resultSet.getInt("priority");
        boolean isDefault = resultSet.getBoolean("is_default");
        LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
        int createdBy = resultSet.getInt("created_by");
        LocalDateTime changedAt = resultSet.getObject("changed_at", LocalDateTime.class);
        Integer changedBy = resultSet.getObject("changed_by", Integer.class);
        return new ClanGroupImpl(clanId, groupId, groupName, priority, isDefault, createdBy, createdAt, changedBy, changedAt);
    }
}
