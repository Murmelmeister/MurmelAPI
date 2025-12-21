package de.murmelmeister.murmelapi.clan.permission;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClanPermission(@NotNull UUID clanId, int groupId, @NotNull String permission,
                             @Nullable LocalDateTime expiresAt,
                             @NotNull LocalDateTime createdAt, int createdBy,
                             @Nullable LocalDateTime changedAt, @Nullable Integer changedBy) {

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isPermanent() {
        return expiresAt == null;
    }

    public ClanPermission withUpdateMeta(@Nullable LocalDateTime expiresAt, @Nullable LocalDateTime changedAt, @Nullable Integer changedBy) {
        return new ClanPermission(clanId, groupId, permission,
                expiresAt != null ? expiresAt : this.expiresAt,
                createdAt, createdBy,
                changedAt != null ? changedAt : this.changedAt,
                changedBy != null ? changedBy : this.changedBy);
    }
}
