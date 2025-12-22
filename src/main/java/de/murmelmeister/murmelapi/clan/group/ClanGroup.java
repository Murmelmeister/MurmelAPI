package de.murmelmeister.murmelapi.clan.group;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClanGroup(@NotNull UUID clanId, UUID groupId, @NotNull String groupName,
                        int priority, boolean defaultGroup,
                        @NotNull LocalDateTime createdAt, int createdBy,
                        @Nullable LocalDateTime changedAt, @Nullable Integer changedBy) {
    public ClanGroup withUpdateMeta(@Nullable String groupName, @Nullable Integer priority, @Nullable Boolean defaultGroup,
                                    @Nullable Integer changedBy, @Nullable LocalDateTime changedAt) {
        return new ClanGroup(clanId, groupId,
                groupName != null ? groupName : this.groupName,
                priority != null ? priority : this.priority,
                defaultGroup != null ? defaultGroup : this.defaultGroup,
                createdAt, createdBy,
                changedAt != null ? changedAt : this.changedAt,
                changedBy != null ? changedBy : this.changedBy
        );
                                    }
}
