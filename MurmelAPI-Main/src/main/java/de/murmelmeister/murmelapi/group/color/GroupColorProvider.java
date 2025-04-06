package de.murmelmeister.murmelapi.group.color;

import de.murmelmeister.murmelapi.database.Database;

import java.sql.Timestamp;

import static de.murmelmeister.murmelapi.MurmelAPI.getDateFormat;

public final class GroupColorProvider implements GroupColor {
    private static final String TABLE_NAME = "GroupColorSettings";
    private final Database database;

    public GroupColorProvider(Database database) {
        this.database = database;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "GroupID INT PRIMARY KEY, FOREIGN KEY (GroupID) REFERENCES Groups(ID), " +
                                         "ChatPrefix VARCHAR(300), ChatSuffix VARCHAR(300), ChatColor VARCHAR(30), " +
                                         "TabPrefix VARCHAR(300), TabSuffix VARCHAR(300), TabColor VARCHAR(30), " +
                                         "TeamPrefix VARCHAR(300), TeamSuffix VARCHAR(300), TeamColor VARCHAR(30), " +
                                         "CreatedBy INT, FOREIGN KEY (CreatedBy) REFERENCES Users(ID), " +
                                         "CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP(), " +
                                         "ModifiedBy INT, FOREIGN KEY (ModifiedBy) REFERENCES Users(ID), " +
                                         "ModifiedAt DATETIME DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP()");
        Procedure.loadAll(database);
    }

    @Override
    public boolean existsGroup(int groupId) {
        return groupId > 0 && database.existsCallable(Procedure.GET_ALL_BY_ID.getName(), groupId);
    }

    @Override
    public void createGroup(int executorId, int groupId) {
        createGroup(executorId, groupId, "", "", "", "", "", "", "", "", "7");
    }

    @Override
    public void createGroup(int executorId, int groupId, String chatPrefix, String chatSuffix, String chatColor, String tabPrefix, String tabSuffix, String tabColor, String teamPrefix, String teamSuffix, String teamColor) {
        database.updateCallable(Procedure.CREATE.getName(),
                groupId, chatPrefix, chatSuffix, chatColor,
                tabPrefix, tabSuffix, tabColor,
                teamPrefix, teamSuffix, teamColor, executorId, executorId);
    }

    @Override
    public void deleteGroup(int groupId) {
        database.updateCallable(Procedure.DELETE.getName(), groupId);
    }

    @Override
    public String getPrefix(int groupId, GroupColorType type) {
        return database.queryCallable(Procedure.GET_ALL_BY_ID.getName(), null, resultSet -> resultSet.getString(type.getName() + "Prefix"), groupId);
    }

    @Override
    public String getSuffix(int groupId, GroupColorType type) {
        return database.queryCallable(Procedure.GET_ALL_BY_ID.getName(), null, resultSet -> resultSet.getString(type.getName() + "Suffix"), groupId);
    }

    @Override
    public String getColor(int groupId, GroupColorType type) {
        return database.queryCallable(Procedure.GET_ALL_BY_ID.getName(), null, resultSet -> resultSet.getString(type.getName() + "Color"), groupId);
    }

    @Override
    public void setPrefix(int executorId, int groupId, GroupColorType type, String prefix) {
        database.updateCallable(Procedure.UPDATE.getName(), type.getName() + "Prefix", prefix, groupId, executorId);
    }

    @Override
    public void setSuffix(int executorId, int groupId, GroupColorType type, String suffix) {
        database.updateCallable(Procedure.UPDATE.getName(), type.getName() + "Suffix", suffix, groupId, executorId);
    }

    @Override
    public void setColor(int executorId, int groupId, GroupColorType type, String color) {
        database.updateCallable(Procedure.UPDATE.getName(), type.getName() + "Color", color, groupId, executorId);
    }

    @Override
    public int getCreatedBy(int groupId) {
        return database.queryCallable(Procedure.GET_ALL_BY_ID.getName(), -2, resultSet -> resultSet.getInt("CreatedBy"), groupId);
    }

    @Override
    public Timestamp getCreatedAt(int groupId) {
        return database.queryCallable(Procedure.GET_ALL_BY_ID.getName(), null, resultSet -> resultSet.getTimestamp("CreatedAt"), groupId);
    }

    @Override
    public String getCreatedDate(int groupId) {
        Timestamp time = getCreatedAt(groupId);
        return time == null ? "never" : getDateFormat().format(time);
    }

    @Override
    public int getModifiedBy(int groupId) {
        return database.queryCallable(Procedure.GET_ALL_BY_ID.getName(), -2, resultSet -> resultSet.getInt("ModifiedBy"), groupId);
    }

    @Override
    public Timestamp getModifiedAt(int groupId) {
        return database.queryCallable(Procedure.GET_ALL_BY_ID.getName(), null, resultSet -> resultSet.getTimestamp("ModifiedAt"), groupId);
    }

    @Override
    public String getModifiedDate(int groupId) {
        Timestamp time = getModifiedAt(groupId);
        return time == null ? "never" : getDateFormat().format(time);
    }

    private enum Procedure {
        GET_ALL_BY_ID("GroupColor_GetAllByID", "gid INT", "SELECT * FROM [TABLE] WHERE GroupID=gid;"),
        CREATE("GroupColor_Create", "gid INT, cp VARCHAR(300), cs VARCHAR(300), cc VARCHAR(30), tp VARCHAR(300), ts VARCHAR(300), tc VARCHAR(30), tep VARCHAR(300), tes VARCHAR(300), tec VARCHAR(30), " +
                                    "created INT, modified INT",
                "INSERT INTO [TABLE] (GroupID,ChatPrefix,ChatSuffix,ChatColor,TabPrefix,TabSuffix,TabColor,TeamPrefix,TeamSuffix,TeamColor,CreatedBy,ModifiedBy) " +
                "VALUES (gid,cp,cs,cc,tp,ts,tc,tep,tes,tec,created,modified);"),
        DELETE("GroupColor_Delete", "gid INT", "DELETE FROM [TABLE] WHERE GroupID=gid;"),
        UPDATE("GroupColor_Update", "columnName VARCHAR(300), columnValue VARCHAR(300), gid INT, modified INT", """
                IF columnName IN ('ChatPrefix', 'ChatSuffix', 'ChatColor',
                                  'TabPrefix', 'TabSuffix', 'TabColor',
                                  'TeamPrefix', 'TeamSuffix', 'TeamColor') THEN
                    SET @sql = CONCAT('UPDATE [TABLE] SET ', columnName, '="', columnValue, '", ModifiedBy=', modified, ' WHERE GroupID=', gid, ';');
                    PREPARE stmt FROM @sql;
                    EXECUTE stmt;
                    DEALLOCATE PREPARE stmt;
                ELSE
                    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid column name';
                END IF;
                """);
        private static final Procedure[] VALUES = values();

        private final String name;
        private final String query;

        Procedure(final String name, final String input, final String query) {
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
            for (Procedure procedure : VALUES) database.update(procedure.getQuery());
        }
    }
}
