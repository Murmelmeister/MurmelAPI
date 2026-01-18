package de.murmelmeister.murmelapi.user.color;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    }

    public UserPrefixColor withActive(@Nullable Boolean active) {
        return new UserPrefixColor(
                this.userId,
                this.colorId,
                active != null ? active : this.active,
                this.createdAt
        );
    }
}
