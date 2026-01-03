package de.murmelmeister.murmelapi.utils.update;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.*;

public final class RefreshProviderImpl implements RefreshProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshProviderImpl.class);

    private final CopyOnWriteArrayList<RefreshListener> listeners = new CopyOnWriteArrayList<>();

    private final Set<RefreshEvent<?>> recentEvent = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final ScheduledExecutorService debouncer;

    public RefreshProviderImpl() {
        this.debouncer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "RefreshDebouncer");
            thread.setDaemon(true);
            return thread;
        });
    }

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
        if (!recentEvent.add(event)) return; // Prevent duplicate events
        if (!debouncer.isShutdown())
            debouncer.schedule(() -> recentEvent.remove(event), 100, TimeUnit.MILLISECONDS);

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
        fire(new RefreshEvent<>(cacheName, key));
    }

    @Override
    public <K> void fireSingle(@NotNull RefreshType type, @Nullable K key) {
        fire(new RefreshEvent<>(type, key));
    }

    @Override
    public void fireCache(@NotNull String cacheName) {
        fire(new RefreshEvent<>(cacheName, null));
    }

    @Override
    public void fireCache(@NotNull RefreshType type) {
        fire(new RefreshEvent<>(type, null));
    }

    @Override
    public void fireAll() {
        fire(new RefreshEvent<>(RefreshType.ALL, null));
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
        recentEvent.clear();
        debouncer.shutdown();

        try {
            if (!debouncer.awaitTermination(250, TimeUnit.MILLISECONDS)) {
                debouncer.shutdownNow();

                if (!debouncer.awaitTermination(250, TimeUnit.MILLISECONDS))
                    LOGGER.warn("Debouncer did not terminate in time");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
