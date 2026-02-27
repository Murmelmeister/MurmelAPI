package de.murmelmeister.murmelapi.permission;

import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.GroupProvider;
import de.murmelmeister.murmelapi.permission.parent.Parent;
import de.murmelmeister.murmelapi.permission.parent.ParentProvider;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static de.murmelmeister.murmelapi.MurmelAPI.DEFAULT_GROUP_ID;

public record PermissionService(ParentProvider parentProvider, PermissionProvider permissionProvider,
                                GroupProvider groupProvider, UserProvider userProvider) {

    public PermissionService {
        Objects.requireNonNull(parentProvider, "parentProvider must not be null");
        Objects.requireNonNull(permissionProvider, "permissionProvider must not be null");
        Objects.requireNonNull(groupProvider, "groupProvider must not be null");
        Objects.requireNonNull(userProvider, "userProvider must not be null");
    }

    public @Nullable Group getHighestGroup(@NotNull PermissionTarget target) {
        List<Integer> parentIds = parentProvider.findParents(target)
                .stream()
                .map(Parent::parentId)
                .toList();
        if (parentIds.isEmpty())
            return groupProvider.findById(DEFAULT_GROUP_ID);

        return parentIds.stream()
                .map(groupProvider::findById)
                .filter(Objects::nonNull)
                .min(Comparator.comparingInt(Group::priority))
                .orElse(groupProvider.findById(DEFAULT_GROUP_ID));
    }

    public @Nullable String getName(@NotNull PermissionTarget target) {
        if (target.type() == PermissionTarget.TargetType.USER) {
            User user = userProvider.findById(target.id());
            return user == null ? null : user.username();
        } else {
            Group group = groupProvider.findById(target.id());
            return group == null ? null : group.groupName();
        }
    }

    public @NotNull List<Parent> getParents(@NotNull PermissionTarget target) {
        Set<Integer> visitedGroups = new LinkedHashSet<>();
        ArrayDeque<PermissionTarget> queue = new ArrayDeque<>();
        Map<String, Parent> parentMap = new LinkedHashMap<>();

        if (target.type() == PermissionTarget.TargetType.GROUP)
            visitedGroups.add(target.id());
        queue.add(target);

        while (!queue.isEmpty()) {
            PermissionTarget currentTarget = queue.removeFirst();

            for (Parent parent : parentProvider.findParents(currentTarget)) {
                if (parent.isExpired()) continue;

                String prefix = currentTarget.type() == PermissionTarget.TargetType.GROUP ? "group:" : "user:";
                String key = prefix + currentTarget.id() + ":" + parent.parentId();
                parentMap.putIfAbsent(key, parent);

                if (visitedGroups.add(parent.parentId()))
                    queue.addLast(PermissionTarget.group(parent.parentId()));
            }
        }
        return new ArrayList<>(parentMap.values());
    }

    private @NotNull Set<PermissionTarget> resolveHierarchy(@NotNull PermissionTarget target) {
        Set<PermissionTarget> hierarchy = new LinkedHashSet<>();
        hierarchy.add(target);

        for (Parent parent : getParents(target))
            hierarchy.add(PermissionTarget.group(parent.parentId()));
        return hierarchy;
    }

    public @NotNull List<Permission> getPermissions(@NotNull PermissionTarget target) {
        Set<PermissionTarget> hierarchy = resolveHierarchy(target);
        Map<Integer, Permission> permissionMap = new LinkedHashMap<>();

        for (PermissionTarget currentTarget : hierarchy) {
            for (Permission permission : permissionProvider.findPermissions(currentTarget)) {
                if (permission.isExpired()) continue;
                permissionMap.putIfAbsent(permission.id(), permission);
            }
        }
        return new ArrayList<>(permissionMap.values());
    }

    public boolean hasPermission(@NotNull PermissionTarget target, @NotNull String permission) {
        if (permission.isBlank()) return false;

        List<Permission> allPermissions = getPermissions(target);
        if (allPermissions.isEmpty()) return false;

        Set<String> permissions = new LinkedHashSet<>();
        allPermissions.forEach(entry -> permissions.add(entry.permission()));

        if (permissions.contains("-" + permission)) return false;

        for (String negative : permissions) {
            if (negative.startsWith("-") && negative.endsWith(".*")) {
                String prefix = negative.substring(1, negative.length() - 1);
                if (permission.startsWith(prefix)) return false;
            }
        }

        if (permissions.contains("*")) return true;
        if (permissions.contains(permission)) return true;

        for (String positive : permissions) {
            if (positive.endsWith(".*")) {
                String prefix = positive.substring(0, positive.length() - 1);
                if (permission.startsWith(prefix)) return true;
            }
        }
        return false;
    }

    public int loadExpired() {
        int parents = parentProvider.loadExpired();
        int permissions = permissionProvider.loadExpired();
        return parents + permissions;
    }
}
