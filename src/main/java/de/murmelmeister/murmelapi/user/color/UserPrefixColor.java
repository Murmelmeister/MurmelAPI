package de.murmelmeister.murmelapi.user.color;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

public record UserPrefixColor(int userId, @NotNull String colorId, boolean active, @NotNull LocalDateTime createdAt) {
    public UserPrefixColor withActive(@Nullable Boolean active) {
        return new UserPrefixColor(
                this.userId,
                this.colorId,
                active != null ? active : this.active,
                this.createdAt
        );
    }
}
