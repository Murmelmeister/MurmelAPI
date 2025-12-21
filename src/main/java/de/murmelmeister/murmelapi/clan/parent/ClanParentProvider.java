package de.murmelmeister.murmelapi.clan.parent;

import java.util.List;
import java.util.UUID;

public interface ClanParentProvider {
    void refreshCache();

    ClanParent findParent(UUID clanId, int groupId, int parentId);

    List<ClanParent> findParents(UUID clanId, int groupId);

    ClanParent add(UUID clanId, int groupId, int parentId, long duration, int createdBy);

    int remove(UUID clanId, int groupId, int parentId);

    int clear(UUID clanId, int groupId);

    ClanParent update(UUID clanId, int groupId, int parentId, long duration, int changedBy);

    int loadExpired();
}
