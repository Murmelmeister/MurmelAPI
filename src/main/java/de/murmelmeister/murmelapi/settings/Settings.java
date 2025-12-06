package de.murmelmeister.murmelapi.settings;

import java.time.LocalDateTime;

public record Settings(String tagId, String json, LocalDateTime updatedAt) {
    public Settings withUpdateMeta(String json, LocalDateTime updatedAt) {
        return new Settings(
                tagId,
                json != null ? json : this.json,
                updatedAt != null ? updatedAt : this.updatedAt
        );
    }
}
