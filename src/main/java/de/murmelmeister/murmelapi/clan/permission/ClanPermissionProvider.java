package de.murmelmeister.murmelapi.clan.permission;

import java.util.List;
import java.util.UUID;

public interface ClanPermissionProvider {
    void refreshCache();

    ClanPermission findPermission(UUID clanId, UUID groupId, String permission);

    List<ClanPermission> findPermissions(UUID clanId, UUID groupId);

    ClanPermission add(UUID clanId, UUID groupId, String permission, long duration, int createdBy);

    int remove(UUID clanId, UUID groupId, String permission);

    int clear(UUID clanId, UUID groupId);

    ClanPermission update(UUID clanId, UUID groupId, String permission, long duration, int changedBy);

    int loadExpired();
}
