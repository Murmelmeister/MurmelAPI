package de.murmelmeister.murmelapi.group.parent;

import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.utils.Database;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

public final class GroupParentProvider implements GroupParent {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public GroupParentProvider() {
        String tableName = "GroupParent";
        createTable(tableName);
        Procedure.loadAll(tableName);
    }

    private void createTable(String tableName) {
        Database.createTable("GroupID INT, CreatorID INT, ParentID INT, CreatedTime BIGINT(255), ExpiredTime BIGINT(255)", tableName);
    }

    @Override
    public boolean existsParent(int groupId, int parentId) {
        return Database.existsCall(Procedure.GROUP_PARENT_PARENT.getName(), groupId, parentId);
    }

    @Override
    public void addParent(int groupId, int creatorId, int parentId, long time) {
        if (existsParent(groupId, parentId)) return;
        long expired = time == -1 ? time : System.currentTimeMillis() + time;
        Database.updateCall(Procedure.GROUP_PARENT_ADD.getName(), groupId, creatorId, parentId, System.currentTimeMillis(), expired);
    }

    @Override
    public void removeParent(int groupId, int parentId) {
        Database.updateCall(Procedure.GROUP_PARENT_REMOVE.getName(), groupId, parentId);
    }

    @Override
    public void clearParent(int groupId) {
        Database.updateCall(Procedure.GROUP_PARENT_CLEAR.getName(), groupId);
    }

    @Override
    public List<Integer> getParentIds(int groupId) {
        return Database.getIntListCall("ParentID", Procedure.GROUP_PARENT_GROUP_ID.getName(), groupId);
    }

    @Override
    public List<String> getParentNames(Group group, int groupId) {
        return getParentIds(groupId).parallelStream().map(group::getName).collect(Collectors.toList());
    }

    @Override
    public int getCreatorId(int groupId, int parentId) {
        return Database.getIntCall(-2, "CreatorID", Procedure.GROUP_PARENT_PARENT.getName(), groupId, parentId);
    }

    @Override
    public long getCreatedTime(int groupId, int parentId) {
        return Database.getLongCall(-1, "CreatedTime", Procedure.GROUP_PARENT_PARENT.getName(), groupId, parentId);
    }

    @Override
    public String getCreatedDate(int groupId, int parentId) {
        return dateFormat.format(getCreatedTime(groupId, parentId));
    }

    @Override
    public long getExpiredTime(int groupId, int parentId) {
        return Database.getLongCall(-2, "ExpiredTime", Procedure.GROUP_PARENT_PARENT.getName(), groupId, parentId);
    }

    @Override
    public String getExpiredDate(int groupId, int parentId) {
        long time = getExpiredTime(groupId, parentId);
        return time == -1 ? "never" : dateFormat.format(time);
    }

    @Override
    public String setExpiredTime(int groupId, int parentId, long time) {
        long expired = time == -1 ? time : System.currentTimeMillis() + time;
        Database.updateCall(Procedure.GROUP_PARENT_EXPIRED.getName(), groupId, parentId, expired);
        return getExpiredDate(groupId, parentId);
    }

    @Override
    public String addExpiredTime(int groupId, int parentId, long time) {
        long current = getExpiredTime(groupId, parentId);
        long expired = current == -1 ? System.currentTimeMillis() + time : current + time;
        Database.updateCall(Procedure.GROUP_PARENT_EXPIRED.getName(), groupId, parentId, expired);
        return getExpiredDate(groupId, parentId);
    }

    @Override
    public String removeExpiredTime(int groupId, int parentId, long time) {
        long current = getExpiredTime(groupId, parentId);
        long expired = current == -1 ? System.currentTimeMillis() : current - time;
        Database.updateCall(Procedure.GROUP_PARENT_EXPIRED.getName(), groupId, parentId, expired);
        return getExpiredDate(groupId, parentId);
    }

    @Override
    public void loadExpired(Group group) {
        for (int groupId : group.getUniqueIds())
            for (int parentId : getParentIds(groupId)) {
                long time = getExpiredTime(groupId, parentId);
                if (time == -1) continue;
                if (time <= System.currentTimeMillis()) removeParent(groupId, parentId);
            }
    }

    private enum Procedure {
        GROUP_PARENT_GROUP_ID("GroupParent_GroupID", "gid INT", "SELECT * FROM [TABLE] WHERE GroupID=gid;"),
        GROUP_PARENT_PARENT("GroupParent_Parent", "gid INT, pid INT", "SELECT * FROM [TABLE] WHERE GroupID=gid AND ParentID=pid;"),
        GROUP_PARENT_ADD("GroupParent_Add", "gid INT, creator INT, pid INT, created BIGINT(255), expired BIGINT(255)", "INSERT INTO [TABLE] VALUES (gid, creator, pid, created, expired);"),
        GROUP_PARENT_REMOVE("GroupParent_Remove", "gid INT, pid INT", "DELETE FROM [TABLE] WHERE GroupID=gid AND ParentID=pid;"),
        GROUP_PARENT_CLEAR("GroupParent_Clear", "gid INT", "DELETE FROM [TABLE] WHERE GroupID=gid;"),
        GROUP_PARENT_EXPIRED("GroupParent_Expired", "gid INT, pid INT, expired BIGINT(255)", "UPDATE [TABLE] SET ExpiredTime=expired WHERE GroupID=gid AND ParentID=pid;");
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
