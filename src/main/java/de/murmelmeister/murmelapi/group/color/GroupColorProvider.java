package de.murmelmeister.murmelapi.group.color;

import de.murmelmeister.murmelapi.database.Database;

import java.sql.Timestamp;

import static de.murmelmeister.murmelapi.MurmelAPI.getDateFormat;

/**
 * The GroupColorProvider class provides methods to manage group colors in the database.
 * It allows creating, deleting, and updating group colors, as well as retrieving their properties.
 */
public final class GroupColorProvider implements GroupColor {
    private static final String TABLE_NAME = "group_color";

    private final Database database;

    public GroupColorProvider(Database database) {
        this.database = database;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "groupId INT PRIMARY KEY, " +
                                         "chatPrefix VARCHAR(300), chatSuffix VARCHAR(300), chatColor(30), " +
                                         "tabPrefix VARCHAR(300), tabSuffix VARCHAR(300), tabColor VARCHAR(30), " +
                                         "teamPrefix VARCHAR(300), teamSuffix VARCHAR(300), teamColor VARCHAR(30), " +
                                         "createdBy INT, FOREIGN KEY (createdBy) REFERENCES users(id), " +
                                         "createdAt DATETIME DEFAULT CURRENT_TIMESTAMP(), " +
                                         "updatedBy INT, FOREIGN KEY (updatedBy) REFERENCES users(id), " +
                                         "updatedAt DATETIME DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP(), " +
                                         "FOREIGN KEY (groupId) REFERENCES groups(id)");
        Procedure.loadAll(database);
    }

    @Override
    public boolean existsGroup(int groupId) {
        return groupId > 0 && database.existsCallable(Procedure.GET_DATA.getName(), groupId);
    }

    @Override
    public int createGroup(int groupId, String chatPrefix, String chatSuffix, String chatColor,
                           String tabPrefix, String tabSuffix, String tabColor,
                           String teamPrefix, String teamSuffix, String teamColor,
                           int createdBy) {
        if (groupId < 1 || createdBy == -2) return 0;
        return database.updateCallable(Procedure.CREATE.getName(), groupId, chatPrefix, chatSuffix, chatColor,
                tabPrefix, tabSuffix, tabColor,
                teamPrefix, teamSuffix, teamColor,
                createdBy, createdBy);
    }

    @Override
    public int createGroup(int groupId, int createdBy) {
        return createGroup(groupId, null, null, null, null, null, null, null, null, "7", createdBy);
    }

    @Override
    public int deleteGroup(int groupId) {
        if (groupId < 1) return 0;
        return database.updateCallable(Procedure.DELETE.getName(), groupId);
    }

    @Override
    public String getPrefix(int groupId, GroupColorType type) {
        if (groupId < 1) return null;
        return database.queryCallable(Procedure.GET_DATA.getName(), null, resultSet -> resultSet.getString(type.getName() + "Prefix"), groupId);
    }

    @Override
    public String getSuffix(int groupId, GroupColorType type) {
        if (groupId < 1) return null;
        return database.queryCallable(Procedure.GET_DATA.getName(), null, resultSet -> resultSet.getString(type.getName() + "Suffix"), groupId);
    }

    @Override
    public String getColor(int groupId, GroupColorType type) {
        if (groupId < 1) return null;
        return database.queryCallable(Procedure.GET_DATA.getName(), null, resultSet -> resultSet.getString(type.getName() + "Color"), groupId);
    }

    @Override
    public int setPrefix(int groupId, GroupColorType type, String prefix, int updatedBy) {
        if (groupId < 1 || updatedBy == -2) return 0;
        return database.updateCallable(Procedure.UPDATE.getName(), groupId, type.getName() + "Prefix", prefix, updatedBy);
    }

    @Override
    public int setSuffix(int groupId, GroupColorType type, String suffix, int updatedBy) {
        if (groupId < 1 || updatedBy == -2) return 0;
        return database.updateCallable(Procedure.UPDATE.getName(), groupId, type.getName() + "Suffix", suffix, updatedBy);
    }

    @Override
    public int setColor(int groupId, GroupColorType type, String color, int updatedBy) {
        if (groupId < 1 || updatedBy == -2) return 0;
        return database.updateCallable(Procedure.UPDATE.getName(), groupId, type.getName() + "Color", color, updatedBy);
    }

    @Override
    public int getCreatedBy(int groupId) {
        if (groupId < 1) return -2;
        return database.queryCallable(Procedure.GET_DATA.getName(), -2, resultSet -> resultSet.getInt("createdBy"), groupId);
    }

    @Override
    public Timestamp getCreatedAt(int groupId) {
        if (groupId < 1) return null;
        return database.queryCallable(Procedure.GET_DATA.getName(), null, resultSet -> resultSet.getTimestamp("createdAt"), groupId);
    }

    @Override
    public String getCreatedDate(int groupId) {
        Timestamp createdAt = getCreatedAt(groupId);
        return createdAt == null ? null : getDateFormat().format(createdAt);
    }

    @Override
    public int getUpdatedBy(int groupId) {
        if (groupId < 1) return -2;
        return database.queryCallable(Procedure.GET_DATA.getName(), -2, resultSet -> resultSet.getInt("updatedBy"), groupId);
    }

    @Override
    public Timestamp getUpdatedAt(int groupId) {
        if (groupId < 1) return null;
        return database.queryCallable(Procedure.GET_DATA.getName(), null, resultSet -> resultSet.getTimestamp("updatedAt"), groupId);
    }

    @Override
    public String getUpdatedDate(int groupId) {
        Timestamp updatedAt = getUpdatedAt(groupId);
        return updatedAt == null ? null : getDateFormat().format(updatedAt);
    }

    private enum Procedure {
        CREATE("groupColor_create", "p_groupId INT, " +
                                    "p_chatPrefix VARCHAR(300), p_chatSuffix VARCHAR(300), p_chatColor VARCHAR(30), " +
                                    "p_tabPrefix VARCHAR(300), p_tabSuffix VARCHAR(300), p_tabColor VARCHAR(30), " +
                                    "p_teamPrefix VARCHAR(300), p_teamSuffix VARCHAR(300), p_teamColor VARCHAR(30), " +
                                    "p_createdBy INT, p_updatedBy INT",
                "INSERT INTO [TABLE] (groupId, chatPrefix, chatSuffix, chatColor, tabPrefix, tabSuffix, tabColor, teamPrefix, teamSuffix, teamColor, createdBy, updatedBy) " +
                "VALUES (p_groupId, p_chatPrefix, p_chatSuffix, p_chatColor, p_tabPrefix, p_tabSuffix, p_tabColor, p_teamPrefix, p_teamSuffix, p_teamColor, p_createdBy, p_updatedBy);"),
        DELETE("groupColor_delete", "p_groupId INT", "DELETE FROM [TABLE] WHERE groupId=p_groupId;"),
        GET_DATA("groupColor_getData", "p_groupId INT", "SELECT * FROM [TABLE] WHERE groupId=p_groupId;"),
        UPDATE("groupColor_update", "p_groupId INT, columnName VARCHAR(300), columnValue VARCHAR(300), p_updatedBy INT", """
                IF columnName IN ('ChatPrefix', 'ChatSuffix', 'ChatColor',
                                  'TabPrefix', 'TabSuffix', 'TabColor',
                                  'TeamPrefix', 'TeamSuffix', 'TeamColor') THEN
                    SET @sql = CONCAT('UPDATE [TABLE] SET ', columnName, '="', columnValue, '", updatedBy=', p_updatedBy, ' WHERE groupId=', p_groupId, ';');
                    PREPARE stmt FROM @sql;
                    EXECUTE stmt;
                    DEALLOCATE PREPARE stmt;
                ELSE
                    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid column name';
                END IF;""");
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

        public static void loadAll(Database database) {
            for (Procedure procedure : VALUES)
                database.update(procedure.getQuery());
        }
    }
}
