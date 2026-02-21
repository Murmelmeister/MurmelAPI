package de.murmelmeister.murmelapi.settings;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Objects;

public record Settings(
        @NotNull String tagId,
        @NotNull String json,
        @NotNull LocalDateTime updatedAt
) {
    public Settings {
        Objects.requireNonNull(tagId, "tagId must not be null");
        Objects.requireNonNull(json, "json must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (tagId.length() > 100) throw new IllegalArgumentException("tagId cannot be longer than 100 characters");
    }

    public static @NotNull Builder builder(@NotNull Settings settings) {
        return new Builder(settings);
    }

    public static class Builder {
        private final String tagId;
        private String json;
        private LocalDateTime updatedAt;

        private Builder(@NotNull Settings settings) {
            this.tagId = settings.tagId();
            this.json = settings.json();
            this.updatedAt = settings.updatedAt();
        }

        public Builder json(@NotNull String json) {
            this.json = json;
            return this;
        }

        public Builder updatedAt(@NotNull LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public @NotNull Settings build() {
            return new Settings(tagId, json, updatedAt);
        }
    }
}
