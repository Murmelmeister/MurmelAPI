package de.murmelmeister.murmelapi.settings;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

final class SettingsRowMapper {
    static Settings resultSet(@NotNull ResultSet resultSet) throws SQLException {
        String tagId = resultSet.getString("tag_id");
        String json = resultSet.getString("value_json");
        LocalDateTime updatedAt = resultSet.getTimestamp("updated_at").toLocalDateTime();
        return new SettingsImpl(tagId, json, updatedAt);
    }
}
