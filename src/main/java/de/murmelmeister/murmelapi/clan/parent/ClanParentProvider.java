package de.murmelmeister.murmelapi.clan.parent;

import java.util.List;
import java.util.UUID;

public interface ClanParentProvider {
    void refreshCache();

    ClanParent findParent(UUID clanId, UUID groupId, int parentId);

    List<ClanParent> findParents(UUID clanId, UUID groupId);

    ClanParent add(UUID clanId, UUID groupId, int parentId, long duration, int createdBy);

    int remove(UUID clanId, UUID groupId, int parentId);

    int clear(UUID clanId, UUID groupId);

    ClanParent update(UUID clanId, UUID groupId, int parentId, long duration, int changedBy);

    int loadExpired();
}
