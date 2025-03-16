package de.murmelmeister.murmelapi.group.color;

import de.murmelmeister.murmelapi.database.Database;
import de.murmelmeister.murmelapi.group.Group;

import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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

    // === Asynchrone API-Methoden ===

    public CompletableFuture<Boolean> existsGroupAsync(int groupId) {
        return database.asyncExists(Procedure.GET_ALL_BY_ID.getName(), groupId);
    }

    public CompletableFuture<Void> createGroupAsync(int executorId, int groupId) {
        return createGroupAsync(executorId, groupId, "", "", "", "", "", "", "", "", "7");
    }

    public CompletableFuture<Void> createGroupAsync(int executorId, int groupId,
                                                    String chatPrefix, String chatSuffix, String chatColor,
                                                    String tabPrefix, String tabSuffix, String tabColor,
                                                    String teamPrefix, String teamSuffix, String teamColor) {
        return database.asyncUpdate(Procedure.CREATE.getName(),
                groupId, chatPrefix, chatSuffix, chatColor,
                tabPrefix, tabSuffix, tabColor,
                teamPrefix, teamSuffix, teamColor, executorId, executorId);
    }

    public CompletableFuture<Void> deleteGroupAsync(int groupId) {
        return database.asyncUpdate(Procedure.DELETE.getName(), groupId);
    }

    public CompletableFuture<String> getPrefixAsync(int groupId, GroupColorType type) {
        return database.asyncQuery(null, type.getName() + "Prefix", String.class, Procedure.GET_ALL_BY_ID.getName(), groupId);
    }

    public CompletableFuture<String> getSuffixAsync(int groupId, GroupColorType type) {
        return database.asyncQuery(null, type.getName() + "Suffix", String.class, Procedure.GET_ALL_BY_ID.getName(), groupId);
    }

    public CompletableFuture<String> getColorAsync(int groupId, GroupColorType type) {
        return database.asyncQuery(null, type.getName() + "Color", String.class, Procedure.GET_ALL_BY_ID.getName(), groupId);
    }

    public CompletableFuture<Void> setPrefixAsync(int executorId, int groupId, GroupColorType type, String prefix) {
        return database.asyncUpdate(Procedure.UPDATE.getName(), type.getName() + "Prefix", prefix, groupId, executorId);
    }

    public CompletableFuture<Void> setSuffixAsync(int executorId, int groupId, GroupColorType type, String suffix) {
        return database.asyncUpdate(Procedure.UPDATE.getName(), type.getName() + "Suffix", suffix, groupId, executorId);
    }

    public CompletableFuture<Void> setColorAsync(int executorId, int groupId, GroupColorType type, String color) {
        return database.asyncUpdate(Procedure.UPDATE.getName(), type.getName() + "Color", color, groupId, executorId);
    }

    public CompletableFuture<Integer> getCreatedByAsync(int groupId) {
        return database.asyncQuery(-2, "CreatedBy", int.class, Procedure.GET_ALL_BY_ID.getName(), groupId);
    }

    public CompletableFuture<Timestamp> getCreatedAtAsync(int groupId) {
        return database.asyncQuery(null, "CreatedAt", Timestamp.class, Procedure.GET_ALL_BY_ID.getName(), groupId);
    }

    public CompletableFuture<Integer> getModifiedByAsync(int groupId) {
        return database.asyncQuery(-2, "ModifiedBy", int.class, Procedure.GET_ALL_BY_ID.getName(), groupId);
    }

    public CompletableFuture<Timestamp> getModifiedAtAsync(int groupId) {
        return database.asyncQuery(null, "ModifiedAt", Timestamp.class, Procedure.GET_ALL_BY_ID.getName(), groupId);
    }

    public CompletableFuture<Void> loadTablesAsync(Group group) {
        return CompletableFuture.runAsync(() -> {
            List<Integer> groupIds = group.getUniqueIds();
            for (Integer groupId : groupIds) {
                if (!existsGroupAsync(groupId).join()) {
                    createGroupAsync(-1, groupId).join();
                }
            }
        });
    }

    // === Synchrone Wrapper (Interface-Implementierung) ===

    @Override
    public boolean existsGroup(int groupId) {
        return existsGroupAsync(groupId).join();
    }

    @Override
    public void createGroup(int executorId, int groupId) {
        createGroup(executorId, groupId, "", "", "", "", "", "", "", "", "7");
    }

    @Override
    public void createGroup(int executorId, int groupId, String chatPrefix, String chatSuffix, String chatColor, String tabPrefix, String tabSuffix, String tabColor, String teamPrefix, String teamSuffix, String teamColor) {
        createGroupAsync(executorId, groupId, chatPrefix, chatSuffix, chatColor, tabPrefix, tabSuffix, tabColor, teamPrefix, teamSuffix, teamColor).join();
    }

    @Override
    public void deleteGroup(int groupId) {
        deleteGroupAsync(groupId).join();
    }

    @Override
    public String getPrefix(int groupId, GroupColorType type) {
        return getPrefixAsync(groupId, type).join();
    }

    @Override
    public String getSuffix(int groupId, GroupColorType type) {
        return getSuffixAsync(groupId, type).join();
    }

    @Override
    public String getColor(int groupId, GroupColorType type) {
        return getColorAsync(groupId, type).join();
    }

    @Override
    public void setPrefix(int executorId, int groupId, GroupColorType type, String prefix) {
        setPrefixAsync(executorId, groupId, type, prefix).join();
    }

    @Override
    public void setSuffix(int executorId, int groupId, GroupColorType type, String suffix) {
        setSuffixAsync(executorId, groupId, type, suffix).join();
    }

    @Override
    public void setColor(int executorId, int groupId, GroupColorType type, String color) {
        setColorAsync(executorId, groupId, type, color).join();
    }

    @Override
    public int getCreatedBy(int groupId) {
        return getCreatedByAsync(groupId).join();
    }

    @Override
    public Timestamp getCreatedAt(int groupId) {
        return getCreatedAtAsync(groupId).join();
    }

    @Override
    public int getModifiedBy(int groupId) {
        return getModifiedByAsync(groupId).join();
    }

    @Override
    public Timestamp getModifiedAt(int groupId) {
        return getModifiedAtAsync(groupId).join();
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
