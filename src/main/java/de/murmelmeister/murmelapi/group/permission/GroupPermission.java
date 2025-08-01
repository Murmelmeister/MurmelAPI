package de.murmelmeister.murmelapi.group.permission;

import java.time.LocalDateTime;

public record GroupPermission(int groupId, String permission, LocalDateTime expiresAt, int createdBy,
                              LocalDateTime createdAt, Integer changedBy, LocalDateTime changedAt) {

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isPermanent() {
        return expiresAt == null;
    }

    public GroupPermission withUpdateMeta(LocalDateTime expiresAt, Integer changedBy, LocalDateTime changedAt) {
        return new GroupPermission(groupId, permission,
                expiresAt != null ? expiresAt : this.expiresAt,
                createdBy, createdAt,
                changedBy != null ? changedBy : this.changedBy,
                changedAt != null ? changedAt : this.changedAt);
    }
}
