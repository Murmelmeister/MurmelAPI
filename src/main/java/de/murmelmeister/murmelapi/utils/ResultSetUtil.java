package de.murmelmeister.murmelapi.utils;

import de.murmelmeister.library.database.ResultSetProcessor;
import de.murmelmeister.murmelapi.clan.Clan;
import de.murmelmeister.murmelapi.clan.group.ClanGroup;
import de.murmelmeister.murmelapi.clan.member.ClanMember;
import de.murmelmeister.murmelapi.clan.parent.ClanParent;
import de.murmelmeister.murmelapi.clan.permission.ClanPermission;
import de.murmelmeister.murmelapi.maintenance.Maintenance;
import de.murmelmeister.murmelapi.maintenance.MaintenanceType;
import de.murmelmeister.murmelapi.maintenance.whitelist.MaintenanceWhitelist;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentAudit;
import de.murmelmeister.murmelapi.punishment.ip.PunishmentIpAddress;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;
import de.murmelmeister.murmelapi.punishment.user.PunishmentUser;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.excuse.UserExcuse;
import de.murmelmeister.murmelapi.user.inventory.UserInventory;
import de.murmelmeister.murmelapi.user.login.UserLogin;
import de.murmelmeister.murmelapi.user.session.UserSession;
import de.murmelmeister.murmelapi.user.stats.UserStats;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public final class ResultSetUtil {
    public static @NotNull ResultSetProcessor<User> user() {
        return resultSet -> {
            int id = resultSet.getInt("id");
            UUID mojangId = UUID.fromString(resultSet.getString("mojang_id"));
            String username = resultSet.getString("username");
            LocalDateTime firstJoin = resultSet.getObject("first_login", LocalDateTime.class);
            boolean isSystemUser = resultSet.getBoolean("system_user");
            boolean isDebugUser = resultSet.getBoolean("debug_user");
            boolean isDebugActive = resultSet.getBoolean("debug_enabled");
            int languageId = resultSet.getInt("language_id");
            return new User(id, mojangId, username, firstJoin, isSystemUser, isDebugUser, isDebugActive, languageId);
        };
    }

    public static @NotNull ResultSetProcessor<UserSession> userSession() {
        return resultSet -> {
            UUID id = UUID.fromString(resultSet.getString("id"));
            int userId = resultSet.getInt("user_id");
            LocalDateTime loginTime = resultSet.getTimestamp("login_time").toLocalDateTime();
            String ipAddress = resultSet.getString("ip_address");
            InetAddress inetAddress;
            try {
                inetAddress = InetAddress.getByName(ipAddress);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            String clientBrand = resultSet.getString("client_brand");
            int protocolVersion = resultSet.getInt("protocol_version");

            return new UserSession(id, userId, loginTime, inetAddress, clientBrand, protocolVersion);
        };
    }

    public static @NotNull ResultSetProcessor<UserLogin> userLogin() {
        return resultSet -> {
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

            return new UserLogin(id, userId, loginTime, logoutTime, inetAddress, clientBrand, protocolVersion);
        };
    }

    public static @NotNull ResultSetProcessor<PunishmentAudit> punishmentAudit() {
        return resultSet -> {
            UUID id = UUID.fromString(resultSet.getString("id"));
            PunishmentAudit.Action action = PunishmentAudit.Action.valueOf(resultSet.getString("action"));
            UUID mojangId = resultSet.getObject("mojang_id", UUID.class);
            String ipAddress = resultSet.getString("ip_address");
            InetAddress inetAddress;
            try {
                inetAddress = InetAddress.getByName(ipAddress);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            Integer reasonId = resultSet.getObject("reason_id", Integer.class);
            int reasonTypeId = resultSet.getInt("reason_type_id");
            String reasonText = resultSet.getString("reason_text");
            Long reasonDuration = resultSet.getObject("reason_duration", Long.class);
            boolean reasonAutoFlagIp = resultSet.getBoolean("reason_auto_flag_ip");
            int createdBy = resultSet.getInt("created_by");
            LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
            return new PunishmentAudit(id, action, mojangId, inetAddress, reasonId, reasonTypeId, reasonText,
                    reasonDuration, reasonAutoFlagIp, createdBy, createdAt);
        };
    }

    public static @NotNull ResultSetProcessor<PunishmentReason> punishmentReason() {
        return resultSet -> {
            int id = resultSet.getInt("id");
            int typeId = resultSet.getInt("type_id");
            String reasonText = resultSet.getString("reason_text");
            Long durationSecs = resultSet.getObject("duration_secs", Long.class);
            boolean autoFlagIp = resultSet.getBoolean("auto_flag_ip");
            int createdBy = resultSet.getInt("created_by");
            LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
            Integer changedBy = resultSet.getObject("changed_by", Integer.class);
            LocalDateTime changedAt = resultSet.getObject("changed_at", LocalDateTime.class);
            return new PunishmentReason(id, typeId, reasonText, durationSecs, autoFlagIp,
                    createdBy, createdAt, changedBy, changedAt);
        };
    }

    public static @NotNull ResultSetProcessor<PunishmentIpAddress> punishmentIpAddress() {
        return resultSet -> {
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
            return new PunishmentIpAddress(inetAddress, typeId, auditId, expiresAt);
        };
    }

    public static @NotNull ResultSetProcessor<PunishmentUser> punishmentUser() {
        return resultSet -> {
            UUID mojangId = UUID.fromString(resultSet.getString("mojang_id"));
            int typeId = resultSet.getInt("type_id");
            UUID auditId = UUID.fromString(resultSet.getString("audit_id"));
            LocalDateTime expiresAt = resultSet.getObject("expires_at", LocalDateTime.class);
            return new PunishmentUser(mojangId, typeId, auditId, expiresAt);
        };
    }

    public static @NotNull ResultSetProcessor<Clan> clan() {
        return resultSet -> {
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
            return new Clan(id, clanName, tag, sign, description, ownerId, createdBy, createdAt, changedBy, changedAt);
        };
    }

    public static @NotNull ResultSetProcessor<ClanMember> clanMember() {
        return resultSet -> {
            UUID clanId = UUID.fromString(resultSet.getString("clan_id"));
            int userId = resultSet.getInt("user_id");
            LocalDateTime joinedAt = resultSet.getTimestamp("joined_at").toLocalDateTime();
            UUID clan_group_id = UUID.fromString(resultSet.getString("group_id"));
            return new ClanMember(clanId, userId, joinedAt, clan_group_id);
        };
    }

    public static @NotNull ResultSetProcessor<ClanGroup> clanGroup() {
        return resultSet -> {
            UUID clanId = UUID.fromString(resultSet.getString("clan_id"));
            UUID groupId = UUID.fromString(resultSet.getString("group_id"));
            String groupName = resultSet.getString("group_name");
            int priority = resultSet.getInt("priority");
            boolean isDefault = resultSet.getBoolean("is_default");
            LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
            int createdBy = resultSet.getInt("created_by");
            LocalDateTime changedAt = resultSet.getObject("changed_at", LocalDateTime.class);
            Integer changedBy = resultSet.getObject("changed_by", Integer.class);
            return new ClanGroup(clanId, groupId, groupName, priority, isDefault, createdBy, createdAt, changedBy, changedAt);
        };
    }

    public static @NotNull ResultSetProcessor<ClanParent> clanParent() {
        return resultSet -> {
            UUID clanId = UUID.fromString(resultSet.getString("clan_id"));
            UUID groupId = UUID.fromString(resultSet.getString("group_id"));
            int parentId = resultSet.getInt("parent_id");
            LocalDateTime expiresAt = resultSet.getObject("expires_at", LocalDateTime.class);
            LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
            int createdBy = resultSet.getInt("created_by");
            LocalDateTime changedAt = resultSet.getObject("changed_at", LocalDateTime.class);
            Integer changedBy = resultSet.getObject("changed_by", Integer.class);
            return new ClanParent(clanId, groupId, parentId, expiresAt, createdBy, createdAt, changedBy, changedAt);
        };
    }

    public static @NotNull ResultSetProcessor<ClanPermission> clanPermission() {
        return resultSet -> {
            UUID clanId = UUID.fromString(resultSet.getString("clan_id"));
            UUID groupId = UUID.fromString(resultSet.getString("group_id"));
            String permission = resultSet.getString("permission");
            LocalDateTime expiresAt = resultSet.getObject("expires_at", LocalDateTime.class);
            LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
            int createdBy = resultSet.getInt("created_by");
            LocalDateTime changedAt = resultSet.getObject("changed_at", LocalDateTime.class);
            Integer changedBy = resultSet.getObject("changed_by", Integer.class);
            return new ClanPermission(clanId, groupId, permission, expiresAt, createdBy, createdAt, changedBy, changedAt);
        };
    }

    public static @NotNull ResultSetProcessor<UserInventory> userInventory() {
        return resultSet -> {
            int userId = resultSet.getInt("user_id");
            int inventoryId = resultSet.getInt("inventory_id");
            String value = resultSet.getString("inventory_value");
            return new UserInventory(userId, inventoryId, value);
        };
    }

    public static @NotNull ResultSetProcessor<UserStats> userStats() {
        return resultSet -> {
            int userId = resultSet.getInt("id");
            int playTime = resultSet.getInt("play_time");
            int dailyStreak = resultSet.getInt("daily_streak");
            LocalDate lastDay = resultSet.getObject("daily_streak_last_day", LocalDate.class);
            LocalDateTime lastSeen = resultSet.getObject("last_seen_at", LocalDateTime.class);
            return new UserStats(userId, playTime, dailyStreak, lastDay, lastSeen);
        };
    }

    public static @NotNull ResultSetProcessor<Maintenance> maintenance() {
        return resultSet -> {
            int id = resultSet.getInt("id");
            String title = resultSet.getString("title");
            String reason = resultSet.getString("reason");
            MaintenanceType status = MaintenanceType.valueOf(resultSet.getString("status"));
            LocalDateTime startAt = resultSet.getTimestamp("start_at").toLocalDateTime();
            LocalDateTime endAt = resultSet.getTimestamp("end_at").toLocalDateTime();
            LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
            int createdBy = resultSet.getInt("created_by");
            LocalDateTime changedAt = resultSet.getObject("changed_at", LocalDateTime.class);
            Integer changedBy = resultSet.getObject("changed_by", Integer.class);
            return new Maintenance(id, title, reason, status, startAt, endAt, createdAt, createdBy, changedAt, changedBy);
        };
    }

    public static @NotNull ResultSetProcessor<MaintenanceWhitelist> maintenanceWhitelist() {
        return resultSet -> {
            int id = resultSet.getInt("id");
            int maintenanceId = resultSet.getInt("maintenance_id");
            int userId = resultSet.getInt("user_id");
            LocalDateTime startAt = resultSet.getObject("start_at", LocalDateTime.class);
            LocalDateTime endAt = resultSet.getObject("end_at", LocalDateTime.class);
            String note = resultSet.getString("note");
            LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
            int createdBy = resultSet.getInt("created_by");
            LocalDateTime changedAt = resultSet.getObject("changed_at", LocalDateTime.class);
            Integer changedBy = resultSet.getObject("changed_by", Integer.class);
            return new MaintenanceWhitelist(id, maintenanceId, userId, startAt, endAt, note, createdAt, createdBy, changedAt, changedBy);
        };
    }

    public static @NotNull ResultSetProcessor<UserExcuse> userExcuse() {
        return resultSet -> {
            int id = resultSet.getInt("id");
            int userId = resultSet.getInt("user_id");
            LocalDateTime startAt = resultSet.getTimestamp("start_at").toLocalDateTime();
            LocalDateTime endAt = resultSet.getTimestamp("end_at").toLocalDateTime();
            String reason = resultSet.getString("reason");
            LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
            int createdBy = resultSet.getInt("created_by");
            LocalDateTime changedAt = resultSet.getObject("changed_at", LocalDateTime.class);
            Integer changedBy = resultSet.getObject("changed_by", Integer.class);
            return new UserExcuse(id, userId, startAt, endAt, reason, createdAt, createdBy, changedAt, changedBy);
        };
    }
}
