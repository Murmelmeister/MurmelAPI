package de.murmelmeister.murmelapi.punishment.reason;

import de.murmelmeister.murmelapi.database.Database;

import java.sql.Timestamp;
import java.util.List;

import static de.murmelmeister.murmelapi.MurmelAPI.getDateFormat;

/**
 * The PunishmentReasonProvider class implements the PunishmentReason interface and manages
 * punishment reason records in the database. It provides methods for checking the existence of a
 * punishment reason, adding and removing punishment reasons, retrieving and updating punishment reason details,
 * as well as obtaining metadata such as duration, auto-flag settings, and creation/update information.
 */
public final class PunishmentReasonProvider implements PunishmentReason {
    private static final String TABLE_NAME = "punishment_reasons";

    private final Database database;

    public PunishmentReasonProvider(Database database) {
        this.database = database;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "id INT PRIMARY KEY, " +
                                         "typeId INT, FOREIGN KEY (typeId) REFERENCES punishment_types(id), " +
                                         "reason TEXT, " +
                                         "duration BIGINT, " +
                                         "autoFlagIp BOOL, " +
                                         "autoPunish BOOL, " +
                                         "createdBy INT, FOREIGN KEY (createdBy) REFERENCES users(id), " +
                                         "createdAt DATETIME DEFAULT CURRENT_TIMESTAMP(), " +
                                         "updatedBy INT, FOREIGN KEY (updatedBy) REFERENCES users(id), " +
                                         "updatedAt DATETIME DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP()");
        Procedure.loadAll(database);
    }

    @Override
    public boolean existsReason(int id) {
        return database.existsCallable(Procedure.GET_DATA_BY_ID.getName(), id);
    }

    @Override
    public int addReason(int id, int typeId, String reason, long duration, boolean autoFlagIp, boolean autoPunish, int createdBy) {
        if (typeId < 1 || reason == null || createdBy == -2) return 0;
        return database.updateCallable(Procedure.CREATE.getName(), id, typeId, reason, duration, autoFlagIp, autoPunish, createdBy, createdBy);
    }

    @Override
    public int removeReason(int id) {
        return database.updateCallable(Procedure.DELETE.getName(), id);
    }

    @Override
    public List<Integer> getReasonIds() {
        return database.queryListCallable(Procedure.GET_DATA.getName(), resultSet -> resultSet.getInt("id"));
    }

    @Override
    public String getReason(int id) {
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), null, resultSet -> resultSet.getString("reason"), id);
    }

    @Override
    public int setReason(int id, String reason, int updatedBy) {
        if (reason == null || updatedBy == -2) return 0;
        return database.updateCallable(Procedure.UPDATE_REASON.getName(), id, reason, updatedBy);
    }

    @Override
    public long getDuration(int id) {
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), -1L, resultSet -> resultSet.getLong("duration"), id);
    }

    @Override
    public int setDuration(int id, long duration, int updatedBy) {
        if (updatedBy == -2) return 0;
        return database.updateCallable(Procedure.UPDATE_DURATION.getName(), id, duration, updatedBy);
    }

    @Override
    public boolean getAutoFlagIp(int id) {
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), false, resultSet -> resultSet.getBoolean("autoFlagIp"), id);
    }

    @Override
    public int setAutoFlagIp(int id, boolean autoFlagIp, int updatedBy) {
        if (updatedBy == -2) return 0;
        return database.updateCallable(Procedure.UPDATE_AUTO_FLAG_IP.getName(), id, autoFlagIp, updatedBy);
    }

    @Override
    public boolean getAutoPunish(int id) {
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), false, resultSet -> resultSet.getBoolean("autoPunish"), id);
    }

    @Override
    public int setAutoPunish(int id, boolean autoPunish, int updatedBy) {
        if (updatedBy == -2) return 0;
        return database.updateCallable(Procedure.UPDATE_AUTO_PUNISH.getName(), id, autoPunish, updatedBy);
    }

    @Override
    public int getCreatedBy(int id) {
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), -2, resultSet -> resultSet.getInt("createdBy"), id);
    }

    @Override
    public Timestamp getCreatedAt(int id) {
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), null, resultSet -> resultSet.getTimestamp("createdAt"), id);
    }

    @Override
    public String getCreatedDate(int id) {
        Timestamp createdAt = getCreatedAt(id);
        return createdAt == null ? null : getDateFormat().format(createdAt);
    }

    @Override
    public int getUpdatedBy(int id) {
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), -2, resultSet -> resultSet.getInt("updatedBy"), id);
    }

    @Override
    public Timestamp getUpdatedAt(int id) {
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), null, resultSet -> resultSet.getTimestamp("updatedAt"), id);
    }

    @Override
    public String getUpdatedDate(int id) {
        Timestamp updatedAt = getUpdatedAt(id);
        return updatedAt == null ? null : getDateFormat().format(updatedAt);
    }

    private enum Procedure {
        CREATE("punishmentReason_create", "p_id INT, p_typeId INT, p_reason TEXT, p_duration BIGINT, " +
                                          "p_autoFlagIp BOOL, p_autoPunish BOOL, p_createdBy INT, p_updatedBy INT",
                "INSERT INTO [TABLE] (id, typeId, reason, duration, autoFlagIp, autoPunish, createdBy, updatedBy) " +
                "VALUES (p_id, p_typeId, p_reason, p_duration, p_autoFlagIp, p_autoPunish, p_createdBy, p_updatedBy);"),
        DELETE("punishmentReason_delete", "p_id INT", "DELETE FROM [TABLE] WHERE id=p_id;"),
        GET_DATA_BY_ID("punishmentReason_getDataById", "p_id INT", "SELECT * FROM [TABLE] WHERE id=p_id;"),
        GET_DATA("punishmentReason_getData", "", "SELECT * FROM [TABLE];"),
        UPDATE_REASON("punishmentReason_updateReason", "p_id INT, p_reason TEXT, p_updatedBy INT",
                "UPDATE [TABLE] SET reason=p_reason, updatedBy=p_updatedBy WHERE id=p_id;"),
        UPDATE_DURATION("punishmentReason_updateDuration", "p_id INT, p_duration BIGINT, p_updatedBy INT",
                "UPDATE [TABLE] SET duration=p_duration, updatedBy=p_updatedBy WHERE id=p_id;"),
        UPDATE_AUTO_FLAG_IP("punishmentReason_updateAutoFlagIp", "p_id INT, p_autoFlagIp BOOL, p_updatedBy INT",
                "UPDATE [TABLE] SET autoFlagIp=p_autoFlagIp, updatedBy=p_updatedBy WHERE id=p_id;"),
        UPDATE_AUTO_PUNISH("punishmentReason_updateAutoPunish", "p_id INT, p_autoPunish BOOL, p_updatedBy INT",
                "UPDATE [TABLE] SET autoPunish=p_autoPunish, updatedBy=p_updatedBy WHERE id=p_id;");
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
