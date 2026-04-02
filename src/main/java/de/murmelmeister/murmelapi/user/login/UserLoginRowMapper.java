package de.murmelmeister.murmelapi.user.login;

import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

final class UserLoginRowMapper {
    static UserLogin resultSet(@NotNull ResultSet resultSet) throws SQLException {
        UUID id = UUID.fromString(resultSet.getString("id"));
        int userId = resultSet.getInt("user_id");
        LocalDateTime loginTime = resultSet.getTimestamp("login_time").toLocalDateTime();
        LocalDateTime logoutTime = resultSet.getTimestamp("logout_time").toLocalDateTime();
        String ipAddress = resultSet.getString("ip_address");
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(ipAddress);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        String clientBrand = resultSet.getString("client_brand");
        int protocolVersion = resultSet.getInt("protocol_version");

        return new UserLoginImpl(id, userId, loginTime, logoutTime, inetAddress, clientBrand, protocolVersion);
    }
}
