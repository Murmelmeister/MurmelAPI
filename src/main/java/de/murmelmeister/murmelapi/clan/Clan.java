package de.murmelmeister.murmelapi.clan;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Consumer;

public interface Clan {
    @NotNull UUID id();

    @NotNull String name();

    @Nullable String tag();

    @Nullable String sign();

    @Nullable String description();

    int ownerId();

    int createdBy();

    @NotNull LocalDateTime createdAt();

    @Nullable Integer changedBy();

    @Nullable LocalDateTime changedAt();

    @NotNull Builder builder();

    @NotNull Clan with(@NotNull Consumer<Builder> consumer);

    static @NotNull Clan of(@NotNull UUID id, @NotNull String name, @Nullable String tag, @Nullable String sign, @Nullable String description, int ownerId, int createdBy, @NotNull LocalDateTime createdAt) {
        return new ClanImpl(id, name, tag, sign, description, ownerId, createdBy, createdAt, null, null);
    }

    interface Builder {
        @NotNull Builder name(@NotNull String name);

        @NotNull Builder tag(@Nullable String tag);

        @NotNull Builder sign(@Nullable String sign);

        @NotNull Builder description(@Nullable String description);

        @NotNull Builder ownerId(int ownerId);

        @NotNull Builder changedAt(@Nullable LocalDateTime changedAt);

        @NotNull Builder changedBy(@Nullable Integer changedBy);

        @NotNull Clan build();
    }
}
