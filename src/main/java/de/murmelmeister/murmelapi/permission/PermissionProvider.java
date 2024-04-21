package de.murmelmeister.murmelapi.permission;

import de.murmelmeister.murmelapi.group.parent.GroupParent;
import de.murmelmeister.murmelapi.group.permission.GroupPermission;
import de.murmelmeister.murmelapi.user.parent.UserParent;
import de.murmelmeister.murmelapi.user.permission.UserPermission;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public record PermissionProvider(GroupParent groupParent, GroupPermission groupPermission,
                                 UserParent userParent, UserPermission userPermission) implements Permission {
    @Override
    public List<String> getPermissions(int userId) throws SQLException {
        List<String> permissions = new ArrayList<>(userPermission.getPermissions(userId));
        for (int parentId : userParent.getParentIds(userId))
            permissions.addAll(groupPermission.getAllPermissions(groupParent, parentId));
        return permissions;
    }

    @Override
    public boolean hasPermission(int userId, String permission) throws SQLException {
        List<String> permissions = new ArrayList<>(getPermissions(userId));
        if (permissions.contains("-" + permission)) return false;
        if (permissions.contains("*")) return true;
        return permissions.contains(permission);
    }
}
