package de.murmelmeister.murmelapi.punishment;

import de.murmelmeister.murmelapi.database.Database;
import de.murmelmeister.murmelapi.punishment.log.PunishmentLog;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

public final class PunishmentUserProvider implements PunishmentUser {
    private static final String TABLE_NAME = "PunishmentUsers";

    private final Database database;
    private final PunishmentReason reason;
    private final PunishmentLog log;

    // TODO: Delete User

    public PunishmentUserProvider(Database database, PunishmentReason reason, PunishmentLog log) {
        this.database = database;
        this.reason = reason;
        this.log = log;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "UserID INT, TypeID INT, PRIMARY KEY (UserID, TypeID), " +
                                         "FOREIGN KEY (UserID) REFERENCES Users(ID), " +
                                         "FOREIGN KEY (TypeID) REFERENCES PunishmentTypes(ID), " +
                                         "LogID UUID, FOREIGN KEY (LogID) REFERENCES PunishmentLog(LogID)");
        Procedure.loadAll(database);
    }

    @Override
    public boolean exists(int userId, int typeId) {
        return database.exists(Procedure.GET_BY_ID.getName(), userId, typeId);
    }

    @Override
    public void punish(int userId, int typeId, int executorId, InetAddress inetAddress, int reasonId) {
        UUID logId = log.addLogUser(executorId, typeId, userId, inetAddress, reasonId);
        database.callUpdate(Procedure.CREATE.getName(), userId, typeId, logId.toString());
    }

    @Override
    public void unpunished(int userId, int typeId) {
        database.callUpdate(Procedure.DELETE.getName(), userId, typeId);
    }

    @Override
    public List<Integer> getUsers(int typeId) {
        return database.queryList(null, "UserID", int.class, Procedure.GET_ALL.getName(), typeId);
    }

    @Override
    public UUID getLogId(int userId, int typeId) {
        String id = database.query(null, "LogID", String.class, Procedure.GET_BY_ID.getName(), userId, typeId);
        return id != null ? UUID.fromString(id) : null;
    }

    @Override
    public int getReasonId(int userId, int typeId) {
        UUID logId = getLogId(userId, typeId);
        return logId != null ? log.getReasonId(logId, typeId) : -1;
    }

    @Override
    public void setReasonId(int userId, int typeId, int executorId, int reasonId) {
        UUID logId = getLogId(userId, typeId);
        log.setReasonId(logId, typeId, executorId, reasonId);
    }

    @Override
    public String getReason(int userId, int typeId) {
        int reasonId = getReasonId(userId, typeId);
        return reason.getReason(reasonId, typeId);
    }

    @Override
    public long getDuration(int userId, int typeId) {
        int reasonId = getReasonId(userId, typeId);
        return reason.getDuration(reasonId, typeId);
    }

    @Override
    public long getExpiredTime(int userId, int typeId) {
        UUID logId = getLogId(userId, typeId);
        return logId != null ? log.getExpiredTime(logId, typeId) : -2;
    }

    @Override
    public String getExpiredDate(int userId, int typeId) {
        UUID logId = getLogId(userId, typeId);
        return logId != null ? log.getExpiredDate(logId, typeId) : null;
    }

    @Override
    public void setExpiredTime(int userId, int typeId, int executorId, long time) {
        UUID logId = getLogId(userId, typeId);
        log.setExpiredTime(logId, typeId, executorId, time);
    }

    @Override
    public boolean isPunished(int userId, int typeId) {
        UUID logId = getLogId(userId, typeId);
        return logId != null && log.isExpired(logId, typeId);
    }

    @Override
    public int getCreatedBy(int userId, int typeId) {
        UUID logId = getLogId(userId, typeId);
        return logId != null ? log.getCreatedBy(logId, typeId) : -2;
    }

    @Override
    public Timestamp getCreatedAt(int userId, int typeId) {
        UUID logId = getLogId(userId, typeId);
        return logId != null ? log.getCreatedAt(logId, typeId) : null;
    }

    @Override
    public int getModifiedBy(int userId, int typeId) {
        UUID logId = getLogId(userId, typeId);
        return logId != null ? log.getModifiedBy(logId, typeId) : -2;
    }

    @Override
    public Timestamp getModifiedAt(int userId, int typeId) {
        UUID logId = getLogId(userId, typeId);
        return logId != null ? log.getModifiedAt(logId, typeId) : null;
    }

    private enum Procedure {
        GET_BY_ID("PunishmentUser_GetById", "uid INT, tid INT", "SELECT * FROM [TABLE] WHERE UserID=uid AND TypeID=tid;"),
        GET_ALL("PunishmentUser_GetAll", "tid INT", "SELECT * FROM [TABLE] WHERE TypeID=tid;"),
        CREATE("PunishmentUser_Create", "uid INT, tid INT, lid UUID", "INSERT INTO [TABLE] VALUES (uid,tid,lid);"),
        DELETE("PunishmentUser_Delete", "uid INT, tid INT", "DELETE FROM [TABLE] WHERE UserID=uid AND TypeID=tid;");
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
