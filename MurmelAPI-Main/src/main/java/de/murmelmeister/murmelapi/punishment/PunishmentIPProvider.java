package de.murmelmeister.murmelapi.punishment;

import de.murmelmeister.murmelapi.database.Database;
import de.murmelmeister.murmelapi.punishment.log.PunishmentLog;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public final class PunishmentIPProvider implements PunishmentIP {
    private static final String TABLE_NAME = "PunishmentIPs";

    private final Database database;
    private final PunishmentReason reason;
    private final PunishmentLog log;

    public PunishmentIPProvider(Database database, PunishmentReason reason, PunishmentLog log) {
        this.database = database;
        this.reason = reason;
        this.log = log;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "IPAddress INET6, TypeID INT, PRIMARY KEY (IPAddress, TypeID), " +
                                         "FOREIGN KEY (TypeID) REFERENCES PunishmentTypes(ID), " +
                                         "LogID UUID, FOREIGN KEY (LogID) REFERENCES PunishmentLog(LogID)");
        Procedure.loadAll(database);
    }

    @Override
    public boolean exists(InetAddress inetAddress, int typeId) {
        return database.existsCallable(Procedure.GET_BY_ID.getName(), inetAddress.getHostAddress(), typeId);
    }

    @Override
    public void punish(InetAddress inetAddress, int typeId, int executorId, int reasonId) {
        UUID logId = log.addLogIp(executorId, typeId, inetAddress, reasonId);
        database.updateCallable(Procedure.CREATE.getName(), inetAddress.getHostAddress(), typeId, logId.toString());
    }

    @Override
    public void unpunished(InetAddress inetAddress, int typeId) {
        database.updateCallable(Procedure.DELETE.getName(), inetAddress.getHostAddress(), typeId);
    }

    @Override
    public List<String> getIps(int typeId) {
        return database.queryListCallable(Procedure.GET_ALL.getName(), new LinkedList<>(), resultSet -> resultSet.getString("IPAddress"), typeId);
    }

    @Override
    public UUID getLogId(InetAddress inetAddress, int typeId) {
        String id = database.queryCallable(Procedure.GET_BY_ID.getName(), null, resultSet -> resultSet.getString("LogID"), inetAddress.getHostAddress(), typeId);
        return id != null ? UUID.fromString(id) : null;
    }

    @Override
    public int getReasonId(InetAddress inetAddress, int typeId) {
        UUID logId = getLogId(inetAddress, typeId);
        return logId != null ? log.getReasonId(logId, typeId) : -1;
    }

    @Override
    public void setReasonId(InetAddress inetAddress, int typeId, int executorId, int reasonId) {
        UUID logId = getLogId(inetAddress, typeId);
        log.setReasonId(logId, typeId, executorId, reasonId);
    }

    @Override
    public String getReason(InetAddress inetAddress, int typeId) {
        int reasonId = getReasonId(inetAddress, typeId);
        return reason.getReason(reasonId, typeId);
    }

    @Override
    public long getDuration(InetAddress inetAddress, int typeId) {
        int reasonId = getReasonId(inetAddress, typeId);
        return reason.getDuration(reasonId, typeId);
    }

    @Override
    public Timestamp getExpiredAt(InetAddress inetAddress, int typeId) {
        UUID logId = getLogId(inetAddress, typeId);
        return logId != null ? log.getExpiredAt(logId, typeId) : null;
    }

    @Override
    public void setExpiredAt(InetAddress inetAddress, int typeId, int executorId, long time) {
        UUID logId = getLogId(inetAddress, typeId);
        log.setExpiredAt(logId, typeId, executorId, time);
    }

    @Override
    public boolean isPunished(InetAddress inetAddress, int typeId) {
        UUID logId = getLogId(inetAddress, typeId);
        return logId != null && !log.isExpired(logId, typeId);
    }

    @Override
    public int getCreatedBy(InetAddress inetAddress, int typeId) {
        UUID logId = getLogId(inetAddress, typeId);
        return logId != null ? log.getCreatedBy(logId, typeId) : -2;
    }

    @Override
    public Timestamp getCreatedAt(InetAddress inetAddress, int typeId) {
        UUID logId = getLogId(inetAddress, typeId);
        return logId != null ? log.getCreatedAt(logId, typeId) : null;
    }

    @Override
    public int getModifiedBy(InetAddress inetAddress, int typeId) {
        UUID logId = getLogId(inetAddress, typeId);
        return logId != null ? log.getModifiedBy(logId, typeId) : -2;
    }

    @Override
    public Timestamp getModifiedAt(InetAddress inetAddress, int typeId) {
        UUID logId = getLogId(inetAddress, typeId);
        return logId != null ? log.getModifiedAt(logId, typeId) : null;
    }

    private enum Procedure {
        GET_BY_ID("PunishmentIPs_GetById", "ip INET6, tid INT", "SELECT * FROM [TABLE] WHERE IPAddress=ip AND TypeID=tid;"),
        GET_ALL("PunishmentIPs_GetAll", "tid INT", "SELECT IPAddress FROM [TABLE] WHERE TypeID=tid;"),
        CREATE("PunishmentIPs_Create", "ip INET6, tid INT, lid UUID", "INSERT INTO [TABLE] VALUES (ip,tid,lid);"),
        DELETE("PunishmentIPs_Delete", "ip INET6, tid INT", "DELETE FROM [TABLE] WHERE IPAddress=ip AND TypeID=tid;");
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
