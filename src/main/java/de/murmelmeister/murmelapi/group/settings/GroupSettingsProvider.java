package de.murmelmeister.murmelapi.group.settings;

import de.murmelmeister.murmelapi.utils.Database;

import java.text.SimpleDateFormat;

public final class GroupSettingsProvider implements GroupSettings {
    public GroupSettingsProvider() {
        String tableName = "GroupSettings";
        createTable(tableName);
        Procedure.loadAll(tableName);
    }

    private void createTable(String tableName) {
        Database.createTable("GroupID INT PRIMARY KEY, CreatorID INT, CreatedTime BIGINT(255), SortID INT, TeamID VARCHAR(100)", tableName);
    }

    @Override
    public boolean existsGroup(int groupId) {
        return Database.existsCall(Procedure.GROUP_SETTINGS_ID.getName(), groupId);
    }

    @Override
    public void createGroup(int groupId, int creatorId, int sortId, String teamId) {
        if (existsGroup(groupId)) return;
        Database.updateCall(Procedure.GROUP_SETTINGS_INSERT.getName(), groupId, creatorId, System.currentTimeMillis(), sortId, teamId);
    }

    @Override
    public void deleteGroup(int groupId) {
        Database.updateCall(Procedure.GROUP_SETTINGS_DELETE.getName(), groupId);
    }

    @Override
    public int getCreatorId(int groupId) {
        return Database.getIntCall(-2, "CreatorID", Procedure.GROUP_SETTINGS_ID.getName(), groupId);
    }

    @Override
    public long getCreatedTime(int groupId) {
        return Database.getLongCall(-1, "CreatedTime", Procedure.GROUP_SETTINGS_ID.getName(), groupId);
    }

    @Override
    public String getCreatedDate(int groupId) {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(getCreatedTime(groupId));
    }

    @Override
    public int getSortId(int groupId) {
        return Database.getIntCall(-1, "SortID", Procedure.GROUP_SETTINGS_ID.getName(), groupId);
    }

    @Override
    public void setSortId(int groupId, int sortId) {
        Database.updateCall(Procedure.GROUP_SETTINGS_UPDATE_SORT.getName(), groupId, sortId);
    }

    @Override
    public String getTeamId(int groupId) {
        return Database.getStringCall(null, "TeamID", Procedure.GROUP_SETTINGS_ID.getName(), groupId);
    }

    @Override
    public void setTeamId(int groupId, String teamId) {
        Database.updateCall(Procedure.GROUP_SETTINGS_UPDATE_TEAM.getName(), groupId, teamId);
    }

    private enum Procedure {
        GROUP_SETTINGS_ID("GroupSettings_ID", "gid INT", "SELECT * FROM [TABLE] WHERE GroupID=gid;"),
        GROUP_SETTINGS_INSERT("GroupSettings_Insert", "gid INT, creator VARCHAR(36), time BIGINT(255), sort INT, team VARCHAR(100)", "INSERT INTO [TABLE] VALUES (gid, creator, time, sort, team);"),
        GROUP_SETTINGS_DELETE("GroupSettings_Delete", "gid INT", "DELETE FROM [TABLE] WHERE GroupID=gid;"),
        GROUP_SETTINGS_UPDATE_SORT("GroupSettings_Update_SortID", "gid INT, sort INT", "UPDATE [TABLE] SET SortID=sort WHERE GroupID=gid;"),
        GROUP_SETTINGS_UPDATE_TEAM("GroupSettings_Update_TeamID", "gid INT, team VARCHAR(100)", "UPDATE [TABLE] SET TeamID=team WHERE GroupID=gid;");
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
