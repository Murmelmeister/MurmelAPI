package de.murmelmeister.murmelapi.utils;

import de.murmelmeister.library.database.ResultSetProcessor;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.color.GroupColor;
import de.murmelmeister.murmelapi.group.parent.GroupParent;
import de.murmelmeister.murmelapi.group.permission.GroupPermission;
import de.murmelmeister.murmelapi.language.Language;
import de.murmelmeister.murmelapi.language.message.Message;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentLog;
import de.murmelmeister.murmelapi.punishment.ip.PunishmentCurrentIp;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;
import de.murmelmeister.murmelapi.punishment.user.PunishmentCurrentUser;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.login.UserLogin;
import de.murmelmeister.murmelapi.user.parent.UserParent;
import de.murmelmeister.murmelapi.user.permission.UserPermission;
import de.murmelmeister.murmelapi.user.playtime.UserPlayTime;
import de.murmelmeister.murmelapi.user.session.UserSession;

import java.time.LocalDateTime;
import java.util.UUID;

public final class ResultSetUtil {
    public static ResultSetProcessor<Language> language() {
        return resultSet -> {
            int id = resultSet.getInt("id");
            String name = resultSet.getString("name");
            return new Language(id, name);
        };
    }

    public static ResultSetProcessor<Message> message() {
        return resultSet -> {
            int id = resultSet.getInt("id");
            String tag = resultSet.getString("tag_id");
            int languageId = resultSet.getInt("language_id");
            String message = resultSet.getString("message");
            return new Message(id, tag, languageId, message);
        };
    }

    public static ResultSetProcessor<User> user() {
        return resultSet -> {
            int id = resultSet.getInt("id");
            UUID mojangId = resultSet.getString("mojang_id") != null ? UUID.fromString(resultSet.getString("mojang_id")) : null;
            String username = resultSet.getString("username");
            LocalDateTime firstJoin = resultSet.getObject("first_login") != null ? resultSet.getTimestamp("first_login").toLocalDateTime() : null;
            boolean isSystemUser = resultSet.getBoolean("system_user");
            boolean isDebugUser = resultSet.getBoolean("debug_user");
            boolean isDebugActive = resultSet.getBoolean("debug_enabled");
            int languageId = resultSet.getInt("language_id");
            return new User(id, mojangId, username, firstJoin, isSystemUser, isDebugUser, isDebugActive, languageId);
        };
    }

    public static ResultSetProcessor<UserPlayTime> userPlayTime() {
        return resultSet -> {
            int userId = resultSet.getInt("id");
            int playTime = resultSet.getInt("play_time");
            int loginStreak = resultSet.getInt("login_count");
            return new UserPlayTime(userId, playTime, loginStreak);
        };
    }

    public static ResultSetProcessor<UserSession> userSession() {
        return result -> {
            UUID id = UUID.fromString(result.getString("id"));
            int userId = result.getInt("user_id");
            LocalDateTime loginTime = result.getTimestamp("login_time").toLocalDateTime();
            String ipAddress = result.getString("ip_address");
            String clientVersion = result.getString("client_version");
            String protocolVersion = result.getString("protocol_version");
            return new UserSession(id, userId, loginTime, ipAddress, clientVersion, protocolVersion);
        };
    }

    public static ResultSetProcessor<UserLogin> userLogin() {
        return resultSet -> {
            UUID id = UUID.fromString(resultSet.getString("id"));
            int userId = resultSet.getInt("user_id");
            LocalDateTime loginTime = resultSet.getTimestamp("login_time").toLocalDateTime();
            LocalDateTime logoutTime = resultSet.getTimestamp("logout_time").toLocalDateTime();
            String ipAddress = resultSet.getString("ip_address");
            String clientVersion = resultSet.getString("client_version");
            String protocolVersion = resultSet.getString("protocol_version");

            return new UserLogin(id, userId, loginTime, logoutTime, ipAddress, clientVersion, protocolVersion);
        };
    }

    public static ResultSetProcessor<UserPermission> userPermission() {
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

    public static ResultSetProcessor<UserParent> userParent() {
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

    public static ResultSetProcessor<Group> group() {
        return resultSet -> {
            int id = resultSet.getInt("id");
            String groupName = resultSet.getString("group_name");
            String teamTagId = resultSet.getString("team_tag_id");
            int priority = resultSet.getInt("priority");
            boolean isDefault = resultSet.getBoolean("is_default");
            int createdBy = resultSet.getInt("created_by");
            LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
            Integer changedBy = resultSet.getObject("changed_by") != null ? resultSet.getInt("changed_by") : null;
            LocalDateTime changedAt = resultSet.getObject("changed_at") != null ? resultSet.getTimestamp("changed_at").toLocalDateTime() : null;
            return new Group(id, groupName, teamTagId, priority, isDefault, createdBy, createdAt, changedBy, changedAt);
        };
    }

    public static ResultSetProcessor<GroupColor> groupColor() {
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

    public static ResultSetProcessor<GroupPermission> groupPermission() {
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

    public static ResultSetProcessor<GroupParent> groupParent() {
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

    public static ResultSetProcessor<PunishmentLog> punishmentLog() {
        return resultSet -> {
            UUID id = UUID.fromString(resultSet.getString("id"));
            PunishmentLog.Action action = PunishmentLog.Action.valueOf(resultSet.getString("action"));
            Integer userId = resultSet.getObject("user_id") == null ? null : resultSet.getInt("user_id");
            String ipAddress = resultSet.getString("ip_address");
            Integer reasonId = resultSet.getObject("reason_id") == null ? null : resultSet.getInt("reason_id");
            int reasonTypeId = resultSet.getInt("reason_type_id");
            String reasonText = resultSet.getString("reason_text");
            Long reasonDuration = resultSet.getObject("reason_duration") == null ? null : resultSet.getLong("reason_duration");
            boolean reasonAutoFlagIp = resultSet.getBoolean("reason_auto_flag_ip");
            boolean reasonAutoPunish = resultSet.getBoolean("reason_auto_punish");
            int createdBy = resultSet.getInt("created_by");
            LocalDateTime createdAt = resultSet.getTimestamp("created_at").toLocalDateTime();
            return new PunishmentLog(id, action, userId, ipAddress, reasonId, reasonTypeId, reasonText,
                    reasonDuration, reasonAutoFlagIp, reasonAutoPunish, createdBy, createdAt);
        };
    }

    public static ResultSetProcessor<PunishmentReason> punishmentReason() {
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

    public static ResultSetProcessor<PunishmentCurrentIp> punishmentCurrentIp() {
        return result -> {
            String ipAddress = result.getString("ip_address");
            int typeId = result.getInt("type_id");
            UUID logId = UUID.fromString(result.getString("log_id"));
            return new PunishmentCurrentIp(ipAddress, typeId, logId);
        };
    }

    public static ResultSetProcessor<PunishmentCurrentUser> punishmentCurrentUser() {
        return result -> {
            int userId = result.getInt("user_id");
            int typeId = result.getInt("type_id");
            UUID logId = UUID.fromString(result.getString("log_id"));
            return new PunishmentCurrentUser(userId, typeId, logId);
        };
    }
}
