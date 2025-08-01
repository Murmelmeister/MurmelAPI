package de.murmelmeister.murmelapi.group.parent;

import java.time.LocalDateTime;

public record GroupParent(int groupId, int parentId, LocalDateTime expiresAt, int createdBy, LocalDateTime createdAt,
                          Integer changedBy, LocalDateTime changedAt) {

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isPermanent() {
        return expiresAt == null;
    }

    public GroupParent withUpdateMeta(LocalDateTime expiresAt, Integer changedBy, LocalDateTime changedAt) {
        return new GroupParent(groupId, parentId,
                expiresAt != null ? expiresAt : this.expiresAt,
                createdBy, createdAt,
                changedBy != null ? changedBy : this.changedBy,
                changedAt != null ? changedAt : this.changedAt
        );
    }
}
