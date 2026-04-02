package de.murmelmeister.murmelapi.language.message;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;

final class MessageRowMapper {
    static Message resultSet(@NotNull ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        String tag = resultSet.getString("tag_id");
        int languageId = resultSet.getInt("language_id");
        String message = resultSet.getString("message");
        return new MessageImpl(id, tag, languageId, message);
    }
}
