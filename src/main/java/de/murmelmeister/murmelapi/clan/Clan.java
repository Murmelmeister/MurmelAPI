package de.murmelmeister.murmelapi.clan;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

public record Clan(@NotNull UUID id, @NotNull String name, @Nullable String tag, @Nullable String sign, @Nullable String description,
                   int ownerId, int createdBy, @Nullable LocalDateTime createdAt, @Nullable Integer changedBy, @Nullable LocalDateTime changedAt) {
    public Clan withUpdateMeta(@Nullable String name, @Nullable String tag, @Nullable String sign, @Nullable String description,
                               @Nullable Integer ownerId, @Nullable Integer changedBy, @Nullable LocalDateTime changedAt) {
        return new Clan(
                id,
                name != null ? name : this.name,
                tag != null ? tag : this.tag,
                sign != null ? sign : this.sign,
                description != null ? description : this.description,
                ownerId != null ? ownerId : this.ownerId,
                createdBy,
                createdAt,
                changedBy != null ? changedBy : this.changedBy,
                changedAt != null ? changedAt : this.changedAt
        );
    }
}
