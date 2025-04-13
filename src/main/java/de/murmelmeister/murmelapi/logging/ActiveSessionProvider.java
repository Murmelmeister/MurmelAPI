package de.murmelmeister.murmelapi.logging;

import de.murmelmeister.murmelapi.database.Database;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.UUID;

import static de.murmelmeister.murmelapi.MurmelAPI.getDateFormat;

/**
 * ActiveSessionProvider class to manage active sessions in the database.
 * This class implements the ActiveSession interface and provides methods to interact with active session data.
 */
public final class ActiveSessionProvider implements ActiveSession {
    private static final String TABLE_NAME = "active_sessions";
    private final Database database;

    public ActiveSessionProvider(Database database) {
        this.database = database;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "id VARCHAR(36) PRIMARY KEY, " +
                                         "userId INT UNIQUE, " +
                                         "loginTime DATETIME DEFAULT CURRENT_TIMESTAMP(), " +
                                         "ipAddress VARCHAR(45), " +
                                         "clientVersion VARCHAR(100), " +
                                         "protocolVersion VARCHAR(100), " +
                                         "FOREIGN KEY (userId) REFERENCES users(id)");
        Procedure.loadAll(database);
    }

    @Override
    public boolean existsSession(int userId) {
        return userId > 0 && database.existsCallable(Procedure.IS_ONLINE.getName(), userId);
    }

    @Override
    public int startSession(int userId, String ipAddress, String clientVersion, String protocolVersion) {
        if (userId < 1) return 0;
        return database.updateCallable(Procedure.START.getName(), UUID.randomUUID(), userId, ipAddress, clientVersion, protocolVersion);
    }

    @Override
    public int startSession(int userId, InetAddress inetAddress, String clientVersion, String protocolVersion) {
        return startSession(userId, inetAddress.getHostAddress(), clientVersion, protocolVersion);
    }

    @Override
    public int closeSession(int userId) {
        if (userId < 1) return 0;
        return database.updateCallable(Procedure.CLOSE.getName(), userId);
    }

    @Override
    public UUID getSessionId(int userId) {
        if (userId < 1) return null;
        return database.queryCallable(Procedure.GET_DATA.getName(), null, result -> UUID.fromString(result.getString("id")), userId);
    }

    @Override
    public String getIpAddress(int userId) {
        if (userId < 1) return null;
        return database.queryCallable(Procedure.GET_DATA.getName(), null, result -> result.getString("ipAddress"), userId);
    }

    @Override
    public Timestamp getLoginTime(int userId) {
        if (userId < 1) return null;
        return database.queryCallable(Procedure.GET_DATA.getName(), null, result -> result.getTimestamp("loginTime"), userId);
    }

    @Override
    public String getLoginDate(int userId) {
        if (userId < 1) return null;
        Timestamp loginTime = getLoginTime(userId);
        return loginTime == null ? null : getDateFormat().format(loginTime);
    }

    @Override
    public String getClientVersion(int userId) {
        if (userId < 1) return null;
        return database.queryCallable(Procedure.GET_DATA.getName(), null, result -> result.getString("clientVersion"), userId);
    }

    @Override
    public String getProtocolVersion(int userId) {
        if (userId < 1) return null;
        return database.queryCallable(Procedure.GET_DATA.getName(), null, result -> result.getString("protocolVersion"), userId);
    }

    @Override
    public boolean isOnline(int userId) {
        if (userId < 1) return false;
        return database.queryCallable(Procedure.IS_ONLINE.getName(), false, result -> result.getInt("activeSessions") > 0, userId);
    }

    private enum Procedure {
        START("activeSession_start", "p_id VARCHAR(36), p_userId INT, p_ipAddress VARCHAR(45), p_clientVersion VARCHAR(100), p_protocolVersion VARCHAR(100)",
                "INSERT INTO [TABLE] (id, userId, ipAddress, clientVersion, protocolVersion) VALUES (p_id, p_userId, p_ipAddress, p_clientVersion, p_protocolVersion);"),
        CLOSE("activeSession_close", "p_userId INT", """
                INSERT INTO login_history (id, userId, loginTime, logoutTime, ipAddress, clientVersion, protocolVersion)
                SELECT id, p_userId, loginTime, CURRENT_TIMESTAMP(), ipAddress, clientVersion, protocolVersion
                FROM [TABLE] WHERE userId=p_userId;
                DELETE FROM [TABLE] WHERE userId=p_userId;"""),
        IS_ONLINE("activeSession_isOnline", "p_userId INT", "SELECT COUNT(*) AS activeSessions FROM [TABLE] WHERE userId=p_userId;"),
        GET_DATA("activeSession_getData", "p_userId INT", "SELECT * FROM [TABLE] WHERE userId=p_userId;");
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
