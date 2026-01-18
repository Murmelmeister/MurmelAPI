package de.murmelmeister.murmelapi.user.parent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a parent assigned to a user, including expiration details.
 * This record is immutable and provides methods to check if the parent has expired
 * and to create a new instance with updated metadata.
 */
public record UserParent(
        int userId,
        int parentId,
        @Nullable LocalDateTime expiresAt,
        int createdBy,
        @NotNull LocalDateTime createdAt,
        @Nullable Integer changedBy,
        @Nullable LocalDateTime changedAt
) {
    public UserParent {
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isPermanent() {
        return expiresAt == null;
    }

    public static @NotNull Builder builder(@NotNull UserParent userParent) {
        return new Builder(userParent);
    }

    public static class Builder {
        private final int userId;
        private final int parentId;
        private final int createdBy;
        private final LocalDateTime createdAt;

        private LocalDateTime expiresAt;
        private Integer changedBy;
        private LocalDateTime changedAt;

        private Builder(@NotNull UserParent userParent) {
            this.userId = userParent.userId();
            this.parentId = userParent.parentId();
            this.expiresAt = userParent.expiresAt();
            this.createdBy = userParent.createdBy();
            this.createdAt = userParent.createdAt();
            this.changedBy = userParent.changedBy();
            this.changedAt = userParent.changedAt();
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

        public @NotNull UserParent build() {
            return new UserParent(userId, parentId, expiresAt, createdBy, createdAt, changedBy, changedAt);
        }
    }
}
