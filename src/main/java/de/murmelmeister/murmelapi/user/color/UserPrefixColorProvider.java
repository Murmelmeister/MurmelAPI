package de.murmelmeister.murmelapi.user.color;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface UserPrefixColorProvider {
    void refreshCache();

    @Nullable UserPrefixColor findById(int userId, @NotNull String colorId);

    @NotNull List<UserPrefixColor> findAll();

    @Nullable UserPrefixColor create(int userId, @NotNull String colorId, boolean active);

    int delete(int userId, @NotNull String colorId);

    @Nullable UserPrefixColor update(int userId, @NotNull String colorId, boolean active);
}
