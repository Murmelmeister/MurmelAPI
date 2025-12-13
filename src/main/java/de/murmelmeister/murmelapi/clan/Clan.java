package de.murmelmeister.murmelapi.clan;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

public record Clan(@NotNull UUID id, @NotNull String name, @Nullable String tag, @Nullable String sign, @Nullable String description,
                   int ownerId, int createdBy, @NotNull LocalDateTime createdAt, @Nullable Integer changedBy, @Nullable LocalDateTime changedAt) {
}
