package de.murmelmeister.murmelapi.color;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface PrefixColorProvider {
    void refreshCache();

    @Nullable PrefixColor findById(@Nullable String id);

    @NotNull List<PrefixColor> findAll();

    @Nullable PrefixColor create(@NotNull String id, @NotNull String color, boolean animated, int createdBy);

    int delete(@NotNull String id);

    @Nullable PrefixColor update(@NotNull String id, @NotNull String color, boolean animated, int changedBy);
}
