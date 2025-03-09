package de.murmelmeister.murmelapi.permission;

import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.user.User;

import java.util.*;

public record PermissionProvider(Group group, User user) implements Permission {
    @Override
    public List<String> getPermissions(int userId) {
        Set<String> permissions = new LinkedHashSet<>(user.getPermission().getPermissions(userId));
        List<Integer> parentIds = user.getParent().getParentIds(userId);

        for (int i = parentIds.size() - 1; i >= 0; i--) {
            int parentId = parentIds.get(i);
            permissions.addAll(group.getPermission().getAllPermissions(group.getParent(), parentId));
        }
        return new LinkedList<>(permissions);
    }

    @Override
    public boolean hasPermission(UUID uuid, String permission) {
        return hasPermission(user.getId(uuid), permission);
    }

    @Override
    public boolean hasPermission(int userId, String permission) {
        Set<String> permissions = new LinkedHashSet<>(getPermissions(userId));
        boolean hasUniversalPermission = permissions.contains("*");

        for (String perm : permissions) {
            if (perm.startsWith("-") && wildcardMatch(perm.substring(1), permission)) return false;
            if (wildcardMatch(perm, permission)) return true;
        }
        return hasUniversalPermission;
    }

    private boolean wildcardMatch(String pattern, String input) {
        if (pattern.isEmpty()) return input.isEmpty();
        if (pattern.equals("*")) return true;

        String[] parts = pattern.split("\\*");
        int endIndex = input.length();

        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i];
            int index = input.lastIndexOf(part, endIndex);
            if (index == -1 || index + part.length() < endIndex) return false;
            endIndex = index;
        }
        return true;
    }
}
