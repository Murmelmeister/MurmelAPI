package de.murmelmeister.murmelapi.language;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;

import static de.murmelmeister.murmelapi.MurmelAPI.ENGLISH_CODE;

final class LanguageTypeRowMapper {
    static LanguageType resultSet(@NotNull ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        String code = resultSet.getString("code");
        return new LanguageTypeImpl(id, code != null && !code.isBlank() ? code : ENGLISH_CODE);
    }
}
