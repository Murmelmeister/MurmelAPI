package de.murmelmeister.murmelapi.user.color;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Objects;

public record UserPrefixColor(
        int userId,
        @NotNull String colorId,
        boolean active,
        @NotNull LocalDateTime createdAt
) {
    public UserPrefixColor {
        Objects.requireNonNull(colorId, "colorId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (userId < 1) throw new IllegalArgumentException("userId must be >= 1");
        if (colorId.length() > 100) throw new IllegalArgumentException("colorId cannot be longer than 100 characters");
        if (colorId.isBlank()) throw new IllegalArgumentException("colorId cannot be blank");
    }

    public static @NotNull Builder builder(@NotNull UserPrefixColor userPrefixColor) {
        return new Builder(userPrefixColor);
    }

    public static class Builder {
        private final int userId;
        private final String colorId;
        private final LocalDateTime createdAt;

        private boolean active;

        private Builder(@NotNull UserPrefixColor userPrefixColor) {
            this.userId = userPrefixColor.userId();
            this.colorId = userPrefixColor.colorId();
            this.active = userPrefixColor.active();
            this.createdAt = userPrefixColor.createdAt();
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public @NotNull UserPrefixColor build() {
            return new UserPrefixColor(userId, colorId, active, createdAt);
        }
    }
}
