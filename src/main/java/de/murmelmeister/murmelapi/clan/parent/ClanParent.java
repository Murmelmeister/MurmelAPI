package de.murmelmeister.murmelapi.clan.parent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record ClanParent(
        @NotNull UUID clanId,
        @NotNull UUID groupId,
        int parentId,
        @Nullable LocalDateTime expiresAt,
        int createdBy,
        @NotNull LocalDateTime createdAt,
        @Nullable Integer changedBy,
        @Nullable LocalDateTime changedAt
) {
    public ClanParent {
        Objects.requireNonNull(clanId, "clanId must not be null");
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isPermanent() {
        return expiresAt == null;
    }

    public static @NotNull Builder builder(@NotNull ClanParent clanParent) {
        return new Builder(clanParent);
    }

    public static class Builder {
        private final UUID clanId;
        private final UUID groupId;
        private final int parentId;
        private final int createdBy;
        private final LocalDateTime createdAt;

        private LocalDateTime expiresAt;
        private Integer changedBy;
        private LocalDateTime changedAt;

        private Builder(@NotNull ClanParent clanParent) {
            this.clanId = clanParent.clanId();
            this.groupId = clanParent.groupId();
            this.parentId = clanParent.parentId();
            this.createdBy = clanParent.createdBy();
            this.createdAt = clanParent.createdAt();
            this.expiresAt = clanParent.expiresAt();
            this.changedBy = clanParent.changedBy();
            this.changedAt = clanParent.changedAt();
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

        public @NotNull ClanParent build() {
            return new ClanParent(clanId, groupId, parentId, expiresAt, createdBy, createdAt, changedBy, changedAt);
        }
    }
}
