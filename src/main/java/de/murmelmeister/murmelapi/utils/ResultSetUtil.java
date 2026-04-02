package de.murmelmeister.murmelapi.utils;

import de.murmelmeister.library.database.ResultSetProcessor;
import de.murmelmeister.murmelapi.clan.Clan;
import de.murmelmeister.murmelapi.clan.group.ClanGroup;
import de.murmelmeister.murmelapi.clan.member.ClanMember;
import de.murmelmeister.murmelapi.clan.parent.ClanParent;
import de.murmelmeister.murmelapi.clan.permission.ClanPermission;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentAudit;
import de.murmelmeister.murmelapi.punishment.ip.PunishmentIpAddress;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;
import de.murmelmeister.murmelapi.punishment.user.PunishmentUser;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.UUID;

public final class ResultSetUtil {
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
}
