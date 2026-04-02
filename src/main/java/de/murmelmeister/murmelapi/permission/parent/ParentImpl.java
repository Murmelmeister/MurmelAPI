package de.murmelmeister.murmelapi.permission.parent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Consumer;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

record ParentImpl(
        int id,
        @Nullable Integer userId,
        @Nullable Integer groupId,
        int parentId,
        @Nullable LocalDateTime expiresAt,
        int createdBy,
        @NotNull LocalDateTime createdAt,
        @Nullable Integer changedBy,
        @Nullable LocalDateTime changedAt
) implements Parent {

    public ParentImpl {
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if ((userId != null) == (groupId != null))
            throw new IllegalArgumentException("userId and groupId cannot both be null or both be non-null");
        if (createdBy < CONSOLE_USER_ID) throw new IllegalArgumentException("createdBy must be >= " + CONSOLE_USER_ID);
        if (changedBy != null && changedBy < CONSOLE_USER_ID)
            throw new IllegalArgumentException("changedBy must be null or >= " + CONSOLE_USER_ID);
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isPermanent() {
        return expiresAt == null;
    }

    public boolean isUser() {
        return userId != null;
    }

    public boolean isGroup() {
        return groupId != null;
    }

    public @NotNull Parent.Builder builder() {
        return new Builder(this);
    }

    public @NotNull Parent with(@NotNull Consumer<Parent.Builder> consumer) {
        Parent.Builder builder = this.builder();
        consumer.accept(builder);
        return builder.build();
    }

    static final class Builder implements Parent.Builder {
        private final int id;
        private final Integer userId;
        private final Integer groupId;
        private final int parentId;
        private final int createdBy;
        private final LocalDateTime createdAt;

        private LocalDateTime expiresAt;
        private Integer changedBy;
        private LocalDateTime changedAt;

        public Builder(@NotNull Parent parent) {
            this.id = parent.id();
            this.userId = parent.userId();
            this.groupId = parent.groupId();
            this.parentId = parent.parentId();
            this.createdBy = parent.createdBy();
            this.createdAt = parent.createdAt();
            this.expiresAt = parent.expiresAt();
            this.changedBy = parent.changedBy();
            this.changedAt = parent.changedAt();
        }

        public @NotNull Parent.Builder expiresAt(@Nullable LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public @NotNull Parent.Builder changedBy(@Nullable Integer changedBy) {
            this.changedBy = changedBy;
            return this;
        }

        public @NotNull Parent.Builder changedAt(@Nullable LocalDateTime changedAt) {
            this.changedAt = changedAt;
            return this;
        }

        public @NotNull Parent build() {
            return new ParentImpl(id, userId, groupId, parentId, expiresAt, createdBy, createdAt, changedBy, changedAt);
        }
    }
}