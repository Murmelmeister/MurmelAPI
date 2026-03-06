package de.murmelmeister.murmelapi.user.color;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Optional;

public interface UserPrefixColorProvider {
    void refreshCache();

    @NotNull Optional<UserPrefixColor> findById(int userId, @NotNull String colorId);

    @NotNull Optional<UserPrefixColor> findActiveById(int userId);

    @NotNull
    @Unmodifiable
    List<UserPrefixColor> findByUserId(int userId);

    @NotNull Optional<UserPrefixColor> upsert(int userId, @NotNull String colorId, boolean active);

    int delete(int userId, @NotNull String colorId);
}
