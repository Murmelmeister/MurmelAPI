package de.murmelmeister.murmelapi.clan.group;

import java.util.List;
import java.util.UUID;

public interface ClanGroupProvider {
    void refreshCache();

    ClanGroup findById(UUID clanId, int groupId);

    List<ClanGroup> findByClanId(UUID clanId);

    List<ClanGroup> findAll();

    ClanGroup create(UUID clanId, String groupName, int priority, boolean defaultGroup, int createdBy);

    int delete(UUID clanId, int groupId);

    ClanGroup update(UUID clanId, int groupId, String groupName, int priority, boolean defaultGroup, int changedBy);
}
