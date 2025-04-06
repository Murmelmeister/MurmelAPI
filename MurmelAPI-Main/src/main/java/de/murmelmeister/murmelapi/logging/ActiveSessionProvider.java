package de.murmelmeister.murmelapi.logging;

import de.murmelmeister.murmelapi.database.Database;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.UUID;

import static de.murmelmeister.murmelapi.MurmelAPI.getDateFormat;

public final class ActiveSessionProvider implements ActiveSession {
    private static final String TABLE_NAME = "ActiveSessions";

    private final Database database;

    public ActiveSessionProvider(Database database) {
        this.database = database;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "SessionID UUID PRIMARY KEY, " +
                                         "UserID INT UNIQUE, FOREIGN KEY (UserID) REFERENCES Users(ID), " +
                                         "IPAddress INET6, " +
                                         "LoginTime DATETIME DEFAULT CURRENT_TIMESTAMP(), " +
                                         "ClientVersion VARCHAR(100), " +
                                         "ProtocolVersion VARCHAR(50)");
        Procedure.loadAll(database);
    }

    @Override
    public boolean existsSession(int userId) {
        return database.existsCallable(Procedure.IS_ONLINE.getName(), userId);
    }

    @Override
    public void startSession(int userId, InetAddress inetAddress, String clientVersion, String protocolVersion) {
        database.updateCallable(Procedure.START.getName(), UUID.randomUUID(), userId, inetAddress.getHostAddress(), clientVersion, protocolVersion);
    }

    @Override
    public void closeSession(int userId) {
        database.updateCallable(Procedure.CLOSE.getName(), userId);
    }

    @Override
    public UUID getSessionId(int userId) {
        return database.queryCallable(Procedure.GET_DATA.getName(), null, resultSet -> {
            String sessionId = resultSet.getString("SessionID");
            return sessionId != null ? UUID.fromString(sessionId) : null;
        }, userId);
    }

    @Override
    public String getIpAddress(int userId) {
        return database.queryCallable(Procedure.GET_DATA.getName(), null, resultSet -> resultSet.getString("IPAddress"), userId);
    }

    @Override
    public Timestamp getLoginTime(int userId) {
        return database.queryCallable(Procedure.GET_DATA.getName(), null, resultSet -> resultSet.getTimestamp("LoginTime"), userId);
    }

    @Override
    public String getLoginDate(int userId) {
        Timestamp time = getLoginTime(userId);
        return time == null ? "never" : getDateFormat().format(time);
    }

    @Override
    public String getClientVersion(int userId) {
        return database.queryCallable(Procedure.GET_DATA.getName(), null, resultSet -> resultSet.getString("ClientVersion"), userId);
    }

    @Override
    public String getProtocolVersion(int userId) {
        return database.queryCallable(Procedure.GET_DATA.getName(), null, resultSet -> resultSet.getString("ProtocolVersion"), userId);
    }

    @Override
    public boolean isOnline(int userId) {
        int sessions = database.queryCallable(Procedure.IS_ONLINE.getName(), 0, resultSet -> resultSet.getInt("ActiveSessions"), userId);
        return sessions > 0;
    }

    private enum Procedure {
        START("ActiveSessions_Start", "session UUID, uid INT, ip INET6, cv VARCHAR(100), pv VARCHAR(50)",
                "INSERT INTO [TABLE] (SessionID,UserID,IPAddress,ClientVersion,ProtocolVersion) VALUES (session,uid,ip,cv,pv);"),
        CLOSE("ActiveSessions_Close", "uid INT", """
                INSERT INTO LoginHistory (LoginID, UserID, IPAddress, LoginTime, LogoutTime, ClientVersion, ProtocolVersion)
                SELECT SessionID, uid, IPAddress, LoginTime, CURRENT_TIMESTAMP(), ClientVersion, ProtocolVersion
                FROM [TABLE] WHERE UserID=uid;
                DELETE FROM [TABLE] WHERE UserID=uid;"""),
        IS_ONLINE("ActiveSessions_IsOnline", "uid INT", "SELECT COUNT(*) AS ActiveSessions FROM [TABLE] WHERE UserID=uid;"),
        GET_DATA("ActiveSessions_GetData", "uid INT", "SELECT SessionID, IPAddress, LoginTime, ClientVersion, ProtocolVersion FROM [TABLE] WHERE UserID=uid;");
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
