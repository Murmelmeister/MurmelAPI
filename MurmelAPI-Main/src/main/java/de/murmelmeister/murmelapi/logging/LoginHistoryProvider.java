package de.murmelmeister.murmelapi.logging;

import de.murmelmeister.murmelapi.database.Database;

import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

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
        return database.exists(Procedure.GET_HISTORY_BY_ID.getName(), loginId.toString());
    }

    @Override
    public void deleteUserLogin(int userId) {
        database.callUpdate(Procedure.DELETE.getName(), userId);
    }

    @Override
    public List<UUID> getLogins(int userId) {
        return database.queryList(new LinkedList<>(), "LoginID", String.class, Procedure.GET_HISTORY_BY_USER.getName(), userId)
                .parallelStream().map(UUID::fromString).toList();
    }

    @Override
    public List<UUID> getSortedLogins(int userId) {
        return database.queryList(new LinkedList<>(), "LoginID", String.class, Procedure.GET_HISTORY_BY_USER_SORT.getName(), userId, 10)
                .parallelStream().map(UUID::fromString).toList();
    }

    private <T> T get(UUID loginId, String columnName, T defaultValue, Class<T> type) {
        return database.query(defaultValue, columnName, type, Procedure.GET_HISTORY_BY_ID.getName(), loginId.toString());
    }

    @Override
    public UUID getLastLoginId(int userId) {
        return UUID.fromString(database.query(null, "LoginID", String.class, Procedure.GET_LAST_LOGIN_ID.getName(), userId));
    }

    @Override
    public Timestamp getLastQuit(int userId) {
        return database.query(null, "LastQuit", Timestamp.class, Procedure.GET_LAST_QUIT.getName(), userId);
    }

    @Override
    public List<Integer> getUserIdsByIP(String ipAddress) {
        return database.queryList(new LinkedList<>(), "UserID", int.class, Procedure.GET_ID_BY_IP.getName(), ipAddress);
    }

    @Override
    public int getUserId(UUID loginId) {
        return get(loginId, "UserID", -2, int.class);
    }

    @Override
    public String getIPAddress(UUID loginId) {
        return get(loginId, "IPAddress", null, String.class);
    }

    @Override
    public Timestamp getFirstLoginTimeByUser(int userId, String ipAddress) {
        return database.query(null, "LoginTime", Timestamp.class, Procedure.LOGIN_HISTORY_GET_MIN_TIME_BY_USER.getName(), userId, ipAddress);
    }

    @Override
    public Timestamp getFirstLoginTimeByIP(String ipAddress) {
        return database.query(null, "LoginTime", Timestamp.class, Procedure.LOGIN_HISTORY_GET_MIN_TIME_BY_IP.getName(), ipAddress);
    }

    @Override
    public Timestamp getLastLoginTimeByUser(int userId, String ipAddress) {
        return database.query(null, "LoginTime", Timestamp.class, Procedure.LOGIN_HISTORY_GET_MAX_TIME_BY_USER.getName(), userId, ipAddress);
    }

    @Override
    public Timestamp getLastLoginTimeByIP(String ipAddress) {
        return database.query(null, "LoginTime", Timestamp.class, Procedure.LOGIN_HISTORY_GET_MAX_TIME_BY_IP.getName(), ipAddress);
    }

    @Override
    public Timestamp getLoginTime(UUID loginId) {
        return get(loginId, "LoginTime", null, Timestamp.class);
    }

    @Override
    public Timestamp getLogoutTime(UUID loginId) {
        return get(loginId, "LogoutTime", null, Timestamp.class);
    }

    @Override
    public String getLogoutDate(UUID loginId) {
        Timestamp time = getLogoutTime(loginId);
        return time == null ? "never" : time.toString();
    }

    @Override
    public String getClientVersion(UUID loginId) {
        return get(loginId, "ClientVersion", null, String.class);
    }

    @Override
    public String getProtocolVersion(UUID loginId) {
        return get(loginId, "ProtocolVersion", null, String.class);
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
