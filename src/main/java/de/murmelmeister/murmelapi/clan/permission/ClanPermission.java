package de.murmelmeister.murmelapi.clan.permission;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record ClanPermission(
        @NotNull UUID clanId,
        @NotNull UUID groupId,
        @NotNull String permission,
        @Nullable LocalDateTime expiresAt,
        int createdBy,
        @NotNull LocalDateTime createdAt,
        @Nullable Integer changedBy,
        @Nullable LocalDateTime changedAt
) {
    public ClanPermission {
        Objects.requireNonNull(clanId, "clanId must not be null");
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(permission, "permission must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isPermanent() {
        return expiresAt == null;
    }

    public static @NotNull Builder builder(@NotNull ClanPermission clanPermission) {
        return new Builder(clanPermission);
    }

    public static class Builder {
        private final UUID clanId;
        private final UUID groupId;
        private final String permission;
        private final int createdBy;
        private final LocalDateTime createdAt;

        private LocalDateTime expiresAt;
        private Integer changedBy;
        private LocalDateTime changedAt;

        private Builder(@NotNull ClanPermission clanPermission) {
            this.clanId = clanPermission.clanId();
            this.groupId = clanPermission.groupId();
            this.permission = clanPermission.permission();
            this.createdBy = clanPermission.createdBy();
            this.createdAt = clanPermission.createdAt();
            this.expiresAt = clanPermission.expiresAt();
            this.changedBy = clanPermission.changedBy();
            this.changedAt = clanPermission.changedAt();
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

        public @NotNull ClanPermission build() {
            return new ClanPermission(clanId, groupId, permission, expiresAt, createdBy, createdAt, changedBy, changedAt);
        }
    }
}
