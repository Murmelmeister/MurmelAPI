package de.murmelmeister.murmelapi.clan.permission;

import java.util.List;
import java.util.UUID;

public interface ClanPermissionProvider {
    void refreshCache();

    ClanPermission findPermission(UUID clanId, int groupId, String permission);

    List<ClanPermission> findPermissions(UUID clanId, int groupId);

    ClanPermission add(UUID clanId, int groupId, String permission, long duration, int createdBy);

    int remove(UUID clanId, int groupId, String permission);

    int clear(UUID clanId, int groupId);

    ClanPermission update(UUID clanId, int groupId, String permission, long duration, int changedBy);

    int loadExpired();
}
