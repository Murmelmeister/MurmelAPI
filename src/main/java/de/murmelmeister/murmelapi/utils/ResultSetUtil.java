package de.murmelmeister.murmelapi.utils;

import de.murmelmeister.library.database.ResultSetProcessor;
import de.murmelmeister.murmelapi.clan.Clan;
import de.murmelmeister.murmelapi.clan.group.ClanGroup;
import de.murmelmeister.murmelapi.clan.member.ClanMember;
import de.murmelmeister.murmelapi.clan.parent.ClanParent;
import de.murmelmeister.murmelapi.clan.permission.ClanPermission;
import de.murmelmeister.murmelapi.color.PrefixColor;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.color.GroupColor;
import de.murmelmeister.murmelapi.group.parent.GroupParent;
import de.murmelmeister.murmelapi.group.permission.GroupPermission;
import de.murmelmeister.murmelapi.inventory.InventoryType;
import de.murmelmeister.murmelapi.language.Language;
import de.murmelmeister.murmelapi.language.message.Message;
import de.murmelmeister.murmelapi.maintenance.Maintenance;
import de.murmelmeister.murmelapi.maintenance.MaintenanceType;
import de.murmelmeister.murmelapi.maintenance.whitelist.MaintenanceWhitelist;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentLog;
import de.murmelmeister.murmelapi.punishment.ip.PunishmentCurrentIp;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;
import de.murmelmeister.murmelapi.punishment.user.PunishmentCurrentUser;
import de.murmelmeister.murmelapi.settings.Settings;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.color.UserPrefixColor;
import de.murmelmeister.murmelapi.user.inventory.UserInventory;
import de.murmelmeister.murmelapi.user.login.UserLogin;
import de.murmelmeister.murmelapi.user.excuse.UserExcuse;
import de.murmelmeister.murmelapi.user.parent.UserParent;
import de.murmelmeister.murmelapi.user.permission.UserPermission;
import de.murmelmeister.murmelapi.user.session.UserSession;
import de.murmelmeister.murmelapi.user.stats.UserStats;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static de.murmelmeister.murmelapi.MurmelAPI.ENGLISH_CODE;

public final class ResultSetUtil {
    public static @NotNull ResultSetProcessor<Settings> settings() {
        return resultSet -> {
            String tagId = resultSet.getString("tag_id");
            String json = resultSet.getString("value_json");
            LocalDateTime updatedAt = resultSet.getTimestamp("updated_at").toLocalDateTime();
            return new Settings(tagId, json, updatedAt);
        };
    }

    public static @NotNull ResultSetProcessor<Language> language() {
        return resultSet -> {
            int id = resultSet.getInt("id");
            String code = resultSet.getString("code");
            return new Language(id, code != null && !code.isBlank() ? code : ENGLISH_CODE);
        };
    }

    public static @NotNull ResultSetProcessor<Message> message() {
        return resultSet -> {
            int id = resultSet.getInt("id");
            String tag = resultSet.getString("tag_id");
            int languageId = resultSet.getInt("language_id");
            String message = resultSet.getString("message");
            return new Message(id, tag, languageId, message);
        };
    }

    public static @NotNull ResultSetProcessor<User> user() {
        return resultSet -> {
            int id = resultSet.getInt("id");
            UUID mojangId = UUID.fromString(resultSet.getString("mojang_id"));
            String username = resultSet.getString("username");
            LocalDateTime firstJoin = resultSet.getObject("first_login") != null ? resultSet.getTimestamp("first_login").toLocalDateTime() : null;
            boolean isSystemUser = resultSet.getBoolean("system_user");
            boolean isDebugUser = resultSet.getBoolean("debug_user");
            boolean isDebugActive = resultSet.getBoolean("debug_enabled");
            int languageId = resultSet.getInt("language_id");
            return new User(id, mojangId, username, firstJoin, isSystemUser, isDebugUser, isDebugActive, languageId);
        };
    }

