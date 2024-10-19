package de.murmelmeister.murmelapi.group.settings;

import de.murmelmeister.murmelapi.utils.Database;

import java.text.SimpleDateFormat;

public final class GroupColorSettingsProvider implements GroupColorSettings {
    public GroupColorSettingsProvider() {
        String tableName = "GroupColorSettings";
        createTable(tableName);
        Procedure.loadAll(tableName);
    }

    private void createTable(String tableName) {
        Database.createTable(tableName, "GroupID INT PRIMARY KEY, CreatorID INT, EditedTime BIGINT, " +
                                        "ChatPrefix VARCHAR(300), ChatSuffix VARCHAR(300), ChatColor VARCHAR(30), " +
                                        "TabPrefix VARCHAR(300), TabSuffix VARCHAR(300), TabColor VARCHAR(30), " +
                                        "TagPrefix VARCHAR(300), TagSuffix VARCHAR(300), TagColor VARCHAR(30)");
    }

    @Override
    public boolean existsGroup(int groupId) {
        return Database.callExists(Procedure.GROUP_COLOR_SETTINGS_GROUP_ID.getName(), groupId);
    }

    @Override
    public void createGroup(int groupId, int creatorId) {
        createGroup(groupId, creatorId, "", "", "", "", "", "", "", "", "&7");
    }

    @Override
    public void createGroup(int groupId, int creatorId, String chatPrefix, String chatSuffix, String chatColor, String tabPrefix, String tabSuffix, String tabColor, String tagPrefix, String tagSuffix, String tagColor) {
        if (existsGroup(groupId)) return;
        Database.callUpdate(Procedure.GROUP_COLOR_SETTINGS_INSERT.getName(), groupId, creatorId, System.currentTimeMillis(),
                chatPrefix, chatSuffix, chatColor, tabPrefix, tabSuffix, tabColor, tagPrefix, tagSuffix, tagColor);
    }

    @Override
    public void deleteGroup(int groupId) {
        Database.callUpdate(Procedure.GROUP_COLOR_SETTINGS_DELETE.getName(), groupId);
    }

    @Override
    public int getCreatorId(int groupId) {
        return Database.callQuery(-2, "CreatorID", int.class, Procedure.GROUP_COLOR_SETTINGS_GROUP_ID.getName(), groupId);
    }

    @Override
    public long getEditedTime(int groupId) {
        return Database.callQuery(-1L, "EditedTime", long.class, Procedure.GROUP_COLOR_SETTINGS_GROUP_ID.getName(), groupId);
    }

    @Override
    public String getEditedDate(int groupId) {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(getEditedTime(groupId));
    }

    @Override
    public String getPrefix(GroupColorType type, int groupId) {
        return Database.callQuery(null, type.getName() + "Prefix", String.class, Procedure.GROUP_COLOR_SETTINGS_GROUP_ID.getName(), groupId);
    }

    @Override
    public String getSuffix(GroupColorType type, int groupId) {
        return Database.callQuery(null, type.getName() + "Suffix", String.class, Procedure.GROUP_COLOR_SETTINGS_GROUP_ID.getName(), groupId);
    }

    @Override
    public String getColor(GroupColorType type, int groupId) {
        return Database.callQuery(null, type.getName() + "Color", String.class, Procedure.GROUP_COLOR_SETTINGS_GROUP_ID.getName(), groupId);
    }

    @Override
    public void setPrefix(GroupColorType type, int groupId, int creatorId, String prefix) {
        String name = switch (type) {
            case CHAT -> Procedure.GROUP_COLOR_SETTINGS_UPDATE_CHAT_PREFIX.getName();
            case TAB -> Procedure.GROUP_COLOR_SETTINGS_UPDATE_TAB_PREFIX.getName();
            case TAG -> Procedure.GROUP_COLOR_SETTINGS_UPDATE_TAG_PREFIX.getName();
        };
        Database.callUpdate(name, groupId, creatorId, System.currentTimeMillis(), prefix);
    }

    @Override
    public void setSuffix(GroupColorType type, int groupId, int creatorId, String suffix) {
        String name = switch (type) {
            case CHAT -> Procedure.GROUP_COLOR_SETTINGS_UPDATE_CHAT_SUFFIX.getName();
            case TAB -> Procedure.GROUP_COLOR_SETTINGS_UPDATE_TAB_SUFFIX.getName();
            case TAG -> Procedure.GROUP_COLOR_SETTINGS_UPDATE_TAG_SUFFIX.getName();
        };
        Database.callUpdate(name, groupId, creatorId, System.currentTimeMillis(), suffix);
    }

