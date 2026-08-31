package de.murmelmeister.murmelapi.utils.update;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CopyOnWriteArrayList;

final class RefreshProviderImpl implements RefreshProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshProviderImpl.class);

    private final CopyOnWriteArrayList<RefreshListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void register(@NotNull RefreshListener cache) {
        listeners.add(cache);
    }

    @Override
    public void unregister(@NotNull RefreshListener cache) {
        listeners.remove(cache);
    }

    @Override
    public <K> void fire(@NotNull RefreshEvent<K> event) {
        for (RefreshListener listener : listeners) {
            try {
                listener.onRefresh(event);
            } catch (Exception e) {
                LOGGER.error("Listener {} failed to handle refresh event: {}", listener, event, e);
            }
        }
    }

    @Override
    public <K> void fireSingle(@NotNull String cacheName, @Nullable K key) {
        fire(new RefreshEventImpl<>(cacheName, key, RefreshOrigin.LOCAL));
    }

    @Override
    public <K> void fireSingle(@NotNull String cacheName, @org.jspecify.annotations.Nullable K key, @NotNull RefreshOrigin origin) {
        fire(new RefreshEventImpl<>(cacheName, key, origin));
    }

    @Override
    public <K> void fireSingle(@NotNull RefreshType type, @Nullable K key) {
        fire(new RefreshEventImpl<>(type, key, RefreshOrigin.LOCAL));
    }

    @Override
    public <K> void fireSingle(@NotNull RefreshType type, @org.jspecify.annotations.Nullable K key, @NotNull RefreshOrigin origin) {
        fire(new RefreshEventImpl<>(type, key, origin));
    }

    @Override
    public void fireCache(@NotNull String cacheName) {
        fire(new RefreshEventImpl<>(cacheName, null, RefreshOrigin.LOCAL));
    }

    @Override
    public void fireCache(@NotNull String cacheName, @NotNull RefreshOrigin origin) {
        fire(new RefreshEventImpl<>(cacheName, null, origin));
    }

    @Override
    public void fireCache(@NotNull RefreshType type) {
        fire(new RefreshEventImpl<>(type, null, RefreshOrigin.LOCAL));
    }

    @Override
    public void fireCache(@NotNull RefreshType type, @NotNull RefreshOrigin origin) {
        fire(new RefreshEventImpl<>(type, null, origin));
    }

    @Override
    public void fireAll() {
        fire(new RefreshEventImpl<>(RefreshType.ALL, null, RefreshOrigin.LOCAL));
    }

    @Override
    public void fireAll(@NotNull RefreshOrigin origin) {
        fire(new RefreshEventImpl<>(RefreshType.ALL, null, origin));
    }

    @Override
    public void close() {
        for (RefreshListener listener : listeners) {
            if (listener instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (Exception e) {
                    LOGGER.warn("Failed to close listener {}", listener, e);
                }
            }
        }

        listeners.clear();
    }
}
