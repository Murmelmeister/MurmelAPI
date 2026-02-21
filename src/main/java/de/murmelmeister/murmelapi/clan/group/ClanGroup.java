package de.murmelmeister.murmelapi.clan.group;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public record ClanGroup(
        @NotNull UUID clanId,
        @NotNull UUID groupId,
        @NotNull String groupName,
        int priority,
        boolean defaultGroup,
        int createdBy,
        @NotNull LocalDateTime createdAt,
        @Nullable Integer changedBy,
        @Nullable LocalDateTime changedAt
) {
    public ClanGroup {
        Objects.requireNonNull(clanId, "clanId must not be null");
        Objects.requireNonNull(groupId, "groupId must not be null");
        Objects.requireNonNull(groupName, "groupName must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (groupName.length() > 100) throw new IllegalArgumentException("groupName cannot be longer than 100 characters");
        if (createdBy < CONSOLE_USER_ID) throw new IllegalArgumentException("createdBy must be >= " + CONSOLE_USER_ID);
        if (changedBy != null && changedBy < CONSOLE_USER_ID)
            throw new IllegalArgumentException("changedBy must be null or >= " + CONSOLE_USER_ID);
    }

    public static @NotNull Builder builder(@NotNull ClanGroup clanGroup) {
        return new Builder(clanGroup);
    }

    public static class Builder {
        private final UUID clanId;
        private final UUID groupId;
        private final int createdBy;
        private final LocalDateTime createdAt;

        private String groupName;
        private int priority;
        private boolean defaultGroup;
        private Integer changedBy;
        private LocalDateTime changedAt;

        private Builder(@NotNull ClanGroup clanGroup) {
            this.clanId = clanGroup.clanId();
            this.groupId = clanGroup.groupId();
            this.createdBy = clanGroup.createdBy();
            this.createdAt = clanGroup.createdAt();
            this.groupName = clanGroup.groupName();
            this.priority = clanGroup.priority();
            this.defaultGroup = clanGroup.defaultGroup();
            this.changedBy = clanGroup.changedBy();
            this.changedAt = clanGroup.changedAt();
        }

        public Builder groupName(@NotNull String groupName) {
            this.groupName = groupName;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder defaultGroup(boolean defaultGroup) {
            this.defaultGroup = defaultGroup;
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

        public @NotNull ClanGroup build() {
            return new ClanGroup(clanId, groupId, groupName, priority, defaultGroup, createdBy, createdAt, changedBy, changedAt);
        }
    }
}
