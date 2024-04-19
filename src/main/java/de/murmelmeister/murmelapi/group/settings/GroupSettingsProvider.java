package de.murmelmeister.murmelapi.group.settings;

import de.murmelmeister.murmelapi.utils.Database;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.UUID;

import static de.murmelmeister.murmelapi.utils.StringUtil.checkArgumentSQL;

public final class GroupSettingsProvider implements GroupSettings {
    private static final String TABLE_NAME = "GroupSettings";

    public GroupSettingsProvider() throws SQLException {
        this.createTable();
        Procedure.loadAll();
    }

    private void createTable() throws SQLException {
        Database.update("CREATE TABLE IF NOT EXISTS %s (GroupID INT PRIMARY KEY, FOREIGN KEY (GroupID) REFERENCES Groups(GroupID), CreatorID VARCHAR(36), CreatedTime BIGINT(255))", TABLE_NAME);
    }

    @Override
    public boolean existsGroup(int id) throws SQLException {
        return Database.exists("CALL %s('%s')", Procedure.PROCEDURE_ID.getName(), checkArgumentSQL(id));
    }

    @Override
    public void createGroup(int id, UUID creator) throws SQLException {
        if (existsGroup(id)) return;
        Database.update("CALL %s('%s','%s','%s')", Procedure.PROCEDURE_INSERT.getName(), id, creator, System.currentTimeMillis());
    }

    @Override
    public void deleteGroup(int id) throws SQLException {
        Database.update("CALL %s('%s')", Procedure.PROCEDURE_DELETE.getName(), checkArgumentSQL(id));
    }

    @Override
    public UUID getCreatorId(int id) throws SQLException {
        return Database.getUniqueId(null, "CreatorID", "CALL %s('%s')", Procedure.PROCEDURE_ID.getName(), checkArgumentSQL(id));
    }

    @Override
    public long getCreatedTime(int id) throws SQLException {
        return Database.getLong(-1, "CreatedTime", "CALL %s('%s')", Procedure.PROCEDURE_ID.getName(), checkArgumentSQL(id));
    }

    @Override
    public String getCreatedDate(int id) throws SQLException {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(getCreatedTime(checkArgumentSQL(id)));
    }

    private enum Procedure {
        PROCEDURE_ID("GroupSettings_ID", Database.getProcedureQuery("GroupSettings_ID", "gid INT", "SELECT * FROM %s WHERE GroupID=gid;", TABLE_NAME)),
        PROCEDURE_INSERT("GroupSettings_Insert", Database.getProcedureQuery("GroupSettings_Insert", "gid INT, creator VARCHAR(36), time BIGINT(255)", "INSERT INTO %s VALUES (gid, creator, time);", TABLE_NAME)),
        PROCEDURE_DELETE("GroupSettings_Delete", Database.getProcedureQuery("GroupSettings_Delete", "gid INT", "DELETE FROM %s WHERE GroupID=gid;", TABLE_NAME));
        private static final Procedure[] VALUES = values();

        private final String name;
        private final String query;

        Procedure(String name, String query) {
            this.name = name;
            this.query = query;
        }

        public String getName() {
            return name;
        }

        public String getQuery() {
            return query;
        }

        public static void loadAll() throws SQLException {
            for (Procedure procedure : VALUES)
                Database.update(procedure.getQuery());
        }
    }
}
