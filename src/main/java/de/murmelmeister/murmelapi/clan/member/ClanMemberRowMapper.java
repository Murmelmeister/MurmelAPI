package de.murmelmeister.murmelapi.clan.member;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

final class ClanMemberRowMapper {
    static ClanMember resultSet(@NotNull ResultSet resultSet) throws SQLException {
        UUID clanId = UUID.fromString(resultSet.getString("clan_id"));
        int userId = resultSet.getInt("user_id");
        LocalDateTime joinedAt = resultSet.getTimestamp("joined_at").toLocalDateTime();
        UUID clan_group_id = UUID.fromString(resultSet.getString("group_id"));
        return new ClanMemberImpl(clanId, userId, joinedAt, clan_group_id);
    }
}
