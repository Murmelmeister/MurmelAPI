package de.murmelmeister.murmelapi.utils.update;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface RefreshProvider extends AutoCloseable {
    void register(@NotNull RefreshListener cache);

    void unregister(@NotNull RefreshListener cache);

    <K> void fire(@NotNull RefreshEvent<K> event);

    <K> void fireSingle(@NotNull String cacheName, @Nullable K key);

    <K> void fireSingle(@NotNull String cacheName, @Nullable K key, @NotNull RefreshOrigin origin);

    <K> void fireSingle(@NotNull RefreshType type, @Nullable K key);

    <K> void fireSingle(@NotNull RefreshType type, @Nullable K key, @NotNull RefreshOrigin origin);

    void fireCache(@NotNull String cacheName);

    void fireCache(@NotNull String cacheName, @NotNull RefreshOrigin origin);

    void fireCache(@NotNull RefreshType type);

    void fireCache(@NotNull RefreshType type, @NotNull RefreshOrigin origin);

    void fireAll();

    void fireAll(@NotNull RefreshOrigin origin);

    @ApiStatus.Internal
    static @NotNull RefreshProvider of() {
        return new RefreshProviderImpl();
    }
}
