package de.murmelmeister.murmelapi.punishment.log;

import de.murmelmeister.murmelapi.MurmelAPI;
import de.murmelmeister.murmelapi.database.Database;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public final class PunishmentLogProvider implements PunishmentLog {
    private static final String TABLE_NAME = "PunishmentLog";

    private final Database database;
    private final PunishmentReason reason;

    public PunishmentLogProvider(Database database, PunishmentReason reason) {
        this.database = database;
        this.reason = reason;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "LogID UUID, TypeID INT, PRIMARY KEY (LogID, TypeID), " +
                                         "FOREIGN KEY (TypeID) REFERENCES PunishmentTypes(ID), " +
                                         "UserID INT, FOREIGN KEY (UserID) REFERENCES Users(ID), " +
                                         "IPAddress INET6, " +
                                         "ExpiredTime BIGINT, " +
                                         "ReasonID INT, FOREIGN KEY (ReasonID) REFERENCES PunishmentReason(ReasonID), " +
                                         "CreatedBy INT, FOREIGN KEY (CreatedBy) REFERENCES Users(ID), " +
                                         "CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP(), " +
                                         "ModifiedBy INT, FOREIGN KEY (ModifiedBy) REFERENCES Users(ID), " +
                                         "ModifiedAt DATETIME DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP()");
        Procedure.loadAll(database);
    }

    @Override
    public boolean existsLog(UUID logId, int typeId) {
        return database.exists(Procedure.GET_LOG_BY_ID.getName(), logId.toString(), typeId);
    }

    @Override
    public UUID addLogIp(int executorId, int typeId, InetAddress inetAddress, int reasonId) {
        UUID logId = database.generateUniqueIdentifier(Procedure.CHECK_GENERATED_LOG_ID.getName());
        long duration = reason.getDuration(reasonId, typeId);
        long time = duration == -1L ? duration : System.currentTimeMillis() + duration;
        database.callUpdate(Procedure.CREATE_LOG_IP.getName(), logId.toString(), typeId, inetAddress.getHostAddress(), time, reasonId, executorId, executorId);
        return logId;
    }

    @Override
    public UUID addLogUser(int executorId, int typeId, int userId, InetAddress inetAddress, int reasonId) {
        UUID logId = database.generateUniqueIdentifier(Procedure.CHECK_GENERATED_LOG_ID.getName());
        long duration = reason.getDuration(reasonId, typeId);
        long time = duration == -1L ? duration : System.currentTimeMillis() + duration;
        database.callUpdate(Procedure.CREATE_LOG_USER.getName(), logId.toString(), typeId, userId, inetAddress.getHostAddress(), time, reasonId, executorId, executorId);
        return logId;
    }

    @Override
    public void deleteUserLogs(int userId) {
        database.callUpdate(Procedure.DELETE_LOG_USER.getName(), userId);
    }

    @Override
    public List<UUID> getLogs(int userId, int typeId) {
        return database.queryList(new LinkedList<>(), "LogID", String.class, Procedure.GET_LOG_BY_USER.getName(), userId, typeId)
                .parallelStream().map(UUID::fromString).toList();
    }

    @Override
    public List<UUID> getLogs(InetAddress inetAddress, int typeId) {
        return database.queryList(new LinkedList<>(), "LogID", String.class, Procedure.GET_LOG_BY_IP.getName(), inetAddress.getHostAddress(), typeId)
                .parallelStream().map(UUID::fromString).toList();
    }

    @Override
    public int getUserId(UUID logId, int typeId) {
        return database.query(-2, "UserID", int.class, Procedure.GET_LOG_BY_ID.getName(), logId.toString(), typeId);
    }

    @Override
    public String getIpAddress(UUID logId, int typeId) {
        return database.query(null, "IPAddress", String.class, Procedure.GET_LOG_BY_ID.getName(), logId.toString(), typeId);
    }

    @Override
    public int getReasonId(UUID logId, int typeId) {
        return database.query(-1, "ReasonID", int.class, Procedure.GET_LOG_BY_ID.getName(), logId.toString(), typeId);
    }

    @Override
    public void setReasonId(UUID logId, int typeId, int executorId, int reasonId) {
        database.callUpdate(Procedure.SET_LOG_REASON.getName(), logId.toString(), typeId, reasonId, executorId);
    }

    @Override
    public long getExpiredTime(UUID logId, int typeId) {
        return database.query(-2L, "ExpiredTime", long.class, Procedure.GET_LOG_BY_ID.getName(), logId.toString(), typeId);
    }

    @Override
    public String getExpiredDate(UUID logId, int typeId) {
        long time = getExpiredTime(logId, typeId);
        return time == -1 ? "never" : MurmelAPI.getDateFormat().format(time);
    }

    @Override
    public String setExpiredTime(UUID logId, int typeId, int executorId, long duration) {
        long expiredTime = duration == -1 ? duration : System.currentTimeMillis() + duration;
        database.callUpdate(Procedure.SET_LOG_EXPIRED_TIME.getName(), logId.toString(), typeId, expiredTime, executorId);
        return getExpiredDate(logId, typeId);
    }

    @Override
    public boolean isExpired(UUID logId, int typeId) {
        return database.query((byte) 0, "Expired", byte.class, Procedure.IS_LOG_EXPIRED.getName(), logId.toString(), typeId) == 1;
    }

    @Override
    public int getCreatedBy(UUID logId, int typeId) {
        return database.query(-2, "CreatedBy", int.class, Procedure.GET_LOG_BY_ID.getName(), logId.toString(), typeId);
    }

    @Override
    public Timestamp getCreatedAt(UUID logId, int typeId) {
        return database.query(null, "CreatedAt", Timestamp.class, Procedure.GET_LOG_BY_ID.getName(), logId.toString(), typeId);
    }

    @Override
    public int getModifiedBy(UUID logId, int typeId) {
        return database.query(-2, "ModifiedBy", int.class, Procedure.GET_LOG_BY_ID.getName(), logId.toString(), typeId);
    }

    @Override
    public Timestamp getModifiedAt(UUID logId, int typeId) {
        return database.query(null, "ModifiedAt", Timestamp.class, Procedure.GET_LOG_BY_ID.getName(), logId.toString(), typeId);
    }

    private enum Procedure {
        CREATE_LOG_IP("PunishmentLog_CreateIP", "lid UUID, tid INT, ip INET6, time BIGINT, rid INT, created INT, modified INT",
                "INSERT INTO [TABLE] (LogID,TypeID,IPAddress,ExpiredTime,ReasonID,CreatedBy,ModifiedBy) VALUES (lid,tid,ip,time,rid,created,modified);"),
        CREATE_LOG_USER("PunishmentLog_CreateUser", "lid UUID, tid INT, uid INT, ip INET6, time BIGINT, rid INT, created INT, modified INT",
                "INSERT INTO [TABLE] (LogID,TypeID,UserID,IPAddress,ExpiredTime,ReasonID,CreatedBy,ModifiedBy) VALUES (lid,tid,uid,ip,time,rid,created,modified);"),
        DELETE_LOG_USER("PunishmentLog_DeleteUser", "uid INT", "DELETE FROM [TABLE] WHERE UserID=uid;"),
        CHECK_GENERATED_LOG_ID("PunishmentLog_CheckGeneratedLogID", "lid UUID", "SELECT * FROM [TABLE] WHERE LogID=lid;"),
        GET_LOG_BY_ID("PunishmentLog_GetByID", "lid UUID, tid INT", "SELECT * FROM [TABLE] WHERE LogID=lid AND TypeID=tid;"),
        GET_LOG_BY_USER("PunishmentLog_GetByUser", "uid INT, tid INT", "SELECT LogID FROM [TABLE] WHERE UserID=uid AND TypeID=tid;"),
        GET_LOG_BY_IP("PunishmentLog_GetByIP", "ip INET6, tid INT", "SELECT LogID FROM [TABLE] WHERE IPAddress=ip AND TypeID=tid;"),
        SET_LOG_EXPIRED_TIME("PunishmentLog_SetExpiredTime", "lid UUID, tid INT, time BIGINT, modified INT",
                "UPDATE [TABLE] SET ExpiredTime=time, ModifiedBy=modified WHERE LogID=lid AND TypeID=tid;"),
        SET_LOG_REASON("PunishmentLog_SetReason", "lid UUID, tid INT, reason TEXT, modified INT",
                "UPDATE [TABLE] SET ReasonID=reason, ModifiedBy=modified WHERE LogID=lid AND TypeID=tid;"),
        IS_LOG_EXPIRED("PunishmentLog_IsExpired", "lid UUID, tid INT", "SELECT ExpiredTime<=CURRENT_TIMESTAMP() AS Expired FROM [TABLE] WHERE LogID=lid AND TypeID=tid;");
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
