package de.murmelmeister.murmelapi.participant.permission;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public interface ParticipantPermissionProvider {
    void refreshCache();

    @Nullable ParticipantPermission findPermission(int participantId, @NotNull String permission);

    @NotNull
    @Unmodifiable
    List<ParticipantPermission> findPermissions(int participantId);

    @Nullable ParticipantPermission add(int participantId, @NotNull String permission, long duration, int createdBy);

    int remove(int participantId, @NotNull String permission);

    int clear(int participantId);

    @Nullable ParticipantPermission update(int participantId, @NotNull String permission, long duration, int changedBy);

    int loadExpired();
}
