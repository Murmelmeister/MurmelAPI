package de.murmelmeister.murmelapi.clan;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.UUID;

public interface ClanProvider {
    void refreshCache();

    @Nullable Clan findById(@Nullable UUID id);

    @Nullable Clan findByName(@Nullable String name);

    @Nullable Clan findByOwner(int ownerId);

    @NotNull @Unmodifiable List<Clan> findAll();

    @Nullable Clan create(@NotNull String name, @NotNull String tag, @NotNull String sign, @NotNull String description, int ownerId, int createdBy);

    int delete(@NotNull UUID id);

    @Nullable Clan update(@NotNull UUID id, @NotNull String name, @NotNull String tag, @NotNull String sign, @NotNull String description, int ownerId, int changedBy);

    @Nullable Clan upsert(@NotNull UUID id, @NotNull String name, @NotNull String tag, @NotNull String sign, @NotNull String description, int ownerId, int executorId);
}
