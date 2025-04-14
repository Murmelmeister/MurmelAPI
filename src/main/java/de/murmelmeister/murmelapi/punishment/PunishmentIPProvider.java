package de.murmelmeister.murmelapi.punishment;

import de.murmelmeister.murmelapi.database.Database;
import de.murmelmeister.murmelapi.punishment.log.PunishmentLog;

import java.util.UUID;

/**
 * PunishmentIPProvider class to manage IP punishments in the database.
 * This class implements the PunishmentIP interface and provides methods to interact with IP punishment data.
 */
public final class PunishmentIPProvider implements PunishmentIP {
    private static final String TABLE_NAME = "punishment_ip";

    private final Database database;
    private final PunishmentLog punishmentLog;

    public PunishmentIPProvider(Database database, PunishmentLog punishmentLog) {
        this.database = database;
        this.punishmentLog = punishmentLog;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "ipAddress VARCHAR(45) PRIMARY KEY, " +
                                         "logId VARCHAR(36) UNIQUE, " +
                                         "FOREIGN KEY (logId) REFERENCES punishment_logs(id)");
        Procedure.loadAll(database);
    }

    @Override
    public boolean exists(String ipAddress) {
        return ipAddress != null && database.existsCallable(Procedure.GET_DATA.getName(), ipAddress);
    }

    @Override
    public int punish(String ipAddress, int reasonId, int createdBy) {
        if (ipAddress == null || createdBy == -2) return 0;
        UUID logId = punishmentLog.addLogIp(reasonId, ipAddress, createdBy);
        return database.updateCallable(Procedure.CREATE.getName(), ipAddress, logId.toString());
    }

    @Override
    public int unpunish(String ipAddress) {
        if (ipAddress == null) return 0;
        return database.updateCallable(Procedure.DELETE.getName(), ipAddress);
    }

    @Override
    public UUID getLogId(String ipAddress) {
        if (ipAddress == null) return null;
        return database.queryCallable(Procedure.GET_DATA.getName(), null, result -> UUID.fromString(result.getString("logId")), ipAddress);
    }

    @Override
    public boolean isPunished(String ipAddress) {
        if (ipAddress == null) return false;
        UUID logId = getLogId(ipAddress);
        return logId != null && !punishmentLog.isExpired(logId);
    }

    private enum Procedure {
        CREATE("punishmentIp_create", "p_ipAddress VARCHAR(45), p_logId VARCHAR(36)", "INSERT INTO [TABLE] VALUES (p_ipAddress, p_logId);"),
        DELETE("punishmentIp_delete", "p_ipAddress VARCHAR(45)", "DELETE FROM [TABLE] WHERE ipAddress=p_ipAddress;"),
        GET_DATA("punishmentIp_getData", "p_ipAddress VARCHAR(45)", "SELECT * FROM [TABLE] WHERE ipAddress=p_ipAddress;");
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

        private static void loadAll(Database database) {
            for (Procedure procedure : VALUES)
                database.update(procedure.getQuery());
        }
    }
}
