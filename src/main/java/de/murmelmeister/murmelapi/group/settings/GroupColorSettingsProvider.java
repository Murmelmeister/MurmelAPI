package de.murmelmeister.murmelapi.group.settings;

import de.murmelmeister.murmelapi.utils.Database;

import java.sql.SQLException;
import java.text.SimpleDateFormat;

import static de.murmelmeister.murmelapi.utils.StringUtil.checkArgumentSQL;

public final class GroupColorSettingsProvider implements GroupColorSettings {
    private static final String TABLE_NAME = "GroupColorSettings";

    public GroupColorSettingsProvider() throws SQLException {
        createTable();
        Procedure.loadAll();
    }

    private void createTable() throws SQLException {
        Database.update("CREATE TABLE IF NOT EXISTS %s (GroupID INT PRIMARY KEY, CreatorID INT, EditedTime BIGINT(255), " +
                        "ChatPrefix VARCHAR(300), ChatSuffix VARCHAR(300), ChatColor VARCHAR(30), " +
                        "TabPrefix VARCHAR(300), TabSuffix VARCHAR(300), TabColor VARCHAR(30), " +
                        "TagPrefix VARCHAR(300), TagSuffix VARCHAR(300), TagColor VARCHAR(30))", TABLE_NAME);
    }

    @Override
    public boolean existsGroup(int groupId) throws SQLException {
        return Database.exists("CALL %s('%s')", Procedure.PROCEDURE_GROUP_ID.getName(), checkArgumentSQL(groupId));
    }

    @Override
    public void createGroup(int groupId, int creatorId) throws SQLException {
        createGroup(groupId, creatorId, "", "", "", "", "", "", "", "", "");
    }

    @Override
    public void createGroup(int groupId, int creatorId, String chatPrefix, String chatSuffix, String chatColor, String tabPrefix, String tabSuffix, String tabColor, String tagPrefix, String tagSuffix, String tagColor) throws SQLException {
        if (existsGroup(groupId)) return;
        Database.update("CALL %s('%s','%s','%s','%s','%s','%s','%s','%s','%s','%s','%s','%s')", Procedure.PROCEDURE_INSERT.getName(), groupId, creatorId, System.currentTimeMillis(),
                chatPrefix, chatSuffix, chatColor, tabPrefix, tabSuffix, tabColor, tagPrefix, tagSuffix, tagColor);
    }

    @Override
    public void deleteGroup(int groupId) throws SQLException {
        Database.update("CALL %s('%s')", Procedure.PROCEDURE_DELETE.getName(), checkArgumentSQL(groupId));
    }

    @Override
    public int getCreatorId(int groupId) throws SQLException {
        return Database.getInt(-2, "CreatorID", "CALL %s('%s')", Procedure.PROCEDURE_GROUP_ID.getName(), checkArgumentSQL(groupId));
    }

    @Override
    public long getEditedTime(int groupId) throws SQLException {
        return Database.getLong(-1, "EditedTime", "CALL %s('%s')", Procedure.PROCEDURE_GROUP_ID.getName(), checkArgumentSQL(groupId));
    }

    @Override
    public String getEditedDate(int groupId) throws SQLException {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(getEditedTime(groupId));
    }

    @Override
    public String getPrefix(GroupColorType type, int groupId) throws SQLException {
        return Database.getString(null, type.getName() + "Prefix", "CALL %s('%s')", Procedure.PROCEDURE_GROUP_ID.getName(), checkArgumentSQL(groupId));
    }

    @Override
    public String getSuffix(GroupColorType type, int groupId) throws SQLException {
        return Database.getString(null, type.getName() + "Suffix", "CALL %s('%s')", Procedure.PROCEDURE_GROUP_ID.getName(), checkArgumentSQL(groupId));
    }

    @Override
    public String getColor(GroupColorType type, int groupId) throws SQLException {
        return Database.getString(null, type.getName() + "Color", "CALL %s('%s')", Procedure.PROCEDURE_GROUP_ID.getName(), checkArgumentSQL(groupId));
    }

    @Override
    public void setPrefix(GroupColorType type, int groupId, int creatorId, String prefix) throws SQLException {
        var name = switch (type) {
            case CHAT -> Procedure.PROCEDURE_UPDATE_CHAT_PREFIX.getName();
            case TAB -> Procedure.PROCEDURE_UPDATE_TAB_PREFIX.getName();
            case TAG -> Procedure.PROCEDURE_UPDATE_TAG_PREFIX.getName();
        };
        Database.update("CALL %s('%s','%s','%s','%s')", name, checkArgumentSQL(groupId), checkArgumentSQL(creatorId), System.currentTimeMillis(), prefix);
    }

    @Override
    public void setSuffix(GroupColorType type, int groupId, int creatorId, String suffix) throws SQLException {
        var name = switch (type) {
            case CHAT -> Procedure.PROCEDURE_UPDATE_CHAT_SUFFIX.getName();
            case TAB -> Procedure.PROCEDURE_UPDATE_TAB_SUFFIX.getName();
            case TAG -> Procedure.PROCEDURE_UPDATE_TAG_SUFFIX.getName();
        };
        Database.update("CALL %s('%s','%s','%s','%s')", name, checkArgumentSQL(groupId), checkArgumentSQL(creatorId), System.currentTimeMillis(), suffix);
    }

