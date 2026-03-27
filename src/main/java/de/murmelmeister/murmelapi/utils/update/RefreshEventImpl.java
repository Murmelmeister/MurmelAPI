package de.murmelmeister.murmelapi.utils.update;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

record RefreshEventImpl<K>(@NotNull String type, @Nullable K key) implements RefreshEvent<K> {
    public RefreshEventImpl {
        Objects.requireNonNull(type, "RefreshType cannot be null");
    }

    public RefreshEventImpl(@NotNull RefreshType type, @Nullable K key) {
        this(type.getName(), key);
    }
}
