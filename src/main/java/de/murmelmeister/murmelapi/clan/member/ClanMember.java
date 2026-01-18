package de.murmelmeister.murmelapi.clan.member;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record ClanMember(
        @NotNull UUID clanId,
        int userId,
        @NotNull LocalDateTime joinedAt,
        @NotNull UUID groupId
) {
    public ClanMember {
        Objects.requireNonNull(clanId, "clanId must not be null");
        Objects.requireNonNull(joinedAt, "joinedAt must not be null");
        Objects.requireNonNull(groupId, "groupId must not be null");
    }

    public static @NotNull Builder builder(@NotNull ClanMember clanMember) {
        return new Builder(clanMember);
    }

    public static class Builder {
        private final UUID clanId;
        private final int userId;
        private final LocalDateTime joinedAt;

        private UUID groupId;

        private Builder(@NotNull ClanMember clanMember) {
            this.clanId = clanMember.clanId();
            this.userId = clanMember.userId();
            this.joinedAt = clanMember.joinedAt();
            this.groupId = clanMember.groupId();
        }

        public Builder groupId(@NotNull UUID groupId) {
            this.groupId = groupId;
            return this;
        }

        public @NotNull ClanMember build() {
            return new ClanMember(clanId, userId, joinedAt, groupId);
        }
    }
}
