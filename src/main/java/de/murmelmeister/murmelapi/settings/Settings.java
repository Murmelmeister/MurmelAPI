package de.murmelmeister.murmelapi.settings;

import java.time.LocalDateTime;

public record Settings(String tagId, String json, LocalDateTime updatedAt) {
}
