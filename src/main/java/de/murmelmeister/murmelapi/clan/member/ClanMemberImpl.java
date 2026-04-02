package de.murmelmeister.murmelapi.clan.member;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

record ClanMemberImpl(
        @NotNull UUID clanId,
        int userId,
        @NotNull LocalDateTime joinedAt,
        @NotNull UUID groupId
) implements ClanMember {
    public ClanMemberImpl {
        Objects.requireNonNull(clanId, "clanId must not be null");
        Objects.requireNonNull(joinedAt, "joinedAt must not be null");
        Objects.requireNonNull(groupId, "groupId must not be null");
        if (userId < 1) throw new IllegalArgumentException("userId must be >= 1");
    }

    public @NotNull ClanMember.Builder builder() {
        return new Builder(this);
    }

    public @NotNull ClanMember with(@NotNull Consumer<ClanMember.Builder> consumer) {
        ClanMember.Builder builder = this.builder();
        consumer.accept(builder);
        return builder.build();
    }

    static final class Builder implements ClanMember.Builder {
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

        public @NotNull ClanMember.Builder groupId(@NotNull UUID groupId) {
            this.groupId = groupId;
            return this;
        }

        public @NotNull ClanMember build() {
            return new ClanMemberImpl(clanId, userId, joinedAt, groupId);
        }
    }
}
