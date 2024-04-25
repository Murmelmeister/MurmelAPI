package de.murmelmeister.murmelapi.group.parent;

import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.utils.Database;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static de.murmelmeister.murmelapi.utils.StringUtil.checkArgumentSQL;

public final class GroupParentProvider implements GroupParent {
    private static final String TABLE_NAME = "GroupParent";

    public GroupParentProvider() throws SQLException {
        createTable();
        Procedure.loadAll();
    }

    private void createTable() throws SQLException {
        Database.update("CREATE TABLE IF NOT EXISTS %s (GroupID INT PRIMARY KEY, CreatorID INT, ParentID INT, CreatedTime BIGINT(255), ExpiredTime BIGINT(255))", TABLE_NAME);
    }

    @Override
    public boolean existsParent(int groupId, int parentId) throws SQLException {
        return Database.exists("CALL %s('%s','%s')", Procedure.PROCEDURE_PARENT.getName(), checkArgumentSQL(groupId), checkArgumentSQL(parentId));
    }

    @Override
    public void addParent(int groupId, int creatorId, int parentId, long time) throws SQLException {
        if (existsParent(groupId, parentId)) return;
        var expired = time == -1 ? time : System.currentTimeMillis() + time;
        Database.update("CALL %s('%s','%s','%s','%s','%s')", Procedure.PROCEDURE_ADD.getName(), groupId, creatorId, parentId, System.currentTimeMillis(), expired);
    }

    @Override
    public void removeParent(int groupId, int parentId) throws SQLException {
        Database.update("CALL %s('%s','%s')", Procedure.PROCEDURE_REMOVE.getName(), checkArgumentSQL(groupId), checkArgumentSQL(parentId));
    }

    @Override
    public void clearParent(int groupId) throws SQLException {
        Database.update("CALL %s('%s')", Procedure.PROCEDURE_CLEAR.getName(), checkArgumentSQL(groupId));
    }

    @Override
    public List<Integer> getParentIds(int groupId) throws SQLException {
        return Database.getIntList(new ArrayList<>(), "ParentID", "CALL %s('%s')", Procedure.PROCEDURE_GROUP_ID.getName(), checkArgumentSQL(groupId));
    }

    @Override
    public List<String> getParentNames(Group group, int groupId) throws SQLException {
        List<String> names = new ArrayList<>();
        for (int parentId : getParentIds(groupId)) names.add(group.getName(parentId));
        return names;
    }

    @Override
    public int getCreatorId(int groupId, int parentId) throws SQLException {
        return Database.getInt(-2, "CreatorID", "CALL %s('%s','%s')", Procedure.PROCEDURE_PARENT.getName(), checkArgumentSQL(groupId), checkArgumentSQL(parentId));
    }

    @Override
    public UUID getCreatorId(User user, int groupId, int parentId) throws SQLException {
        var creatorId = getCreatorId(groupId, parentId);
        return user.getUniqueId(creatorId);
    }

    @Override
    public long getCreatedTime(int groupId, int parentId) throws SQLException {
        return Database.getLong(-1, "CreatedTime", "CALL %s('%s','%s')", Procedure.PROCEDURE_PARENT.getName(), checkArgumentSQL(groupId), checkArgumentSQL(parentId));
    }

    @Override
    public String getCreatedDate(int groupId, int parentId) throws SQLException {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(getCreatedTime(groupId, parentId));
    }

    @Override
    public long getExpiredTime(int groupId, int parentId) throws SQLException {
        return Database.getLong(-2, "ExpiredTime", "CALL %s('%s','%s')", Procedure.PROCEDURE_PARENT.getName(), checkArgumentSQL(groupId), checkArgumentSQL(parentId));
    }

    @Override
    public String getExpiredDate(int groupId, int parentId) throws SQLException {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(getExpiredTime(groupId, parentId));
    }

    @Override
    public void setExpiredTime(int groupId, int parentId, long time) throws SQLException {
        var expired = time == -1 ? time : System.currentTimeMillis() + time;
        Database.update("CALL %s('%s','%s','%s')", Procedure.PROCEDURE_EXPIRED.getName(), checkArgumentSQL(groupId), checkArgumentSQL(parentId), expired);
    }

    @Override
    public String addExpiredTime(int groupId, int parentId, long time) throws SQLException {
        if (Long.toString(time).startsWith("-")) return "No negative time";
        var expired = getExpiredTime(groupId, parentId) + (System.currentTimeMillis() + time);
        setExpiredTime(groupId, parentId, expired);
        return "";
    }

    @Override
    public String removeExpiredTime(int groupId, int parentId, long time) throws SQLException {
        if (Long.toString(time).startsWith("-")) return "No negative time";
        var expired = getExpiredTime(groupId, parentId) - (System.currentTimeMillis() + time);
        setExpiredTime(groupId, parentId, expired);
        return "";
    }

    @Override
    public void loadExpired(Group group) throws SQLException {
        for (int groupId : group.getUniqueIds())
            for (int parentId : getParentIds(groupId)) {
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

        public static void loadAll() throws SQLException {
            for (Procedure procedure : VALUES)
                Database.update(procedure.getQuery());
        }
    }
}
