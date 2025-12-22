package de.murmelmeister.murmelapi.clan.member;

import java.util.List;
import java.util.UUID;

public interface ClanMemberProvider {
    void refreshCache();

    ClanMember findMember(UUID clanId, int userId);

    List<ClanMember> findClan(UUID clanId);

    List<ClanMember> findAll();

    ClanMember create(UUID clanId, int userId, UUID groupId);

    int delete(UUID clanId, int userId);

    ClanMember update(UUID clanId, int userId, UUID groupId);
}
