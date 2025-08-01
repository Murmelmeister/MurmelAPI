package de.murmelmeister.murmelapi.user.parent;

import java.time.LocalDateTime;

/**
 * Represents a parent assigned to a user, including expiration details.
 * This record is immutable and provides methods to check if the parent has expired
 * and to create a new instance with updated metadata.
 *
 * @param expiresAt Nullable
 * @param changedBy Nullable
 * @param changedAt Nullable
 */
public record UserParent(int userId, int parentId, LocalDateTime expiresAt, int createdBy, LocalDateTime createdAt,
                         Integer changedBy, LocalDateTime changedAt) {

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isPermanent() {
        return expiresAt == null;
    }

    public UserParent withUpdateMeta(LocalDateTime expiresAt, Integer changedBy, LocalDateTime changedAt) {
        return new UserParent(userId, parentId,
                expiresAt != null ? expiresAt : this.expiresAt,
                createdBy, createdAt,
                changedBy != null ? changedBy : this.changedBy,
                changedAt != null ? changedAt : this.changedAt);

    }
}
