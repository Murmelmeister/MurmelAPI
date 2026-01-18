package de.murmelmeister.murmelapi.clan.permission;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface ClanPermissionProvider {
    void refreshCache();

    @Nullable ClanPermission findPermission(@Nullable UUID clanId, @Nullable UUID groupId, @Nullable String permission);

    @Nullable List<ClanPermission> findPermissions(@Nullable UUID clanId, @Nullable UUID groupId);

    @Nullable ClanPermission add(@NotNull UUID clanId, @NotNull UUID groupId, @NotNull String permission, long duration, int createdBy);

    int remove(@NotNull UUID clanId, @NotNull UUID groupId, @NotNull String permission);

    int clear(@NotNull UUID clanId, @NotNull UUID groupId);

    @Nullable ClanPermission update(@NotNull UUID clanId, @NotNull UUID groupId, @NotNull String permission, long duration, int changedBy);

    int loadExpired();
}