    public static @NotNull ResultSetProcessor<UserSession> userSession() {
        return result -> {
            UUID id = UUID.fromString(result.getString("id"));
            int userId = result.getInt("user_id");
            LocalDateTime loginTime = result.getTimestamp("login_time").toLocalDateTime();
            String ipAddress = result.getString("ip_address");
            InetAddress inetAddress;
            try {
                inetAddress = InetAddress.getByName(ipAddress);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            String clientBrand = result.getString("client_brand");
            int protocolVersion = result.getInt("protocol_version");

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

    public static @NotNull ResultSetProcessor<UserPermission> userPermission() {
        return resultSet -> {
            int userId = resultSet.getInt("user_id");
            String permission = resultSet.getString("permission");
            LocalDateTime expires_at = resultSet.getObject("expires_at") != null ? resultSet.getTimestamp("expires_at").toLocalDateTime() : null;
            int createdBy = resultSet.getInt("created_by");
            LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
            Integer changedBy = resultSet.getObject("changed_by") != null ? resultSet.getInt("changed_by") : null;
            LocalDateTime changedAt = resultSet.getObject("changed_at") != null ? resultSet.getTimestamp("changed_at").toLocalDateTime() : null;
            return new UserPermission(userId, permission, expires_at, createdBy, createdAt, changedBy, changedAt);
        };
    }

    public static @NotNull ResultSetProcessor<UserParent> userParent() {
        return resultSet -> {
            int userId = resultSet.getInt("user_id");
            int parentId = resultSet.getInt("parent_id");
            LocalDateTime expiresAt = resultSet.getObject("expires_at") != null ? resultSet.getTimestamp("expires_at").toLocalDateTime() : null;
            int createdBy = resultSet.getInt("created_by");
            LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
            Integer changedBy = resultSet.getObject("changed_by") != null ? resultSet.getInt("changed_by") : null;
            LocalDateTime changedAt = resultSet.getObject("changed_at") != null ? resultSet.getTimestamp("changed_at").toLocalDateTime() : null;
            return new UserParent(userId, parentId, expiresAt, createdBy, createdAt, changedBy, changedAt);
        };
    }

    public static @NotNull ResultSetProcessor<Group> group() {
        return resultSet -> {
            int id = resultSet.getInt("id");
            String groupName = resultSet.getString("group_name");
            int priority = resultSet.getInt("priority");
            boolean isDefault = resultSet.getBoolean("is_default");
            int createdBy = resultSet.getInt("created_by");
            LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
            Integer changedBy = resultSet.getObject("changed_by") != null ? resultSet.getInt("changed_by") : null;
            LocalDateTime changedAt = resultSet.getObject("changed_at") != null ? resultSet.getTimestamp("changed_at").toLocalDateTime() : null;
            return new Group(id, groupName, priority, isDefault, createdBy, createdAt, changedBy, changedAt);
        };
    }

    public static @NotNull ResultSetProcessor<GroupColor> groupColor() {
        return resultSet -> {
            int groupId = resultSet.getInt("group_id");
            int typeId = resultSet.getInt("type_id");
            String value = resultSet.getString("value");
            int createdBy = resultSet.getInt("created_by");
            LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
            Integer changedBy = resultSet.getObject("changed_by") != null ? resultSet.getInt("changed_by") : null;
            LocalDateTime changedAt = resultSet.getObject("changed_at") != null ? resultSet.getTimestamp("changed_at").toLocalDateTime() : null;
            return new GroupColor(groupId, typeId, value, createdBy, createdAt, changedBy, changedAt);
        };
    }

    public static @NotNull ResultSetProcessor<GroupPermission> groupPermission() {
        return resultSet -> {
            int groupId = resultSet.getInt("group_id");
            String permission = resultSet.getString("permission");
            LocalDateTime expiresAt = resultSet.getObject("expires_at") != null ? resultSet.getTimestamp("expires_at").toLocalDateTime() : null;
            int createdBy = resultSet.getInt("created_by");
            LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
            Integer changedBy = resultSet.getObject("changed_by") != null ? resultSet.getInt("changed_by") : null;
            LocalDateTime changedAt = resultSet.getObject("changed_at") != null ? resultSet.getTimestamp("changed_at").toLocalDateTime() : null;
            return new GroupPermission(groupId, permission, expiresAt, createdBy, createdAt, changedBy, changedAt);
        };
    }

    public static @NotNull ResultSetProcessor<GroupParent> groupParent() {
        return resultSet -> {
            int groupId = resultSet.getInt("group_id");
            int parentId = resultSet.getInt("parent_id");
            LocalDateTime expiresAt = resultSet.getObject("expires_at") != null ? resultSet.getTimestamp("expires_at").toLocalDateTime() : null;
            int createdBy = resultSet.getInt("created_by");
            LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
            Integer changedBy = resultSet.getObject("changed_by") != null ? resultSet.getInt("changed_by") : null;
            LocalDateTime changedAt = resultSet.getObject("changed_at") != null ? resultSet.getTimestamp("changed_at").toLocalDateTime() : null;
            return new GroupParent(groupId, parentId, expiresAt, createdBy, createdAt, changedBy, changedAt);
        };
    }

    public static @NotNull ResultSetProcessor<PunishmentLog> punishmentLog() {
        return resultSet -> {
            UUID id = UUID.fromString(resultSet.getString("id"));
            PunishmentLog.Action action = PunishmentLog.Action.valueOf(resultSet.getString("action"));
            Integer userId = resultSet.getObject("user_id") == null ? null : resultSet.getInt("user_id");
            String ipAddress = resultSet.getString("ip_address");
            InetAddress inetAddress;
            try {
                inetAddress = InetAddress.getByName(ipAddress);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            Integer reasonId = resultSet.getObject("reason_id") == null ? null : resultSet.getInt("reason_id");
            int reasonTypeId = resultSet.getInt("reason_type_id");
            String reasonText = resultSet.getString("reason_text");
            Long reasonDuration = resultSet.getObject("reason_duration") == null ? null : resultSet.getLong("reason_duration");
            boolean reasonAutoFlagIp = resultSet.getBoolean("reason_auto_flag_ip");
            boolean reasonAutoPunish = resultSet.getBoolean("reason_auto_punish");
            int createdBy = resultSet.getInt("created_by");
            LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
            return new PunishmentLog(id, action, userId, inetAddress, reasonId, reasonTypeId, reasonText,
                    reasonDuration, reasonAutoFlagIp, reasonAutoPunish, createdBy, createdAt);
        };
    }

    public static @NotNull ResultSetProcessor<PunishmentReason> punishmentReason() {
        return result -> {
            int id = result.getInt("id");
            int typeId = result.getInt("type_id");
            String reasonText = result.getString("reason_text");
            Long durationSecs = result.getObject("duration_secs") == null ?
                    null : result.getLong("duration_secs");
            boolean autoFlagIp = result.getBoolean("auto_flag_ip");
            boolean autoPunish = result.getBoolean("auto_punish");
            int createdBy = result.getInt("created_by");
            LocalDateTime createdAt = result.getTimestamp("created_at").toLocalDateTime();
            Integer changedBy = result.getObject("changed_by") == null ?
                    null : result.getInt("changed_by");
            LocalDateTime changedAt = result.getObject("changed_at") == null ?
                    null : result.getTimestamp("changed_at").toLocalDateTime();
            return new PunishmentReason(id, typeId, reasonText, durationSecs, autoFlagIp, autoPunish,
                    createdBy, createdAt, changedBy, changedAt);
        };
    }

    public static @NotNull ResultSetProcessor<PunishmentCurrentIp> punishmentCurrentIp() {
        return result -> {
            String ipAddress = result.getString("ip_address");
            InetAddress inetAddress;
            try {
                inetAddress = InetAddress.getByName(ipAddress);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            int typeId = result.getInt("type_id");
            UUID logId = UUID.fromString(result.getString("log_id"));
            return new PunishmentCurrentIp(inetAddress, typeId, logId);
        };
    }

    public static @NotNull ResultSetProcessor<PunishmentCurrentUser> punishmentCurrentUser() {
        return result -> {
            int userId = result.getInt("user_id");
            int typeId = result.getInt("type_id");
            UUID logId = UUID.fromString(result.getString("log_id"));
            return new PunishmentCurrentUser(userId, typeId, logId);
        };
    }

    public static @NotNull ResultSetProcessor<Clan> clan() {
        return result -> {
            UUID id = UUID.fromString(result.getString("id"));
            String clanName = result.getString("name");
            String tag = result.getString("tag");
            String sign = result.getString("sign");
            String description = result.getString("description");
            int ownerId = result.getInt("owner_id");
            int createdBy = result.getInt("created_by");
            LocalDateTime createdAt = result.getTimestamp("created_at").toLocalDateTime();
            Integer changedBy = result.getObject("changed_by") == null ?
                    null : result.getInt("changed_by");
            LocalDateTime changedAt = result.getObject("changed_at") == null ?
                    null : result.getTimestamp("changed_at").toLocalDateTime();
            return new Clan(id, clanName, tag, sign, description, ownerId, createdBy, createdAt, changedBy, changedAt);
        };
    }

    public static @NotNull ResultSetProcessor<ClanMember> clanMember() {
        return result -> {
            UUID clanId = UUID.fromString(result.getString("clan_id"));
            int userId = result.getInt("user_id");
            LocalDateTime joinedAt = result.getTimestamp("joined_at").toLocalDateTime();
            UUID clan_group_id = UUID.fromString(result.getString("group_id"));
            return new ClanMember(clanId, userId, joinedAt, clan_group_id);
        };
    }

    public static @NotNull ResultSetProcessor<ClanGroup> clanGroup() {
        return result -> {
            UUID clanId = UUID.fromString(result.getString("clan_id"));
            UUID groupId = UUID.fromString(result.getString("group_id"));
            String groupName = result.getString("group_name");
            int priority = result.getInt("priority");
            boolean isDefault = result.getBoolean("is_default");
            LocalDateTime createdAt = result.getTimestamp("created_at").toLocalDateTime();
            int createdBy = result.getInt("created_by");
            LocalDateTime changedAt = result.getObject("changed_at") != null ? result.getTimestamp("changed_at").toLocalDateTime() : null;
            Integer changedBy = result.getObject("changed_by") != null ? result.getInt("changed_by") : null;
            return new ClanGroup(clanId, groupId, groupName, priority, isDefault, createdBy, createdAt, changedBy, changedAt);
        };
    }

    public static @NotNull ResultSetProcessor<ClanParent> clanParent() {
        return result -> {
            UUID clanId = UUID.fromString(result.getString("clan_id"));
            UUID groupId = UUID.fromString(result.getString("group_id"));
            int parentId = result.getInt("parent_id");
            LocalDateTime expiresAt = result.getObject("expires_at") != null ? result.getTimestamp("expires_at").toLocalDateTime() : null;
            LocalDateTime createdAt = result.getTimestamp("created_at").toLocalDateTime();
            int createdBy = result.getInt("created_by");
            LocalDateTime changedAt = result.getObject("changed_at") != null ? result.getTimestamp("changed_at").toLocalDateTime() : null;
            Integer changedBy = result.getObject("changed_by") != null ? result.getInt("changed_by") : null;
            return new ClanParent(clanId, groupId, parentId, expiresAt, createdBy, createdAt, changedBy, changedAt);
        };
    }

    public static @NotNull ResultSetProcessor<ClanPermission> clanPermission() {
        return result -> {
            UUID clanId = UUID.fromString(result.getString("clan_id"));
            UUID groupId = UUID.fromString(result.getString("group_id"));
            String permission = result.getString("permission");
            LocalDateTime expiresAt = result.getObject("expires_at") != null ? result.getTimestamp("expires_at").toLocalDateTime() : null;
            LocalDateTime createdAt = result.getTimestamp("created_at").toLocalDateTime();
            int createdBy = result.getInt("created_by");
            LocalDateTime changedAt = result.getObject("changed_at") != null ? result.getTimestamp("changed_at").toLocalDateTime() : null;
            Integer changedBy = result.getObject("changed_by") != null ? result.getInt("changed_by") : null;
            return new ClanPermission(clanId, groupId, permission, expiresAt, createdBy, createdAt, changedBy, changedAt);
        };
    }

    public static @NotNull ResultSetProcessor<PrefixColor> prefixColor() {
        return result -> {
            String id = result.getString("id");
            String color = result.getString("color");
            boolean animated = result.getBoolean("animated");
            LocalDateTime createdAt = result.getTimestamp("created_at").toLocalDateTime();
            int createdBy = result.getInt("created_by");
            LocalDateTime changedAt = result.getObject("changed_at") != null ? result.getTimestamp("changed_at").toLocalDateTime() : null;
            Integer changedBy = result.getObject("changed_by") != null ? result.getInt("changed_by") : null;
            return new PrefixColor(id, color, animated, createdAt, createdBy, changedAt, changedBy);
        };
    }

    public static @NotNull ResultSetProcessor<UserPrefixColor> userPrefixColor() {
        return result -> {
            int userId = result.getInt("user_id");
            String colorId = result.getString("color_id");
            boolean active = result.getBoolean("active");
            LocalDateTime createdAt = result.getTimestamp("created_at").toLocalDateTime();
            return new UserPrefixColor(userId, colorId, active, createdAt);
        };
    }

    public static @NotNull ResultSetProcessor<InventoryType> inventoryType() {
        return result -> {
            int id = result.getInt("id");
            String name = result.getString("inventory_name");
            return new InventoryType(id, name);
        };
    }

    public static @NotNull ResultSetProcessor<UserInventory> userInventory() {
        return result -> {
            int userId = result.getInt("user_id");
            int inventoryId = result.getInt("inventory_id");
            String value = result.getString("inventory_value");
            return new UserInventory(userId, inventoryId, value);
        };
    }

    public static @NotNull ResultSetProcessor<UserStats> userStats() {
        return result -> {
            int userId = result.getInt("id");
            int playTime = result.getInt("play_time");
            int dailyStreak = result.getInt("daily_streak");
            LocalDate lastDay = result.getDate("daily_streak_last_day") != null ? result.getDate("daily_streak_last_day").toLocalDate() : null;
            LocalDateTime lastSeen = result.getTimestamp("last_seen_at") != null ? result.getTimestamp("last_seen_at").toLocalDateTime() : null;
            return new UserStats(userId, playTime, dailyStreak, lastDay, lastSeen);
        };
    }

    public static @NotNull ResultSetProcessor<Maintenance> maintenance() {
        return result -> {
            int id = result.getInt("id");
            String title = result.getString("title");
            String reason = result.getString("reason");
            MaintenanceType status = MaintenanceType.valueOf(result.getString("status"));
            LocalDateTime startAt = result.getTimestamp("start_at").toLocalDateTime();
            LocalDateTime endAt = result.getTimestamp("end_at").toLocalDateTime();
            LocalDateTime createdAt = result.getTimestamp("created_at").toLocalDateTime();
            int createdBy = result.getInt("created_by");
            LocalDateTime changedAt = result.getTimestamp("changed_at") != null ? result.getTimestamp("changed_at").toLocalDateTime() : null;
            Integer changedBy = result.getInt("changed_by");
            return new Maintenance(id, title, reason, status, startAt, endAt, createdAt, createdBy, changedAt, changedBy);
        };
    }

    public static @NotNull ResultSetProcessor<MaintenanceWhitelist> maintenanceWhitelist() {
        return result -> {
            int id = result.getInt("id");
            int maintenanceId = result.getInt("maintenance_id");
            int userId = result.getInt("user_id");
            LocalDateTime startAt = result.getTimestamp("start_at") != null ? result.getTimestamp("start_at").toLocalDateTime() : null;
            LocalDateTime endAt = result.getTimestamp("end_at") != null ? result.getTimestamp("end_at").toLocalDateTime() : null;
            String note = result.getString("note");
            LocalDateTime createdAt = result.getTimestamp("created_at").toLocalDateTime();
            int createdBy = result.getInt("created_by");
            LocalDateTime changedAt = result.getTimestamp("changed_at") != null ? result.getTimestamp("changed_at").toLocalDateTime() : null;
            Integer changedBy = result.getInt("changed_by");
            return new MaintenanceWhitelist(id, maintenanceId, userId, startAt, endAt, note, createdAt, createdBy, changedAt, changedBy);
        };
    }

    public static @NotNull ResultSetProcessor<UserExcuse> userExcuse() {
        return result -> {
            int id = result.getInt("id");
            int userId = result.getInt("user_id");
            LocalDateTime startAt = result.getTimestamp("start_at").toLocalDateTime();
            LocalDateTime endAt = result.getTimestamp("end_at").toLocalDateTime();
            String reason = result.getString("reason");
            LocalDateTime createdAt = result.getTimestamp("created_at").toLocalDateTime();
            int createdBy = result.getInt("created_by");
            LocalDateTime changedAt = result.getTimestamp("changed_at") != null ? result.getTimestamp("changed_at").toLocalDateTime() : null;
            Integer changedBy = result.getInt("changed_by");
            return new UserExcuse(id, userId, startAt, endAt, reason, createdAt, createdBy, changedAt, changedBy);
        };
    }
}
