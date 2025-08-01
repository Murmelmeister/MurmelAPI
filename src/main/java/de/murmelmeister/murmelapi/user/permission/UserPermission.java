package de.murmelmeister.murmelapi.user.permission;

import java.time.LocalDateTime;

/**
 * Represents a permission assigned to a user, including expiration details.
 * This record is immutable and provides methods to check if the permission has expired
 * and to create a new instance with updated metadata.
 *
 * @param userId     ID of the user to whom the permission is assigned
 * @param permission The permission assigned to the user
 * @param expiresAt  Nullable expiration date and time of the permission
 * @param createdBy  ID of the user who created this permission
 * @param createdAt  Date and time when the permission was created
 * @param changedBy  Nullable ID of the user who last changed this permission
 * @param changedAt  Nullable date and time when the permission was last changed
 */
public record UserPermission(int userId, String permission, LocalDateTime expiresAt, int createdBy,
                             LocalDateTime createdAt, Integer changedBy, LocalDateTime changedAt) {

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isPermanent() {
        return expiresAt == null;
    }

    public UserPermission withUpdateMeta(LocalDateTime expiresAt, Integer changedBy, LocalDateTime changedAt) {
        return new UserPermission(userId, permission,
                expiresAt != null ? expiresAt : this.expiresAt,
                createdBy, createdAt,
                changedBy != null ? changedBy : this.changedBy,
                changedAt != null ? changedAt : this.changedAt);
    }
}
