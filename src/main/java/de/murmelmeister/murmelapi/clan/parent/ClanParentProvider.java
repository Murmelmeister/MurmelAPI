package de.murmelmeister.murmelapi.clan.parent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface ClanParentProvider {
    void refreshCache();

    @Nullable ClanParent findParent(@Nullable UUID clanId, @Nullable UUID groupId, int parentId);

    @Nullable List<ClanParent> findParents(@Nullable UUID clanId, @Nullable UUID groupId);

    @Nullable ClanParent add(@NotNull UUID clanId, @NotNull UUID groupId, int parentId, long duration, int createdBy);

    int remove(@NotNull UUID clanId, @NotNull UUID groupId, int parentId);

    int clear(@NotNull UUID clanId, @NotNull UUID groupId);

    @Nullable ClanParent update(@NotNull UUID clanId, @NotNull UUID groupId, int parentId, long duration, int changedBy);

    int loadExpired();
}
