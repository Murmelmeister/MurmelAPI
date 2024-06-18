package de.murmelmeister.murmelapi.group.parent;

import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.utils.Database;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

public final class GroupParentProvider implements GroupParent {
    private static final String TABLE_NAME = "GroupParent";

    public GroupParentProvider() {
        createTable();
        Procedure.loadAll();
    }

    private void createTable() {
        Database.update("CREATE TABLE IF NOT EXISTS %s (GroupID INT, CreatorID INT, ParentID INT, CreatedTime BIGINT(255), ExpiredTime BIGINT(255))", TABLE_NAME);
    }

    @Override
    public boolean existsParent(int groupId, int parentId) {
        return Database.exists("CALL %s('%s','%s')", Procedure.PROCEDURE_PARENT.getName(), groupId, parentId);
    }

    @Override
    public void addParent(int groupId, int creatorId, int parentId, long time) {
        if (existsParent(groupId, parentId)) return;
        var expired = time == -1 ? time : System.currentTimeMillis() + time;
        Database.update("CALL %s('%s','%s','%s','%s','%s')", Procedure.PROCEDURE_ADD.getName(), groupId, creatorId, parentId, System.currentTimeMillis(), expired);
    }

    @Override
    public void removeParent(int groupId, int parentId) {
        Database.update("CALL %s('%s','%s')", Procedure.PROCEDURE_REMOVE.getName(), groupId, parentId);
    }

    @Override
    public void clearParent(int groupId) {
        Database.update("CALL %s('%s')", Procedure.PROCEDURE_CLEAR.getName(), groupId);
    }

    @Override
    public List<Integer> getParentIds(int groupId) {
        return Database.getIntList("ParentID", "CALL %s('%s')", Procedure.PROCEDURE_GROUP_ID.getName(), groupId);
    }

    @Override
    public List<String> getParentNames(Group group, int groupId) {
        return getParentIds(groupId).parallelStream().map(group::getName).collect(Collectors.toList());
    }

    @Override
    public int getCreatorId(int groupId, int parentId) {
        return Database.getInt(-2, "CreatorID", "CALL %s('%s','%s')", Procedure.PROCEDURE_PARENT.getName(), groupId, parentId);
    }

    @Override
    public long getCreatedTime(int groupId, int parentId) {
        return Database.getLong(-1, "CreatedTime", "CALL %s('%s','%s')", Procedure.PROCEDURE_PARENT.getName(), groupId, parentId);
    }

    @Override
    public String getCreatedDate(int groupId, int parentId) {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(getCreatedTime(groupId, parentId));
    }

    @Override
    public long getExpiredTime(int groupId, int parentId) {
        return Database.getLong(-2, "ExpiredTime", "CALL %s('%s','%s')", Procedure.PROCEDURE_PARENT.getName(), groupId, parentId);
    }

    @Override
    public String getExpiredDate(int groupId, int parentId) {
        var time = getExpiredTime(groupId, parentId);
        return time == -1 ? "never" : new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(time);
    }

    @Override
    public String setExpiredTime(int groupId, int parentId, long time) {
        var expired = time == -1 ? time : System.currentTimeMillis() + time;
        Database.update("CALL %s('%s','%s','%s')", Procedure.PROCEDURE_EXPIRED.getName(), groupId, parentId, expired);
        return getExpiredDate(groupId, parentId);
    }

    @Override
    public String addExpiredTime(int groupId, int parentId, long time) {
        var current = getExpiredTime(groupId, parentId);
        var expired = current == -1 ? System.currentTimeMillis() + time : current + time;
        Database.update("CALL %s('%s','%s','%s')", Procedure.PROCEDURE_EXPIRED.getName(), groupId, parentId, expired);
        return getExpiredDate(groupId, parentId);
    }

    @Override
    public String removeExpiredTime(int groupId, int parentId, long time) {
        var current = getExpiredTime(groupId, parentId);
        var expired = current == -1 ? System.currentTimeMillis() : current - time;
        Database.update("CALL %s('%s','%s','%s')", Procedure.PROCEDURE_EXPIRED.getName(), groupId, parentId, expired);
        return getExpiredDate(groupId, parentId);
    }

    @Override
    public void loadExpired(Group group) {
        for (var groupId : group.getUniqueIds())
            for (var parentId : getParentIds(groupId)) {
                var time = getExpiredTime(groupId, parentId);
                if (time == -1) continue;
                if (time <= System.currentTimeMillis()) removeParent(groupId, parentId);
            }
    }

    private enum Procedure {
        PROCEDURE_GROUP_ID("GroupParent_GroupID", Database.getProcedureQuery("GroupParent_GroupID", "gid INT", "SELECT * FROM %s WHERE GroupID=gid;", TABLE_NAME)),
        PROCEDURE_PARENT("GroupParent_Parent", Database.getProcedureQuery("GroupParent_Parent", "gid INT, pid INT", "SELECT * FROM %s WHERE GroupID=gid AND ParentID=pid;", TABLE_NAME)),
        PROCEDURE_ADD("GroupParent_Add", Database.getProcedureQuery("GroupParent_Add", "gid INT, creator INT, pid INT, created BIGINT(255), expired BIGINT(255)", "INSERT INTO %s VALUES (gid, creator, pid, created, expired);", TABLE_NAME)),
        PROCEDURE_REMOVE("GroupParent_Remove", Database.getProcedureQuery("GroupParent_Remove", "gid INT, pid INT", "DELETE FROM %s WHERE GroupID=gid AND ParentID=pid;", TABLE_NAME)),
        PROCEDURE_CLEAR("GroupParent_Clear", Database.getProcedureQuery("GroupParent_Clear", "gid INT", "DELETE FROM %s WHERE GroupID=gid;", TABLE_NAME)),
        PROCEDURE_EXPIRED("GroupParent_Expired", Database.getProcedureQuery("GroupParent_Expired", "gid INT, pid INT, expired BIGINT(255)", "UPDATE %s SET ExpiredTime=expired WHERE GroupID=gid AND ParentID=pid;", TABLE_NAME));
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

        public static void loadAll() {
            for (var procedure : VALUES)
                Database.update(procedure.getQuery());
        }
    }
}
