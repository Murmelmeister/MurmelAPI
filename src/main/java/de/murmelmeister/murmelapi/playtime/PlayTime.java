package de.murmelmeister.murmelapi.playtime;

import de.murmelmeister.murmelapi.user.User;

import java.sql.SQLException;

public sealed interface PlayTime permits PlayTimeProvider {
    boolean existsUser(int userId) throws SQLException;

    void createUser(int userId) throws SQLException;

    void deleteUser(int userId) throws SQLException;

    long getTime(int userId, PlayTimeType type) throws SQLException;

    void setTime(int userId, PlayTimeType type, long time) throws SQLException;

    void addTime(int userId, PlayTimeType type) throws SQLException;

    void addTime(int userId, PlayTimeType type, long time) throws SQLException;

    void removeTime(int userId, PlayTimeType type) throws SQLException;

    void removeTime(int userId, PlayTimeType type, long time) throws SQLException;

    void resetTime(int userId, PlayTimeType type) throws SQLException;
}
