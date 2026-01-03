package de.murmelmeister.murmelapi.utils.update;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record RefreshEvent<K>(@NotNull String type, @Nullable K key) {
    public RefreshEvent {
        Objects.requireNonNull(type, "RefreshType cannot be null");
    }

    public RefreshEvent(@NotNull RefreshType type, @Nullable K key) {
        this(type.getName(), key);
    }
}
