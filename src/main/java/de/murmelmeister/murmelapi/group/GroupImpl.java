package de.murmelmeister.murmelapi.group;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Consumer;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

record GroupImpl(
        int id,
        @NotNull String groupName,
        int priority,
        boolean isDefault,
        int createdBy,
        @NotNull LocalDateTime createdAt,
        @Nullable Integer changedBy,
        @Nullable LocalDateTime changedAt
) implements Group {

    public GroupImpl {
        Objects.requireNonNull(groupName, "groupName must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (groupName.length() > 100)
            throw new IllegalArgumentException("groupName cannot be longer than 100 characters");
        if (createdBy < CONSOLE_USER_ID) throw new IllegalArgumentException("createdBy must be >= " + CONSOLE_USER_ID);
        if (changedBy != null && changedBy < CONSOLE_USER_ID)
            throw new IllegalArgumentException("changedBy must be null or >= " + CONSOLE_USER_ID);
    }

    public @NotNull Group.Builder builder() {
        return new Builder(this);
    }

    public @NotNull Group with(@NotNull Consumer<Group.Builder> consumer) {
        Group.Builder builder = this.builder();
        consumer.accept(builder);
        return builder.build();
    }

    static final class Builder implements Group.Builder {
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

        public @NotNull Group.Builder groupName(@NotNull String groupName) {
            this.groupName = groupName;
            return this;
        }

        public @NotNull Group.Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public @NotNull Group.Builder changedBy(@Nullable Integer changedBy) {
            this.changedBy = changedBy;
            return this;
        }

        public @NotNull Group.Builder changedAt(@Nullable LocalDateTime changedAt) {
            this.changedAt = changedAt;
            return this;
        }

        public @NotNull Group build() {
            return new GroupImpl(id, groupName, priority, isDefault, createdBy, createdAt, changedBy, changedAt);
        }
    }
}
