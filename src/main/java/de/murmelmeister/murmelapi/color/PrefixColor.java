package de.murmelmeister.murmelapi.color;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;

public record PrefixColor(
        @NotNull String id,
        @NotNull String color,
        boolean animated,
        @NotNull LocalDateTime createdAt,
        int createdBy,
        @Nullable LocalDateTime changedAt,
        @Nullable Integer changedBy
) {
    public PrefixColor {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(color, "color must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static @NotNull Builder builder(@NotNull PrefixColor prefixColor) {
        return new Builder(prefixColor);
    }

    public static class Builder {
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

        public Builder color(@NotNull String color) {
            this.color = color;
            return this;
        }

        public Builder animated(boolean animated) {
            this.animated = animated;
            return this;
        }

        public Builder changedAt(@Nullable LocalDateTime changedAt) {
            this.changedAt = changedAt;
            return this;
        }

        public Builder changedBy(@Nullable Integer changedBy) {
            this.changedBy = changedBy;
            return this;
        }

        public @NotNull PrefixColor build() {
            return new PrefixColor(id, color, animated, createdAt, createdBy, changedAt, changedBy);
        }
    }
}
