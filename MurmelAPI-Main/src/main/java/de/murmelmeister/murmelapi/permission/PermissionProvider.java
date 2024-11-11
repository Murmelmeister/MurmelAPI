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
        boolean hasUniversalPermission = permissions.contains("*");

        for (String perm : permissions) {
            if (perm.startsWith("-") && wildcardMatch(perm.substring(1), permission)) return false;
            if (wildcardMatch(perm, permission)) return true;
        }
        return hasUniversalPermission;
    }

    private boolean wildcardMatch(String pattern, String input) {
        String[] parts = pattern.split("\\*");
        for (String part : parts) {
            int index = input.indexOf(part);
            if (index == -1) return false;
            input = input.substring(index + part.length());
        }
        return true;
    }
}
