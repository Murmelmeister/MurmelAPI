package de.murmelmeister.murmelapi.clan;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public record Clan(
        @NotNull UUID id,
        @NotNull String name,
        @Nullable String tag,
        @Nullable String sign,
        @Nullable String description,
        int ownerId,
        int createdBy,
        @NotNull LocalDateTime createdAt,
        @Nullable Integer changedBy,
        @Nullable LocalDateTime changedAt
) {
    public Clan {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (name.length() > 100) throw new IllegalArgumentException("name cannot be longer than 100 characters");
        if (tag != null && tag.length() > 25)
            throw new IllegalArgumentException("tag cannot be longer than 25 characters");
        if (sign != null && sign.length() > 25)
            throw new IllegalArgumentException("sign cannot be longer than 25 characters");
        if (ownerId < CONSOLE_USER_ID) throw new IllegalArgumentException("ownerId must be >= " + CONSOLE_USER_ID);
        if (createdBy < CONSOLE_USER_ID) throw new IllegalArgumentException("createdBy must be >= " + CONSOLE_USER_ID);
        if (changedBy != null && changedBy < CONSOLE_USER_ID)
            throw new IllegalArgumentException("changedBy must be null or >= " + CONSOLE_USER_ID);
    }

    public static @NotNull Builder builder(@NotNull Clan clan) {
        return new Builder(clan);
    }

    public static class Builder {
        private final UUID id;
        private final int createdBy;
        private final LocalDateTime createdAt;

        private String name;
        private String tag;
        private String sign;
        private String description;
        private int ownerId;
        private LocalDateTime changedAt;
        private Integer changedBy;

        private Builder(@NotNull Clan clan) {
            this.id = clan.id();
            this.name = clan.name();
            this.tag = clan.tag();
            this.sign = clan.sign();
            this.description = clan.description();
            this.ownerId = clan.ownerId();
            this.createdBy = clan.createdBy();
            this.createdAt = clan.createdAt();
            this.changedAt = clan.changedAt();
            this.changedBy = clan.changedBy();
        }

        public Builder name(@NotNull String name) {
            this.name = name;
            return this;
        }

        public Builder tag(@Nullable String tag) {
            this.tag = tag;
            return this;
        }

        public Builder sign(@Nullable String sign) {
            this.sign = sign;
            return this;
        }

        public Builder description(@Nullable String description) {
            this.description = description;
            return this;
        }

        public Builder ownerId(int ownerId) {
            this.ownerId = ownerId;
            return this;
        }

        public Builder changedAt(@Nullable LocalDateTime changedAt) {
            this.changedAt = changedAt;
            return this;
        }

        public Builder changedBy(@Nullable Integer changedBy) {
            this.changedBy = changedBy;
            return this;
        }

        public @NotNull Clan build() {
            return new Clan(id, name, tag, sign, description, ownerId, createdBy, createdAt, changedBy, changedAt);
        }
    }
}
