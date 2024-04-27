package de.murmelmeister.murmelapi.group.settings;

import de.murmelmeister.murmelapi.utils.Database;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static de.murmelmeister.murmelapi.utils.StringUtil.checkArgumentSQL;

public final class GroupSettingsProvider implements GroupSettings {
    private static final String TABLE_NAME = "GroupSettings";

    public GroupSettingsProvider() throws SQLException {
        this.createTable();
        Procedure.loadAll();
    }

    private void createTable() throws SQLException {
        Database.update("CREATE TABLE IF NOT EXISTS %s (GroupID INT PRIMARY KEY, CreatorID INT, CreatedTime BIGINT(255), SortID INT)", TABLE_NAME);
    }

    @Override
    public boolean existsGroup(int groupId) throws SQLException {
        return Database.exists("CALL %s('%s')", Procedure.PROCEDURE_ID.getName(), checkArgumentSQL(groupId));
    }

    @Override
    public void createGroup(int groupId, int creatorId, int sortId) throws SQLException {
        if (existsGroup(groupId)) return;
        Database.update("CALL %s('%s','%s','%s','%s')", Procedure.PROCEDURE_INSERT.getName(), groupId, creatorId, System.currentTimeMillis(), sortId);
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
    public void setSortId(int groupId, int sortId) throws SQLException {
        Database.update("CALL %s('%s','%s')", Procedure.PROCEDURE_UPDATE_SORT_ID.getName(), checkArgumentSQL(groupId), checkArgumentSQL(sortId));
    }

    @Override
    public List<Integer> getSortIds() throws SQLException {
        return Database.getIntList(new ArrayList<>(), "SortID", "CALL %s()", Procedure.PROCEDURE_ALL.getName()).stream().sorted().toList();
    }

    private enum Procedure {
        PROCEDURE_ID("GroupSettings_ID", Database.getProcedureQuery("GroupSettings_ID", "gid INT", "SELECT * FROM %s WHERE GroupID=gid;", TABLE_NAME)),
        PROCEDURE_INSERT("GroupSettings_Insert", Database.getProcedureQuery("GroupSettings_Insert", "gid INT, creator VARCHAR(36), time BIGINT(255), sort INT", "INSERT INTO %s VALUES (gid, creator, time, sort);", TABLE_NAME)),
        PROCEDURE_DELETE("GroupSettings_Delete", Database.getProcedureQuery("GroupSettings_Delete", "gid INT", "DELETE FROM %s WHERE GroupID=gid;", TABLE_NAME)),
        PROCEDURE_UPDATE_SORT_ID("GroupSettings_Update_SortID", Database.getProcedureQuery("GroupSettings_Update_SortID", "gid INT, sort INT", "UPDATE %s SET SortID=sort WHERE GroupID=gid;", TABLE_NAME)),
        PROCEDURE_ALL("GroupSettings_All", Database.getProcedureQuery("GroupSettings_All", "", "SELECT * FROM %s;", TABLE_NAME));
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
