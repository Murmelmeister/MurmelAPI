package de.murmelmeister.murmelapi.clan.member;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Consumer;

public interface ClanMember {
    @NotNull UUID clanId();

    int userId();

    @NotNull LocalDateTime joinedAt();

    @NotNull UUID groupId();

    @NotNull Builder builder();

    @NotNull ClanMember with(@NotNull Consumer<Builder> consumer);

    static @NotNull ClanMember of(@NotNull UUID clanId, int userId, @NotNull LocalDateTime joinedAt, @NotNull UUID groupId) {
        return new ClanMemberImpl(clanId, userId, joinedAt, groupId);
    }

    interface Builder {
        @NotNull Builder groupId(@NotNull UUID groupId);

        @NotNull ClanMember build();
    }
}
