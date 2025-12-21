package de.murmelmeister.murmelapi.color;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

public record PrefixColor(@NotNull String id, @NotNull String color, boolean animated,
                          @NotNull LocalDateTime createdAt, int createdBy,
                          @Nullable LocalDateTime changedAt, @Nullable Integer changedBy) {
    public PrefixColor withUpdateMeta(@Nullable String color, @Nullable Boolean animated, @Nullable LocalDateTime changedAt, @Nullable Integer changedBy) {
        return new PrefixColor(
                id,
                color != null ? color : this.color,
                animated != null ? animated : this.animated,
                createdAt,
                createdBy,
                changedAt != null ? changedAt : this.changedAt,
                changedBy != null ? changedBy : this.changedBy
        );
    }
}