    @Override
    public void setColor(GroupColorType type, int groupId, int creatorId, String color) throws SQLException {
        var name = switch (type) {
            case CHAT -> Procedure.PROCEDURE_UPDATE_CHAT_COLOR.getName();
            case TAB -> Procedure.PROCEDURE_UPDATE_TAB_COLOR.getName();
            case TAG -> Procedure.PROCEDURE_UPDATE_TAG_COLOR.getName();
        };
        Database.update("CALL %s('%s','%s','%s','%s')", name, checkArgumentSQL(groupId), checkArgumentSQL(creatorId), System.currentTimeMillis(), color);
    }

    private enum Procedure {
        PROCEDURE_GROUP_ID("GroupColorSettings_GroupID", Database.getProcedureQuery("GroupColorSettings_GroupID", "gid INT", "SELECT * FROM %s WHERE GroupID=gid;", TABLE_NAME)),
        PROCEDURE_INSERT("GroupColorSettings_Insert", Database.getProcedureQuery("GroupColorSettings_Insert", "gid INT, creator INT, time BIGINT(255), " +
                                                                                                              "chatP VARCHAR(300), chatS VARCHAR(300), chatC VARCHAR(30), " +
                                                                                                              "tabP VARCHAR(300), tabS VARCHAR(300), tabC VARCHAR(30), " +
                                                                                                              "tagP VARCHAR(300), tagS VARCHAR(300), tagC VARCHAR(30)",
                "INSERT INTO %s VALUES (gid, creator, time, chatP, chatS, chatC, tabP, tabS, tabC, tagP, tagS, tagC);", TABLE_NAME)),
        PROCEDURE_DELETE("GroupColorSettings_Delete", Database.getProcedureQuery("GroupColorSettings_Delete", "gid INT", "DELETE FROM %s WHERE GroupID=gid;", TABLE_NAME)),
        PROCEDURE_UPDATE_CHAT_PREFIX("GroupColorSettings_Update_ChatPrefix", Database.getProcedureQuery("GroupColorSettings_Update_ChatPrefix", "gid INT, creator INT, time BIGINT(255), chat VARCHAR(300)", "UPDATE %s SET ChatPrefix=chat, CreatorID=creator, EditedTime=time WHERE GroupID=gid;", TABLE_NAME)),
        PROCEDURE_UPDATE_CHAT_SUFFIX("GroupColorSettings_Update_ChatSuffix", Database.getProcedureQuery("GroupColorSettings_Update_ChatSuffix", "gid INT, creator INT, time BIGINT(255), chat VARCHAR(300)", "UPDATE %s SET ChatSuffix=chat, CreatorID=creator, EditedTime=time WHERE GroupID=gid;", TABLE_NAME)),
        PROCEDURE_UPDATE_CHAT_COLOR("GroupColorSettings_Update_ChatColor", Database.getProcedureQuery("GroupColorSettings_Update_ChatColor", "gid INT, creator INT, time BIGINT(255), chat VARCHAR(30)", "UPDATE %s SET ChatColor=chat, CreatorID=creator, EditedTime=time WHERE GroupID=gid;", TABLE_NAME)),
        PROCEDURE_UPDATE_TAB_PREFIX("GroupColorSettings_Update_TabPrefix", Database.getProcedureQuery("GroupColorSettings_Update_TabPrefix", "gid INT, creator INT, time BIGINT(255), tab VARCHAR(300)", "UPDATE %s SET TabPrefix=tab, CreatorID=creator, EditedTime=time WHERE GroupID=gid;", TABLE_NAME)),
        PROCEDURE_UPDATE_TAB_SUFFIX("GroupColorSettings_Update_TabSuffix", Database.getProcedureQuery("GroupColorSettings_Update_TabSuffix", "gid INT, creator INT, time BIGINT(255), tab VARCHAR(300)", "UPDATE %s SET TabSuffix=tab, CreatorID=creator, EditedTime=time WHERE GroupID=gid;", TABLE_NAME)),
        PROCEDURE_UPDATE_TAB_COLOR("GroupColorSettings_Update_TabColor", Database.getProcedureQuery("GroupColorSettings_Update_TabColor", "gid INT, creator INT, time BIGINT(255), tab VARCHAR(30)", "UPDATE %s SET TabColor=tab, CreatorID=creator, EditedTime=time WHERE GroupID=gid;", TABLE_NAME)),
        PROCEDURE_UPDATE_TAG_PREFIX("GroupColorSettings_Update_TagPrefix", Database.getProcedureQuery("GroupColorSettings_Update_TagPrefix", "gid INT, creator INT, time BIGINT(255), tag VARCHAR(300)", "UPDATE %s SET TagPrefix=tag, CreatorID=creator, EditedTime=time WHERE GroupID=gid;", TABLE_NAME)),
        PROCEDURE_UPDATE_TAG_SUFFIX("GroupColorSettings_Update_TagSuffix", Database.getProcedureQuery("GroupColorSettings_Update_TagSuffix", "gid INT, creator INT, time BIGINT(255), tag VARCHAR(300)", "UPDATE %s SET TagSuffix=tag, CreatorID=creator, EditedTime=time WHERE GroupID=gid;", TABLE_NAME)),
        PROCEDURE_UPDATE_TAG_COLOR("GroupColorSettings_Update_TagColor", Database.getProcedureQuery("GroupColorSettings_Update_TagColor", "gid INT, creator INT, time BIGINT(255), tag VARCHAR(30)", "UPDATE %s SET TagColor=tag, CreatorID=creator, EditedTime=time WHERE GroupID=gid;", TABLE_NAME));
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
