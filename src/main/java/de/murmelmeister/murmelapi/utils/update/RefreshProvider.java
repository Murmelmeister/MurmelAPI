package de.murmelmeister.murmelapi.utils.update;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface RefreshProvider extends AutoCloseable {
    void register(@NotNull RefreshListener cache);

    void unregister(@NotNull RefreshListener cache);

    <K> void fire(@NotNull RefreshEvent<K> event);

    <K> void fireSingle(@NotNull String cacheName, @Nullable K key);

    <K> void fireSingle(@NotNull RefreshType type, @Nullable K key);

    void fireCache(@NotNull String cacheName);

    void fireCache(@NotNull RefreshType type);

    void fireAll();

    @ApiStatus.Internal
    static @NotNull RefreshProvider of() {
        return new RefreshProviderImpl();
    }
}
