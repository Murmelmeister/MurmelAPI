package de.murmelmeister.murmelapi.clan.member;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface ClanMemberProvider {
    void refreshCache();

    @Nullable ClanMember findMember(@NotNull UUID clanId, int userId);

    @Nullable List<ClanMember> findClan(@Nullable UUID clanId);

    @NotNull List<ClanMember> findAll();

    @Nullable ClanMember create(@NotNull UUID clanId, int userId, @NotNull UUID groupId);

    int delete(@NotNull UUID clanId, int userId);

    @Nullable ClanMember update(@NotNull UUID clanId, int userId, @NotNull UUID groupId);
}
