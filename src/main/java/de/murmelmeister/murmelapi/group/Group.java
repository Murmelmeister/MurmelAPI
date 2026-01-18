package de.murmelmeister.murmelapi.group;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;

public record Group(
        int id,
        @NotNull String groupName,
        int priority,
        boolean isDefault,
        int createdBy,
        @NotNull LocalDateTime createdAt,
        @Nullable Integer changedBy,
        @Nullable LocalDateTime changedAt
) {
    public Group {
        Objects.requireNonNull(groupName, "groupName must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static @NotNull Builder builder(@NotNull Group group) {
        return new Builder(group);
    }

    public static class Builder {
        private final int id;
        private final boolean isDefault;
        private final int createdBy;
        private final LocalDateTime createdAt;

        private String groupName;
        private int priority;
        private Integer changedBy;
        private LocalDateTime changedAt;

        private Builder(@NotNull Group group) {
            this.id = group.id();
            this.groupName = group.groupName();
            this.priority = group.priority();
            this.isDefault = group.isDefault();
            this.createdBy = group.createdBy();
            this.createdAt = group.createdAt();
            this.changedBy = group.changedBy();
            this.changedAt = group.changedAt();
        }

        public Builder groupName(@NotNull String groupName) {
            this.groupName = groupName;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
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

        public @NotNull Group build() {
            return new Group(id, groupName, priority, isDefault, createdBy, createdAt, changedBy, changedAt);
        }
    }
}