    @Override
    public void setColor(GroupColorType type, int groupId, int creatorId, String color) {
        String name = switch (type) {
            case CHAT -> Procedure.GROUP_COLOR_SETTINGS_UPDATE_CHAT_COLOR.getName();
            case TAB -> Procedure.GROUP_COLOR_SETTINGS_UPDATE_TAB_COLOR.getName();
            case TAG -> Procedure.GROUP_COLOR_SETTINGS_UPDATE_TAG_COLOR.getName();
        };
        Database.callUpdate(name, groupId, creatorId, System.currentTimeMillis(), color);
    }

    private enum Procedure {
        GROUP_COLOR_SETTINGS_GROUP_ID("GroupColorSettings_GroupID", "gid INT", "SELECT * FROM [TABLE] WHERE GroupID=gid;"),
        GROUP_COLOR_SETTINGS_INSERT("GroupColorSettings_Insert", "gid INT, creator INT, time BIGINT, " +
                                                                 "chatP VARCHAR(300), chatS VARCHAR(300), chatC VARCHAR(30), " +
                                                                 "tabP VARCHAR(300), tabS VARCHAR(300), tabC VARCHAR(30), " +
                                                                 "tagP VARCHAR(300), tagS VARCHAR(300), tagC VARCHAR(30)",
                "INSERT INTO [TABLE] VALUES (gid, creator, time, chatP, chatS, chatC, tabP, tabS, tabC, tagP, tagS, tagC);"),
        GROUP_COLOR_SETTINGS_DELETE("GroupColorSettings_Delete", "gid INT", "DELETE FROM [TABLE] WHERE GroupID=gid;"),
        GROUP_COLOR_SETTINGS_UPDATE_CHAT_PREFIX("GroupColorSettings_Update_ChatPrefix", "gid INT, creator INT, time BIGINT, chat VARCHAR(300)", "UPDATE [TABLE] SET ChatPrefix=chat, CreatorID=creator, EditedTime=time WHERE GroupID=gid;"),
        GROUP_COLOR_SETTINGS_UPDATE_CHAT_SUFFIX("GroupColorSettings_Update_ChatSuffix", "gid INT, creator INT, time BIGINT, chat VARCHAR(300)", "UPDATE [TABLE] SET ChatSuffix=chat, CreatorID=creator, EditedTime=time WHERE GroupID=gid;"),
        GROUP_COLOR_SETTINGS_UPDATE_CHAT_COLOR("GroupColorSettings_Update_ChatColor", "gid INT, creator INT, time BIGINT, chat VARCHAR(30)", "UPDATE [TABLE] SET ChatColor=chat, CreatorID=creator, EditedTime=time WHERE GroupID=gid;"),
        GROUP_COLOR_SETTINGS_UPDATE_TAB_PREFIX("GroupColorSettings_Update_TabPrefix", "gid INT, creator INT, time BIGINT, tab VARCHAR(300)", "UPDATE [TABLE] SET TabPrefix=tab, CreatorID=creator, EditedTime=time WHERE GroupID=gid;"),
        GROUP_COLOR_SETTINGS_UPDATE_TAB_SUFFIX("GroupColorSettings_Update_TabSuffix", "gid INT, creator INT, time BIGINT, tab VARCHAR(300)", "UPDATE [TABLE] SET TabSuffix=tab, CreatorID=creator, EditedTime=time WHERE GroupID=gid;"),
        GROUP_COLOR_SETTINGS_UPDATE_TAB_COLOR("GroupColorSettings_Update_TabColor", "gid INT, creator INT, time BIGINT, tab VARCHAR(30)", "UPDATE [TABLE] SET TabColor=tab, CreatorID=creator, EditedTime=time WHERE GroupID=gid;"),
        GROUP_COLOR_SETTINGS_UPDATE_TAG_PREFIX("GroupColorSettings_Update_TagPrefix", "gid INT, creator INT, time BIGINT, tag VARCHAR(300)", "UPDATE [TABLE] SET TagPrefix=tag, CreatorID=creator, EditedTime=time WHERE GroupID=gid;"),
        GROUP_COLOR_SETTINGS_UPDATE_TAG_SUFFIX("GroupColorSettings_Update_TagSuffix", "gid INT, creator INT, time BIGINT, tag VARCHAR(300)", "UPDATE [TABLE] SET TagSuffix=tag, CreatorID=creator, EditedTime=time WHERE GroupID=gid;"),
        GROUP_COLOR_SETTINGS_UPDATE_TAG_COLOR("GroupColorSettings_Update_TagColor", "gid INT, creator INT, time BIGINT, tag VARCHAR(30)", "UPDATE [TABLE] SET TagColor=tag, CreatorID=creator, EditedTime=time WHERE GroupID=gid;");
        private static final Procedure[] VALUES = values();

        private final String name;
        private final String query;

        Procedure(final String name, final String input, final String query) {
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
            for (Procedure procedure : VALUES) Database.update(procedure.getQuery(tableName));
        }
    }
}
