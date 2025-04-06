package de.murmelmeister.murmelapi.logging;

import de.murmelmeister.murmelapi.database.Database;

import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static de.murmelmeister.murmelapi.MurmelAPI.getDateFormat;

public final class LoginHistoryProvider implements LoginHistory {
    private static final String TABLE_NAME = "LoginHistory";
    private final Database database;

    public LoginHistoryProvider(Database database) {
        this.database = database;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "LoginID UUID PRIMARY KEY, " +
                                         "UserID INT, FOREIGN KEY (UserID) REFERENCES Users(ID), " +
                                         "IPAddress INET6, " +
                                         "LoginTime DATETIME, " +
                                         "LogoutTime DATETIME, " +
                                         "ClientVersion VARCHAR(100), " +
                                         "ProtocolVersion VARCHAR(50)");
        Procedure.loadAll(database);
    }

    @Override
    public boolean existsLogin(UUID loginId) {
        return database.existsCallable(Procedure.GET_HISTORY_BY_ID.getName(), loginId.toString());
    }

    @Override
    public void deleteUserLogin(int userId) {
        database.updateCallable(Procedure.DELETE.getName(), userId);
    }

    @Override
    public List<UUID> getLogins(int userId) {
        return database.queryList(Procedure.GET_HISTORY_BY_USER.getName(), new LinkedList<>(), resultSet -> resultSet.getString("LoginID"), userId)
                .stream().map(UUID::fromString).toList();
    }

    @Override
    public List<UUID> getSortedLogins(int userId) {
        return database.queryList(Procedure.GET_HISTORY_BY_USER_SORT.getName(), new LinkedList<>(), resultSet -> resultSet.getString("LoginID"), userId, 10)
                .stream().map(UUID::fromString).toList();
    }

    @Override
    public UUID getLastLoginId(int userId) {
        return database.queryCallable(Procedure.GET_LAST_LOGIN_ID.getName(), null, resultSet -> {
            String id = resultSet.getString("LoginID");
            return id == null ? null : UUID.fromString(id);
        }, userId);
    }

    @Override
    public Timestamp getLastQuit(int userId) {
        return database.queryCallable(Procedure.GET_LAST_QUIT.getName(), null, resultSet -> resultSet.getTimestamp("LastQuit"), userId);
    }

    @Override
    public List<Integer> getUserIdsByIP(String ipAddress) {
        return database.queryList(Procedure.GET_ID_BY_IP.getName(), new LinkedList<>(), resultSet -> resultSet.getInt("UserID"), ipAddress);
    }

    @Override
    public int getUserId(UUID loginId) {
        return database.queryCallable(Procedure.GET_HISTORY_BY_ID.getName(), -2, resultSet -> resultSet.getInt("UserID"), loginId.toString());
    }

    @Override
    public String getIPAddress(UUID loginId) {
        return database.queryCallable(Procedure.GET_HISTORY_BY_ID.getName(), null, resultSet -> resultSet.getString("IPAddress"), loginId.toString());
    }

    @Override
    public Timestamp getFirstLoginTimeByUser(int userId, String ipAddress) {
        return database.queryCallable(Procedure.LOGIN_HISTORY_GET_MIN_TIME_BY_USER.getName(), null, resultSet -> resultSet.getTimestamp("LoginTime"), userId, ipAddress);
    }

    @Override
    public String getFirstLoginDateByUser(int userId, String ipAddress) {
        Timestamp time = getFirstLoginTimeByUser(userId, ipAddress);
        return time == null ? "never" : getDateFormat().format(time);
    }

    @Override
    public Timestamp getFirstLoginTimeByIP(String ipAddress) {
        return database.queryCallable(Procedure.LOGIN_HISTORY_GET_MIN_TIME_BY_IP.getName(), null, resultSet -> resultSet.getTimestamp("LoginTime"), ipAddress);
    }

    @Override
    public String getFirstLoginDateByIP(String ipAddress) {
        Timestamp time = getFirstLoginTimeByIP(ipAddress);
        return time == null ? "never" : getDateFormat().format(time);
    }

    @Override
    public Timestamp getLastLoginTimeByUser(int userId, String ipAddress) {
        return database.queryCallable(Procedure.LOGIN_HISTORY_GET_MAX_TIME_BY_USER.getName(), null, resultSet -> resultSet.getTimestamp("LoginTime"), userId, ipAddress);
    }

    @Override
    public String getLastLoginDateByUser(int userId, String ipAddress) {
        Timestamp time = getLastLoginTimeByUser(userId, ipAddress);
        return time == null ? "never" : getDateFormat().format(time);
    }

    @Override
    public Timestamp getLastLoginTimeByIP(String ipAddress) {
        return database.queryCallable(Procedure.LOGIN_HISTORY_GET_MAX_TIME_BY_IP.getName(), null, resultSet -> resultSet.getTimestamp("LoginTime"), ipAddress);
    }

    @Override
    public String getLastLoginDateByIP(String ipAddress) {
        Timestamp time = getLastLoginTimeByIP(ipAddress);
        return time == null ? "never" : getDateFormat().format(time);
    }

    @Override
    public Timestamp getLoginTime(UUID loginId) {
        return database.queryCallable(Procedure.GET_HISTORY_BY_ID.getName(), null, resultSet -> resultSet.getTimestamp("LoginTime"), loginId.toString());
    }

    @Override
    public String getLoginDate(UUID loginId) {
        Timestamp time = getLoginTime(loginId);
        return time == null ? "never" : getDateFormat().format(time);
    }

    @Override
    public Timestamp getLogoutTime(UUID loginId) {
        return database.queryCallable(Procedure.GET_HISTORY_BY_ID.getName(), null, resultSet -> resultSet.getTimestamp("LogoutTime"), loginId.toString());
    }

    @Override
    public String getLogoutDate(UUID loginId) {
        Timestamp time = getLogoutTime(loginId);
        return time == null ? "never" : getDateFormat().format(time);
    }

    @Override
    public String getClientVersion(UUID loginId) {
        return database.queryCallable(Procedure.GET_HISTORY_BY_ID.getName(), null, resultSet -> resultSet.getString("ClientVersion"), loginId.toString());
    }

    @Override
    public String getProtocolVersion(UUID loginId) {
        return database.queryCallable(Procedure.GET_HISTORY_BY_ID.getName(), null, resultSet -> resultSet.getString("ProtocolVersion"), loginId.toString());
    }

    private enum Procedure {
        DELETE("LoginHistory_Delete", "uid INT", "DELETE FROM [TABLE] WHERE UserID=uid;"),
        GET_HISTORY_BY_ID("LoginHistory_GetById", "lid UUID", "SELECT * FROM [TABLE] WHERE LoginID=lid;"),
        GET_HISTORY_BY_USER("LoginHistory_GetByUser", "uid INT", "SELECT LoginID FROM [TABLE] WHERE UserID=uid;"),
        GET_HISTORY_BY_USER_SORT("LoginHistory_GetByUserSort", "uid INT, lim INT", "SELECT LoginID FROM [TABLE] WHERE UserID=uid ORDER BY LoginTime DESC LIMIT lim;"),
        GET_LAST_QUIT("LoginHistory_GetLastQuit", "uid INT",
                "SELECT MAX(LogoutTime) AS LastQuit FROM [TABLE] WHERE UserID=uid AND LogoutTime IS NOT NULL;"),
        GET_LAST_LOGIN_ID("LoginHistory_GetLastLoginId", "uid INT",
                "SELECT LoginID FROM [TABLE] WHERE UserID=uid ORDER BY LogoutTime DESC LIMIT 1;"),
        GET_ID_BY_IP("LoginHistory_GetIdByIP", "ip INET6", "SELECT UserID FROM [TABLE] WHERE IPAddress=ip;"),
        LOGIN_HISTORY_GET_MIN_TIME_BY_IP("LoginHistory_GetMinTimeByIP", "ip INET6",
                "SELECT LoginTime FROM [TABLE] WHERE IPAddress=ip ORDER BY LoginTime ASC LIMIT 1;"),
        LOGIN_HISTORY_GET_MAX_TIME_BY_IP("LoginHistory_GetMaxTimeByIP", "ip INET6",
                "SELECT LoginTime FROM [TABLE] WHERE IPAddress=ip ORDER BY LoginTime DESC LIMIT 1;"),
        LOGIN_HISTORY_GET_MIN_TIME_BY_USER("LoginHistory_GetMinTimeByUser", "uid INT, ip INET6",
                "SELECT LoginTime FROM [TABLE] WHERE UserID=uid AND IPAddress=ip ORDER BY LoginTime ASC LIMIT 1;"),
        LOGIN_HISTORY_GET_MAX_TIME_BY_USER("LoginHistory_GetMaxTimeByUser", "uid INT, ip INET6",
                "SELECT LoginTime FROM [TABLE] WHERE UserID=uid AND IPAddress=ip ORDER BY LoginTime DESC LIMIT 1;");
        private static final Procedure[] VALUES = values();

        private final String name;
        private final String query;

        Procedure(final String name, final String input, final String query) {
            this.name = name;
            this.query = Database.getProcedureQuery(name, input, query);
        }

        public String getName() {
            return name;
        }

        public String getQuery() {
            return query.replace("[TABLE]", TABLE_NAME);
        }

        private static void loadAll(Database database) {
            for (Procedure procedure : VALUES) database.update(procedure.getQuery());
        }
    }
}
