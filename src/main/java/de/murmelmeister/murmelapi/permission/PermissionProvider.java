package de.murmelmeister.murmelapi.permission;

import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.user.User;

import java.util.*;

/**
 * The PermissionProvider class provides methods to manage and check permissions for users and groups.
 * It implements the Permission interface.
 */
public record PermissionProvider(Group group, User user) implements Permission {
    // TODO: All permissions of user -> cached

    @Override
    public List<String> getPermissions(int userId) {
        Set<String> permissions = new LinkedHashSet<>(user.getPermission().getPermissions(userId));
        for (int parentId : user.getParent().getParentIds(userId))
            permissions.addAll(group.getPermission().getAllPermissions(group.getParent(), parentId));
        return new ArrayList<>(permissions);
    }

    @Override
    public boolean hasPermission(int userId, String permission) {
        Set<String> permissions = new LinkedHashSet<>(getPermissions(userId));
        if (permissions.contains("-" + permission)) return false;
        if (permissions.contains("*")) return true;
        // TODO: With wildcard
        return permissions.contains(permission);
    }

    @Override
    public boolean hasPermission(UUID uuid, String permission) {
        return hasPermission(user.getId(uuid), permission);
    }

    @Override
    public int loadExpired() {
        int userRows = user.loadExpired();
        int groupRows = group.loadExpired();
        return userRows + groupRows;
    }
}
