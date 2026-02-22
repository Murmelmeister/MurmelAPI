package de.murmelmeister.murmelapi.participant;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public interface ParticipantProvider {
    void refreshCache();

    @Nullable Participant findByUserId(int userId);

    @Nullable Participant findByGroupId(int groupId);

    @NotNull
    @Unmodifiable
    List<Participant> findAll();

    @Nullable Participant create(Integer groupId, Integer userId);

    int delete(Integer groupId, Integer userId);
}
