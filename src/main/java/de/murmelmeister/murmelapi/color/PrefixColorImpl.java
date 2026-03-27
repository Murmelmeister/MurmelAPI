package de.murmelmeister.murmelapi.color;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Consumer;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

record PrefixColorImpl(
        @NotNull String id,
        @NotNull String color,
        boolean animated,
        @NotNull LocalDateTime createdAt,
        int createdBy,
        @Nullable LocalDateTime changedAt,
        @Nullable Integer changedBy
) implements PrefixColor {

    public PrefixColorImpl {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(color, "color must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (id.length() > 100) throw new IllegalArgumentException("id cannot be longer than 100 characters");
        if (createdBy < CONSOLE_USER_ID) throw new IllegalArgumentException("createdBy must be >= " + CONSOLE_USER_ID);
        if (changedBy != null && changedBy < CONSOLE_USER_ID)
            throw new IllegalArgumentException("changedBy must be null or >= " + CONSOLE_USER_ID);
    }

    public @NotNull PrefixColor.Builder builder() {
        return new Builder(this);
    }

    public @NotNull PrefixColor with(@NotNull Consumer<PrefixColor.Builder> consumer) {
        PrefixColor.Builder builder = this.builder();
        consumer.accept(builder);
        return builder.build();
    }

    static final class Builder implements PrefixColor.Builder {
        private final String id;
        private final LocalDateTime createdAt;
        private final int createdBy;

        private String color;
        private boolean animated;
        private LocalDateTime changedAt;
        private Integer changedBy;

        private Builder(@NotNull PrefixColor prefixColor) {
            this.id = prefixColor.id();
            this.createdAt = prefixColor.createdAt();
            this.createdBy = prefixColor.createdBy();
            this.color = prefixColor.color();
            this.animated = prefixColor.animated();
            this.changedAt = prefixColor.changedAt();
            this.changedBy = prefixColor.changedBy();
        }

        public @NotNull PrefixColor.Builder color(@NotNull String color) {
            this.color = color;
            return this;
        }

        public @NotNull PrefixColor.Builder animated(boolean animated) {
            this.animated = animated;
            return this;
        }

        public @NotNull PrefixColor.Builder changedAt(@Nullable LocalDateTime changedAt) {
            this.changedAt = changedAt;
            return this;
        }

        public @NotNull PrefixColor.Builder changedBy(@Nullable Integer changedBy) {
            this.changedBy = changedBy;
            return this;
        }

        public @NotNull PrefixColor build() {
            return new PrefixColorImpl(id, color, animated, createdAt, createdBy, changedAt, changedBy);
        }
    }
}
