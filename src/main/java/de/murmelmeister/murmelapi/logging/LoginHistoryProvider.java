package de.murmelmeister.murmelapi.logging;

import de.murmelmeister.murmelapi.database.Database;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import static de.murmelmeister.murmelapi.MurmelAPI.getDateFormat;

/**
 * LoginHistoryProvider class to manage login history in the database.
 * This class implements the LoginHistory interface and provides methods to interact with login history data.
 */
public final class LoginHistoryProvider implements LoginHistory {
    private static final String TABLE_NAME = "login_history";
    private final Database database;

    public LoginHistoryProvider(Database database) {
        this.database = database;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "id VARCHAR(36) PRIMARY KEY, " +
                                         "userId INT, " +
                                         "loginTime DATETIME, " +
                                         "logoutTime DATETIME, " +
                                         "ipAddress VARCHAR(45), " +
                                         "clientVersion VARCHAR(100), " +
                                         "protocolVersion VARCHAR(100), " +
                                         "FOREIGN KEY (userId) REFERENCES users(id)");
        database.update("CREATE INDEX IF NOT EXISTS userId_index ON " + TABLE_NAME + " (userId)");
        database.update("CREATE INDEX IF NOT EXISTS ipAddress_index ON " + TABLE_NAME + " (ipAddress)");
        Procedure.loadAll(database);
    }

    @Override
    public boolean existsLogin(UUID loginId) {
        return loginId != null && database.existsCallable(Procedure.GET_DATA_BY_ID.getName(), loginId.toString());
    }

    @Override
    public int deleteUserLogins(int userId) {
        if (userId < 1) return 0;
        return database.updateCallable(Procedure.DELETE.getName(), userId);
    }

    @Override
    public int getUserId(UUID loginId) {
        if (loginId == null) return -2;
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), null, result -> result.getInt("userId"), loginId.toString());
    }

    @Override
    public String getIpAddress(UUID loginId) {
        if (loginId == null) return null;
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), null, result -> result.getString("ipAddress"), loginId.toString());
    }

    @Override
    public Timestamp getLoginTime(UUID loginId) {
        if (loginId == null) return null;
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), null, result -> result.getTimestamp("loginTime"), loginId.toString());
    }

    @Override
    public String getLoginDate(UUID loginId) {
        Timestamp loginTime = getLoginTime(loginId);
        return loginTime == null ? null : getDateFormat().format(loginTime);
    }

    @Override
    public Timestamp getLogoutTime(UUID loginId) {
        if (loginId == null) return null;
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), null, result -> result.getTimestamp("logoutTime"), loginId.toString());
    }

    @Override
    public String getLogoutDate(UUID loginId) {
        Timestamp logoutTime = getLogoutTime(loginId);
        return logoutTime == null ? null : getDateFormat().format(logoutTime);
    }

    @Override
    public String getClientVersion(UUID loginId) {
        if (loginId == null) return null;
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), null, result -> result.getString("clientVersion"), loginId.toString());
    }

    @Override
    public String getProtocolVersion(UUID loginId) {
        if (loginId == null) return null;
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), null, result -> result.getString("protocolVersion"), loginId.toString());
    }

    @Override
    public List<UUID> getUserLogins(int userId) {
        if (userId < 1) return null;
        return database.queryListCallable(Procedure.GET_DATA_BY_USER_ID.getName(), result -> UUID.fromString(result.getString("id")), userId);
    }

    @Override
    public List<UUID> getSortedUserLogins(int userId, int limit) {
        if (userId < 1) return null;
        return database.queryListCallable(Procedure.GET_DATA_BY_USER_ID_SORT.getName(), result -> UUID.fromString(result.getString("id")), userId, limit);
    }

    @Override
    public UUID getLastLoginId(int userId) {
        if (userId < 1) return null;
        return database.queryCallable(Procedure.GET_LAST_ID.getName(), null, result -> UUID.fromString(result.getString("id")), userId);
    }

    @Override
    public Timestamp getLastQuit(int userId) {
        if (userId < 1) return null;
        return database.queryCallable(Procedure.GET_LAST_QUIT.getName(), null, result -> result.getTimestamp("lastQuit"), userId);
    }

    @Override
    public String getLastQuitDate(int userId) {
        Timestamp lastQuit = getLastQuit(userId);
        return lastQuit == null ? null : getDateFormat().format(lastQuit);
    }

    @Override
    public List<Integer> getMultiUserIds(String ipAddress) {
        if (ipAddress == null) return null;
        return database.queryListCallable(Procedure.GET_DATA_BY_IP_ADDRESS.getName(), result -> result.getInt("userId"), ipAddress);
    }

    @Override
    public String getTimeRange(int userId, String ipAddress) {
        if (userId < 1 || ipAddress == null) return null;
        return database.queryCallable(Procedure.GET_DATE_BY_USER_AND_IP.getName(), null, result -> {
            Timestamp minTime = result.getTimestamp("minTime");
            Timestamp maxTime = result.getTimestamp("maxTime");
            String minDate = minTime == null ? null : getDateFormat().format(minTime);
            String maxDate = maxTime == null ? null : getDateFormat().format(maxTime);
            return minDate + " - " + maxDate;
        }, userId);
    }

    private enum Procedure {
        DELETE("loginHistory_delete", "p_userId INT", "DELETE FROM [TABLE] WHERE userId=p_userId;"),
        GET_DATA_BY_ID("loginHistory_getDataById", "p_id VARCHAR(36)", "SELECT * FROM [TABLE] WHERE id=p_id;"),
        GET_DATA_BY_USER_ID("loginHistory_getDataByUserId", "p_userId INT", "SELECT id FROM [TABLE] WHERE userId=p_userId;"),
        GET_DATA_BY_IP_ADDRESS("loginHistory_getDataByIpAddress", "p_ipAddress VARCHAR(45)", "SELECT userId FROM [TABLE] WHERE ipAddress=p_ipAddress;"),
        GET_DATA_BY_USER_ID_SORT("loginHistory_getDataByUserIdSort", "p_userId INT, p_limit INT",
                "SELECT id FROM [TABLE] WHERE userId=p_userId ORDER BY loginTime DESC LIMIT p_limit;"),
        GET_LAST_QUIT("loginHistory_getLastQuit", "p_userId INT",
                "SELECT MAX(logoutTime) AS lastQuit FROM [TABLE] WHERE userId=p_userId AND logoutTime IS NOT NULL;"),
        GET_LAST_ID("loginHistory_getLastId", "p_userId INT",
                "SELECT id FROM [TABLE] WHERE userId=p_userId ORDER BY logoutTime DESC LIMIT 1;"),
        GET_DATE_BY_USER_AND_IP("loginHistory_getDateByUserAndIp", "p_userId INT, p_ipAddress VARCHAR(45)",
                "SELECT MIN(loginTime) AS minTime, MAX(logoutTime) AS maxTime FROM [TABLE] WHERE userId=p_userId AND ipAddress=p_ipAddress LIMIT 1;");
        private static final Procedure[] VALUES = values();

        private final String name;
        private final String query;

        Procedure(String name, String input, String query) {
            this.name = name;
            this.query = Database.getProcedureQuery(name, input, query);
        }

        public String getName() {
            return name;
        }

        public String getQuery() {
            return query.replace("[TABLE]", TABLE_NAME);
        }

        public static void loadAll(Database database) {
            for (Procedure procedure : VALUES)
                database.update(procedure.getQuery());
        }
    }
}
