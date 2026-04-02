package de.murmelmeister.murmelapi.clan.group;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Consumer;

public interface ClanGroup {
    @NotNull UUID clanId();

    @NotNull UUID groupId();

    @NotNull String groupName();

    int priority();

    boolean defaultGroup();

    int createdBy();

    @NotNull LocalDateTime createdAt();

    @Nullable Integer changedBy();

    @Nullable LocalDateTime changedAt();

    @NotNull Builder builder();

    @NotNull ClanGroup with(@NotNull Consumer<Builder> consumer);

    static @NotNull ClanGroup of(@NotNull UUID clanId, @NotNull UUID groupId, @NotNull String groupName, int priority, boolean defaultGroup, int createdBy, @NotNull LocalDateTime createdAt) {
        return new ClanGroupImpl(clanId, groupId, groupName, priority, defaultGroup, createdBy, createdAt, null, null);
    }

    interface Builder {
        @NotNull Builder groupName(@NotNull String groupName);

        @NotNull Builder priority(int priority);

        @NotNull Builder defaultGroup(boolean defaultGroup);

        @NotNull Builder changedBy(@Nullable Integer changedBy);

        @NotNull Builder changedAt(@Nullable LocalDateTime changedAt);

        @NotNull ClanGroup build();
    }
}
