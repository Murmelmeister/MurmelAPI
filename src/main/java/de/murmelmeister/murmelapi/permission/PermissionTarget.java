package de.murmelmeister.murmelapi.permission;

import org.jetbrains.annotations.NotNull;

public record PermissionTarget(TargetType type, int id) {
    public enum TargetType {
        USER, GROUP
    }

    public static @NotNull PermissionTarget user(int id) {
        return new PermissionTarget(TargetType.USER, id);
    }

    public static @NotNull PermissionTarget group(int id) {
        return new PermissionTarget(TargetType.GROUP, id);
    }
}
