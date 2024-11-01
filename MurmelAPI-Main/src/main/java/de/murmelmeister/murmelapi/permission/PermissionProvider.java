package de.murmelmeister.murmelapi.permission;

import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.user.User;

import java.util.*;

public record PermissionProvider(Group group, User user) implements Permission {
    @Override
    public List<String> getPermissions(int userId) {
        Set<String> permissions = new LinkedHashSet<>(user.getPermission().getPermissions(userId));
        for (int parentId : user.getParent().getParentIds(userId))
            permissions.addAll(group.getPermission().getAllPermissions(group.getParent(), parentId));
        return new ArrayList<>(permissions);
    }

    @Override
    public boolean hasPermission(UUID uuid, String permission) {
        Set<String> permissions = new LinkedHashSet<>(getPermissions(user.getId(uuid)));
        if (permissions.contains("-" + permission)) return false;
        if (permissions.contains("*")) return true;
        return permissions.contains(permission);
    }
}
