package de.murmelmeister.murmelapi.user.excuse;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.time.LocalDateTime;
import java.util.List;

public interface UserExcuseProvider {
    void refreshCache();

    @Nullable UserExcuse findById(int id);

    @NotNull
    @Unmodifiable
    List<UserExcuse> findByUserId(int userId);

    @NotNull @Unmodifiable List<UserExcuse> findAll();

    @Nullable UserExcuse create(int userId, @NotNull LocalDateTime startAt, @NotNull LocalDateTime endAt, @Nullable String reason, int createdBy);

    @Nullable UserExcuse update(int id, @NotNull LocalDateTime startAt, @NotNull LocalDateTime endAt, @Nullable String reason, int changedBy);
}
