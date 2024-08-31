package de.murmelmeister.murmelapi.bansystem.reason;

import de.murmelmeister.murmelapi.utils.Database;

public final class ReasonProvider implements Reason {
    public ReasonProvider(String tableName) {
        createTable(tableName);
        Procedure.loadAll(tableName);
    }

    private void createTable(String tableName) {
        Database.createTable("ReasonID INT PRIMARY KEY AUTO_INCREMENT, Reason VARCHAR(1000)", tableName);
    }

    @Override
    public boolean exists(int id) {
        return Database.existsCall(Procedure.REASON_GET.getName(), id);
    }

    @Override
    public void add(String reason) {
        Database.updateCall(Procedure.REASON_INSERT.getName(), reason);
    }

    @Override
    public void remove(int id) {
        Database.updateCall(Procedure.REASON_DELETE.getName(), id);
    }

    @Override
    public void update(int id, String reason) {
        Database.updateCall(Procedure.REASON_UPDATE.getName(), id, reason);
    }

    @Override
    public String get(int id) {
        return Database.getStringCall(null, "Reason", Procedure.REASON_GET.getName(), id);
    }

    private enum Procedure {
        REASON_GET("Reason_Get", "rid INT", "SELECT * FROM [TABLE] WHERE ReasonID=rid;"),
        REASON_INSERT("Reason_Insert", "message VARCHAR(1000)", "INSERT INTO [TABLE] (Reason) VALUES (message);"),
        REASON_DELETE("Reason_Delete", "rid INT", "DELETE FROM [TABLE] WHERE ReasonID=rid;"),
        REASON_UPDATE("Reason_Update", "rid INT, message VARCHAR(1000)", "UPDATE [TABLE] SET Reason=message WHERE ReasonID=rid;");
        private static final Procedure[] VALUES = values();

        private final String name;
        private final String query;

        Procedure(String name, String input, String query) {
            this.name = name;
            this.query = Database.getProcedureQueryWithoutObjects(name, input, query);
        }

        public String getName() {
            return name;
        }

        public String getQuery(String tableName) {
            return query.replace("[TABLE]", tableName);
        }

        public static void loadAll(String tableName) {
            for (Procedure procedure : VALUES)
                Database.update(procedure.getQuery(tableName));
        }
    }
}
