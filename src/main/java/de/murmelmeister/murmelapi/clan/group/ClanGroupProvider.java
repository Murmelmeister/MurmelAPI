package de.murmelmeister.murmelapi.clan.group;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface ClanGroupProvider {
    void refreshCache();

    @Nullable ClanGroup findById(@Nullable UUID clanId, @Nullable UUID groupId);

    @Nullable List<ClanGroup> findByClanId(@Nullable UUID clanId);

    @NotNull List<ClanGroup> findAll();

    @Nullable ClanGroup create(@NotNull UUID clanId, @NotNull String groupName, int priority, boolean defaultGroup, int createdBy);

    int delete(@NotNull UUID clanId, @NotNull UUID groupId);

    @Nullable ClanGroup update(@NotNull UUID clanId, @NotNull UUID groupId, @NotNull String groupName, int priority, boolean defaultGroup, int changedBy);
}
