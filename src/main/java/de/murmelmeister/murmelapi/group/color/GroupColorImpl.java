package de.murmelmeister.murmelapi.group.color;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Consumer;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

record GroupColorImpl(
        int groupId,
        int typeId,
        @NotNull String value,
        int createdBy,
        @NotNull LocalDateTime createdAt,
        @Nullable Integer changedBy,
        @Nullable LocalDateTime changedAt
) implements GroupColor {

    public GroupColorImpl {
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (createdBy < CONSOLE_USER_ID) throw new IllegalArgumentException("createdBy must be >= " + CONSOLE_USER_ID);
        if (changedBy != null && changedBy < CONSOLE_USER_ID)
            throw new IllegalArgumentException("changedBy must be null or >= " + CONSOLE_USER_ID);
    }

    public @NotNull GroupColor.Builder builder() {
        return new Builder(this);
    }

    public @NotNull GroupColor with(@NotNull Consumer<GroupColor.Builder> consumer) {
        GroupColor.Builder builder = this.builder();
        consumer.accept(builder);
        return builder.build();
    }

    static final class Builder implements GroupColor.Builder {
        private final int groupId;
        private final int typeId;
        private final int createdBy;
        private final LocalDateTime createdAt;

        private String value;
        private Integer changedBy;
        private LocalDateTime changedAt;

        public Builder(@NotNull GroupColor groupColor) {
            this.groupId = groupColor.groupId();
            this.typeId = groupColor.typeId();
            this.createdBy = groupColor.createdBy();
            this.createdAt = groupColor.createdAt();
            this.value = groupColor.value();
            this.changedBy = groupColor.changedBy();
            this.changedAt = groupColor.changedAt();
        }

        public @NotNull GroupColor.Builder value(@NotNull String value) {
            this.value = value;
            return this;
        }

        public @NotNull GroupColor.Builder changedBy(@Nullable Integer changedBy) {
            this.changedBy = changedBy;
            return this;
        }

        public @NotNull GroupColor.Builder changedAt(@Nullable LocalDateTime changedAt) {
            this.changedAt = changedAt;
            return this;
        }

        public @NotNull GroupColor build() {
            return new GroupColorImpl(groupId, typeId, value, createdBy, createdAt, changedBy, changedAt);
        }
    }
}
