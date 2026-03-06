package de.murmelmeister.murmelapi.color;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Optional;

public interface PrefixColorProvider {
    void refreshCache();

    @NotNull Optional<PrefixColor> findById(@NotNull String id);

    @NotNull
    @Unmodifiable
    List<PrefixColor> findAll();

    @NotNull Optional<PrefixColor> upsert(@NotNull String id, @NotNull String color, boolean animated, int executorId);

    @NotNull Optional<PrefixColor> upsert(@NotNull PrefixColor prefixColor, int executorId);

    int delete(@NotNull String id);
}
