package de.murmelmeister.murmelapi.clan.member;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClanMember(@NotNull UUID clanId, int userId, @NotNull LocalDateTime joinedAt, int groupId) {
    public ClanMember withGroup(Integer groupId) {
        return new ClanMember(clanId, userId, joinedAt, groupId != null ? groupId : this.groupId);
    }
}
