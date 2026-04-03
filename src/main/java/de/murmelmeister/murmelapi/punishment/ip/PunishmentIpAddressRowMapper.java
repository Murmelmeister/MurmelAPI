package de.murmelmeister.murmelapi.punishment.ip;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

final class PunishmentIpAddressRowMapper {
    static PunishmentIpAddress resultSet(ResultSet resultSet) throws SQLException {
        String ipAddress = resultSet.getString("ip_address");
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(ipAddress);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        int typeId = resultSet.getInt("type_id");
        UUID auditId = UUID.fromString(resultSet.getString("audit_id"));
        LocalDateTime expiresAt = resultSet.getObject("expires_at", LocalDateTime.class);
        return new PunishmentIpAddressImpl(inetAddress, typeId, auditId, expiresAt);
    }
}
