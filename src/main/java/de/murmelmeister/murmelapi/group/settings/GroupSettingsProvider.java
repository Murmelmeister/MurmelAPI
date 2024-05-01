package de.murmelmeister.murmelapi.group.settings;

import de.murmelmeister.murmelapi.utils.Database;

import java.sql.SQLException;
import java.text.SimpleDateFormat;

import static de.murmelmeister.murmelapi.utils.StringUtil.checkArgumentSQL;

public final class GroupSettingsProvider implements GroupSettings {
    private static final String TABLE_NAME = "GroupSettings";

    public GroupSettingsProvider() throws SQLException {
        this.createTable();
        Procedure.loadAll();
    }

    private void createTable() throws SQLException {
        Database.update("CREATE TABLE IF NOT EXISTS %s (GroupID INT PRIMARY KEY, CreatorID INT, CreatedTime BIGINT(255), SortID INT, TeamID VARCHAR(100))", TABLE_NAME);
    }

    @Override
    public boolean existsGroup(int groupId) throws SQLException {
        return Database.exists("CALL %s('%s')", Procedure.PROCEDURE_ID.getName(), checkArgumentSQL(groupId));
    }

    @Override
    public void createGroup(int groupId, int creatorId, int sortId, String teamId) throws SQLException {
        if (existsGroup(groupId)) return;
        Database.update("CALL %s('%s','%s','%s','%s','%s')", Procedure.PROCEDURE_INSERT.getName(), groupId, creatorId, System.currentTimeMillis(), sortId, teamId);
    }

    @Override
    public void deleteGroup(int groupId) throws SQLException {
        Database.update("CALL %s('%s')", Procedure.PROCEDURE_DELETE.getName(), checkArgumentSQL(groupId));
    }

    @Override
    public int getCreatorId(int groupId) throws SQLException {
        return Database.getInt(-2, "CreatorID", "CALL %s('%s')", Procedure.PROCEDURE_ID.getName(), checkArgumentSQL(groupId));
    }

    @Override
    public long getCreatedTime(int groupId) throws SQLException {
        return Database.getLong(-1, "CreatedTime", "CALL %s('%s')", Procedure.PROCEDURE_ID.getName(), checkArgumentSQL(groupId));
    }

    @Override
    public String getCreatedDate(int groupId) throws SQLException {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(getCreatedTime(groupId));
    }

    @Override
    public int getSortId(int groupId) throws SQLException {
        return Database.getInt(-1, "SortID", "CALL %s('%s')", Procedure.PROCEDURE_ID.getName(), checkArgumentSQL(groupId));
    }

    @Override
    public String getTeamId(int groupId) throws SQLException {
        return Database.getString(null, "TeamID", "CALL %s('%s')", Procedure.PROCEDURE_ID.getName(), checkArgumentSQL(groupId));
    }

    private enum Procedure {
        PROCEDURE_ID("GroupSettings_ID", Database.getProcedureQuery("GroupSettings_ID", "gid INT", "SELECT * FROM %s WHERE GroupID=gid;", TABLE_NAME)),
        PROCEDURE_INSERT("GroupSettings_Insert", Database.getProcedureQuery("GroupSettings_Insert", "gid INT, creator VARCHAR(36), time BIGINT(255), sort INT, team VARCHAR(100)", "INSERT INTO %s VALUES (gid, creator, time, sort, team);", TABLE_NAME)),
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
