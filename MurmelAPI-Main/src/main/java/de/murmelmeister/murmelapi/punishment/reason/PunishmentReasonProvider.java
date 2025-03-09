package de.murmelmeister.murmelapi.punishment.reason;

import de.murmelmeister.murmelapi.database.Database;

import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;

public final class PunishmentReasonProvider implements PunishmentReason {
    private static final String TABLE_NAME = "PunishmentReason";

    private final Database database;

    public PunishmentReasonProvider(Database database) {
        this.database = database;
        if (!exists(99, 1)) // Security ban
            add(99, 1, -1, "Security", -1, true, true);
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "ReasonID INT, TypeID INT, PRIMARY KEY (ReasonID, TypeID), " +
                                         "FOREIGN KEY (TypeID) REFERENCES PunishmentTypes(ID), " +
                                         "Reason TEXT, Duration BIGINT, AutoFlagIP BOOL, AutoPunish BOOL, " +
                                         "CreatedBy INT, FOREIGN KEY (CreatedBy) REFERENCES Users(ID), " +
                                         "CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP(), " +
                                         "ModifiedBy INT, FOREIGN KEY (ModifiedBy) REFERENCES Users(ID), " +
                                         "ModifiedAt DATETIME DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP()");
        Procedure.loadAll(database);
    }

    @Override
    public boolean exists(int reasonId, int typeId) {
        return database.exists(Procedure.GET_REASON_BY_ID.getName(), reasonId, typeId);
    }

    @Override
    public void add(int reasonId, int typeId, int executorId, String reason, long duration, boolean autoFlagIp, boolean autoPunish) {
        database.callUpdate(Procedure.CREATE_REASON.getName(), reasonId, typeId, reason, duration, autoFlagIp, autoPunish, executorId, executorId);
    }

    @Override
    public void remove(int reasonId, int typeId) {
        database.callUpdate(Procedure.DELETE_REASON.getName(), reasonId, typeId);
    }

    @Override
    public List<Integer> getReasons(int typeId) {
        return database.queryList(new LinkedList<>(), "ReasonID", Integer.class, Procedure.GET_REASON_BY_TYPE.getName(), typeId);
    }

    @Override
    public String getReason(int reasonId, int typeId) {
        return database.query(null, "Reason", String.class, Procedure.GET_REASON_BY_ID.getName(), reasonId, typeId);
    }

    @Override
    public void setReason(int reasonId, int typeId, int executorId, String reason) {
        database.callUpdate(Procedure.SET_REASON.getName(), reasonId, typeId, reason, executorId);
    }

    @Override
    public long getDuration(int reasonId, int typeId) {
        return database.query(-1L, "Duration", long.class, Procedure.GET_REASON_BY_ID.getName(), reasonId, typeId);
    }

    @Override
    public void setDuration(int reasonId, int typeId, int executorId, long duration) {
        database.callUpdate(Procedure.SET_DURATION.getName(), reasonId, typeId, duration, executorId);
    }

    @Override
    public boolean getAutoFlagIP(int reasonId, int typeId) {
        return database.query((byte) 0, "AutoFlagIP", byte.class, Procedure.GET_REASON_BY_ID.getName(), reasonId, typeId) == 1;
    }

    @Override
    public void setAutoFlagIP(int reasonId, int typeId, int executorId, boolean autoFlagIp) {
        database.callUpdate(Procedure.SET_AUTO_FLAG_IP.getName(), reasonId, typeId, autoFlagIp, executorId);
    }

    @Override
    public boolean getAutoPunish(int reasonId, int typeId) {
        return database.query((byte) 0, "AutoPunish", byte.class, Procedure.GET_REASON_BY_ID.getName(), reasonId, typeId) == 1;
    }

    @Override
    public void setAutoPunish(int reasonId, int typeId, int executorId, boolean autoPunish) {
        database.callUpdate(Procedure.SET_AUTO_PUNISH.getName(), reasonId, typeId, autoPunish, executorId);
    }

    @Override
    public int getCreatedBy(int reasonId, int typeId) {
        return database.query(-2, "CreatedBy", int.class, Procedure.GET_REASON_BY_ID.getName(), reasonId, typeId);
    }

    @Override
    public Timestamp getCreatedAt(int reasonId, int typeId) {
        return database.query(null, "CreatedAt", Timestamp.class, Procedure.GET_REASON_BY_ID.getName(), reasonId, typeId);
    }

    @Override
    public int getModifiedBy(int reasonId, int typeId) {
        return database.query(-2, "ModifiedBy", int.class, Procedure.GET_REASON_BY_ID.getName(), reasonId, typeId);
    }

    @Override
    public Timestamp getModifiedAt(int reasonId, int typeId) {
        return database.query(null, "ModifiedAt", Timestamp.class, Procedure.GET_REASON_BY_ID.getName(), reasonId, typeId);
    }

    private enum Procedure {
        CREATE_REASON("PunishmentReason_Create", "rid INT, tid INT, rea TEXT, dur BIGINT, flag BOOL, punish BOOL, created INT, modified INT",
                "INSERT INTO [TABLE] (ReasonID,TypeID,Reason,Duration,AutoFlagIP,AutoPunish,CreatedBy,ModifiedBy) VALUES (rid,tid,rea,dur,flag,punish,created,modified);"),
        DELETE_REASON("PunishmentReason_Delete", "rid INT, tid INT", "DELETE FROM [TABLE] WHERE ReasonID=rid AND TypeID=tid;"),
        GET_REASON_BY_ID("PunishmentReason_GetByID", "rid INT, tid INT", "SELECT * FROM [TABLE] WHERE ReasonID=rid AND TypeID=tid;"),
        GET_REASON_BY_TYPE("PunishmentReason_GetByType", "tid INT", "SELECT * FROM [TABLE] WHERE TypeID=tid;"),
        SET_REASON("PunishmentReason_SetReason", "rid INT, tid INT, rea TEXT, modified INT",
                "UPDATE [TABLE] SET Reason=rea, ModifiedBy=modified WHERE ReasonID=rid AND TypeID=tid;"),
        SET_DURATION("PunishmentReason_SetDuration", "rid INT, tid INT, dur BIGINT, modified INT",
                "UPDATE [TABLE] SET Duration=dur, ModifiedBy=modified WHERE ReasonID=rid AND TypeID=tid;"),
        SET_AUTO_FLAG_IP("PunishmentReason_SetAutoFlagIP", "rid INT, tid INT, flag BOOL, modified INT",
                "UPDATE [TABLE] SET AutoFlagIP=flag, ModifiedBy=modified WHERE ReasonID=rid AND TypeID=tid;"),
        SET_AUTO_PUNISH("PunishmentReason_SetAutoPunish", "rid INT, tid INT, punish BOOL, modified INT",
                "UPDATE [TABLE] SET AutoPunish=punish, ModifiedBy=modified WHERE ReasonID=rid AND TypeID=tid;");
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
