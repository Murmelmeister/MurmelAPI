package de.murmelmeister.murmelapi.utils.update;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface RefreshEvent<K> {
    @NotNull String type();

    @Nullable K key();
}
