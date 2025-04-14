package de.murmelmeister.murmelapi.punishment;

import de.murmelmeister.murmelapi.database.Database;
import de.murmelmeister.murmelapi.punishment.log.PunishmentLog;

import java.util.UUID;

/**
 * The {@code PunishmentUserProvider} class implements the {@code PunishmentUser} interface,
 * providing methods to manage user punishments in a database.
 * It allows checking for user existence, punishing users, unpunishing users, and retrieving punishment logs.
 */
public final class PunishmentUserProvider implements PunishmentUser {
    private static final String TABLE_NAME = "punishment_user";

    private final Database database;
    private final PunishmentLog punishmentLog;

    public PunishmentUserProvider(Database database, PunishmentLog punishmentLog) {
        this.database = database;
        this.punishmentLog = punishmentLog;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "userId INT PRIMARY KEY, " +
                                         "logId VARCHAR(36) UNIQUE, " +
                                         "FOREIGN KEY (userId) REFERENCES users(id), " +
                                         "FOREIGN KEY (logId) REFERENCES punishment_logs(id)");
        Procedure.loadAll(database);
    }

    @Override
    public boolean exists(int userId) {
        return userId > 0 && database.existsCallable(Procedure.GET_DATA.getName(), userId);
    }

    @Override
    public int punish(int userId, String ipAddress, int reasonId, int createdBy) {
        if (userId < 1 || createdBy == -2) return 0;
        UUID logId = punishmentLog.addLogUser(reasonId, userId, ipAddress, createdBy);
        return database.updateCallable(Procedure.CREATE.getName(), userId, logId.toString());
    }

    @Override
    public int unpunish(int userId) {
        if (userId < 1) return 0;
        return database.updateCallable(Procedure.DELETE.getName(), userId);
    }

    @Override
    public UUID getLogId(int userId) {
        if (userId < 1) return null;
        return database.queryCallable(Procedure.GET_DATA.getName(), null, resultSet -> UUID.fromString(resultSet.getString("logId")), userId);
    }

    @Override
    public boolean isPunished(int userId) {
        if (userId < 1) return false;
        UUID logId = getLogId(userId);
        return logId != null && !punishmentLog.isExpired(logId);
    }

    private enum Procedure {
        CREATE("punishmentUser_create", "p_userId INT, p_logId VARCHAR(36)", "INSERT INTO [TABLE] VALUES (p_userId, p_logId);"),
        DELETE("punishmentUser_delete", "p_userId INT", "DELETE FROM [TABLE] WHERE userId=p_userId;"),
        GET_DATA("punishmentUser_getData", "p_userId INT", "SELECT * FROM [TABLE] WHERE userId=p_userId;");
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
