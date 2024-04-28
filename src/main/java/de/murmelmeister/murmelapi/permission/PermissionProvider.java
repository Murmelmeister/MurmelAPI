package de.murmelmeister.murmelapi.permission;

import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.user.User;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record PermissionProvider(Group group, User user) implements Permission {
    @Override
    public List<String> getPermissions(int userId) throws SQLException {
        List<String> permissions = new ArrayList<>(user.getPermission().getPermissions(userId));
        for (int parentId : user.getParent().getParentIds(userId))
            permissions.addAll(group.getPermission().getAllPermissions(group.getParent(), parentId));
        return permissions;
    }

    @Override
    public boolean hasPermission(UUID uuid, String permission) throws SQLException {
        List<String> permissions = new ArrayList<>(getPermissions(user.getId(uuid)));
        if (permissions.contains("-" + permission)) return false;
        if (permissions.contains("*")) return true;
        return permissions.contains(permission);
    }
}
