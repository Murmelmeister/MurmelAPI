package de.murmelmeister.murmelapi.bansystem;

import de.murmelmeister.murmelapi.utils.Database;

public final class ReasonProvider implements Reason {
    private static final String TABLE_NAME = "Reason";

    public ReasonProvider() {
        createTable();
        Procedure.loadAll();
    }

    private void createTable() {
        Database.update("CREATE TABLE IF NOT EXISTS %s (ReasonID INT PRIMARY KEY AUTO_INCREMENT, Reason VARCHAR(1000))", TABLE_NAME);
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
        PROCEDURE_GET("Reason_Get", "rid INT", "SELECT * FROM %s WHERE ReasonID=rid;", TABLE_NAME),
        PROCEDURE_INSERT("Reason_Insert", "message VARCHAR(1000)", "INSERT INTO %s (Reason) VALUES (message);", TABLE_NAME),
        PROCEDURE_DELETE("Reason_Delete", "rid INT", "DELETE FROM %s WHERE ReasonID=rid;", TABLE_NAME),
        PROCEDURE_UPDATE("Reason_Update", "rid INT, message VARCHAR(1000)", "UPDATE %s SET Reason=message WHERE ReasonID=rid;", TABLE_NAME);
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

        public String getQuery() {
            return query;
        }

        public static void loadAll() {
            for (Procedure procedure : VALUES)
                Database.update(procedure.getQuery());
        }
    }
}
