package de.murmelmeister.murmelapi.punishment.user;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

final class PunishmentUserRowMapper {
    static PunishmentUser resultSet(@NotNull ResultSet resultSet) throws SQLException {
        UUID mojangId = UUID.fromString(resultSet.getString("mojang_id"));
        int typeId = resultSet.getInt("type_id");
        UUID auditId = UUID.fromString(resultSet.getString("audit_id"));
        LocalDateTime expiresAt = resultSet.getObject("expires_at", LocalDateTime.class);
        return new PunishmentUserImpl(mojangId, typeId, auditId, expiresAt);
    }
}
