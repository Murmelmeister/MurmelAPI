package de.murmelmeister.murmelapi.participant.parent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public interface ParticipantParentProvider {
    void refreshCache();

    @Nullable ParticipantParent findParent(int participantId, int parentId);

    @NotNull
    @Unmodifiable
    List<ParticipantParent> findParents(int participantId);

    @Nullable ParticipantParent add(int participantId, int parentId, long duration, int createdBy);

    int remove(int participantId, int parentId);

    int clear(int participantId);

    @Nullable ParticipantParent update(int participantId, int parentId, long duration, int changedBy);

    int loadExpired();
}
