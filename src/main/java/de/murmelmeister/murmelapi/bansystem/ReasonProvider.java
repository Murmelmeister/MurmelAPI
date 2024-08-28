package de.murmelmeister.murmelapi.bansystem;

import de.murmelmeister.murmelapi.utils.Database;

public final class ReasonProvider implements Reason {
    private final String tableName;

    public ReasonProvider(String tableName) {
        this.tableName = tableName;
        createTable();
        Procedure.loadAll(tableName);
    }

    private void createTable() {
        Database.update("CREATE TABLE IF NOT EXISTS %s (ReasonID INT PRIMARY KEY AUTO_INCREMENT, Reason VARCHAR(1000))", tableName);
    }

    @Override
    public boolean exists(int id) {
        return Database.existsCall(Procedure.PROCEDURE_GET.getName(), id);
    }

    @Override
    public void add(String reason) {
        Database.updateCall(Procedure.PROCEDURE_INSERT.getName(), reason);
    }

    @Override
    public void remove(int id) {
        Database.updateCall(Procedure.PROCEDURE_DELETE.getName(), id);
    }

    @Override
    public void update(int id, String reason) {
        Database.updateCall(Procedure.PROCEDURE_UPDATE.getName(), id, reason);
    }

    @Override
    public String get(int id) {
        return Database.getStringCall(null, "Reason", Procedure.PROCEDURE_GET.getName(), id);
    }

    private enum Procedure {
        PROCEDURE_GET("Reason_Get", "rid INT", "SELECT * FROM %s WHERE ReasonID=rid;"),
        PROCEDURE_INSERT("Reason_Insert", "message VARCHAR(1000)", "INSERT INTO %s (Reason) VALUES (message);"),
        PROCEDURE_DELETE("Reason_Delete", "rid INT", "DELETE FROM %s WHERE ReasonID=rid;"),
        PROCEDURE_UPDATE("Reason_Update", "rid INT, message VARCHAR(1000)", "UPDATE %s SET Reason=message WHERE ReasonID=rid;");
        private static final Procedure[] VALUES = values();

        private final String name;
        private final String query;

        Procedure(String name, String input, String query, Object... objects) {
            this.name = name;
            this.query = Database.getProcedureQuery(name, input, query, objects);
        }

        public String getName() {
            return name;
        }

        public String getQuery(String tableName) {
            return String.format(query, tableName);
        }

        public static void loadAll(String tableName) {
            for (Procedure procedure : VALUES)
                Database.update(procedure.getQuery(tableName));
        }
    }
}
