package de.murmelmeister.murmelapi.user.permission;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;

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
public record UserPermission(
        int userId,
        @NotNull String permission,
        @Nullable LocalDateTime expiresAt,
        int createdBy,
        @NotNull LocalDateTime createdAt,
        @Nullable Integer changedBy,
        @Nullable LocalDateTime changedAt
) {
    public UserPermission {
        Objects.requireNonNull(permission, "permission must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isPermanent() {
        return expiresAt == null;
    }

    public static @NotNull Builder builder(@NotNull UserPermission userPermission) {
        return new Builder(userPermission);
    }

    public static class Builder {
        private final int userId;
        private final String permission;
        private final int createdBy;
        private final LocalDateTime createdAt;

        private LocalDateTime expiresAt;
        private Integer changedBy;
        private LocalDateTime changedAt;

        private Builder(@NotNull UserPermission userPermission) {
            this.userId = userPermission.userId();
            this.permission = userPermission.permission();
            this.expiresAt = userPermission.expiresAt();
            this.createdBy = userPermission.createdBy();
            this.createdAt = userPermission.createdAt();
            this.changedBy = userPermission.changedBy();
            this.changedAt = userPermission.changedAt();
        }

        public Builder expiresAt(@Nullable LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder changedBy(@Nullable Integer changedBy) {
            this.changedBy = changedBy;
            return this;
        }

        public Builder changedAt(@Nullable LocalDateTime changedAt) {
            this.changedAt = changedAt;
            return this;
        }

        public @NotNull UserPermission build() {
            return new UserPermission(userId, permission, expiresAt, createdBy, createdAt, changedBy, changedAt);
        }
    }
}
