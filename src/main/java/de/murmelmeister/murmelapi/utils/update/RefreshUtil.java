package de.murmelmeister.murmelapi.utils.update;

import de.murmelmeister.murmelapi.utils.MurmelCache;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.*;

public final class RefreshUtil {
    private static final CopyOnWriteArrayList<MurmelCache> LISTENERS = new CopyOnWriteArrayList<>();

    private static final Set<RefreshEvent<?>> RECENT = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final ScheduledExecutorService DEBOUNCER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "RefreshDebouncer");
        thread.setDaemon(true);
        return thread;
    });

    public static void register(MurmelCache cache) {
        LISTENERS.add(cache);
    }

    public static void unregister(MurmelCache cache) {
        LISTENERS.remove(cache);
    }

    public static boolean isRegistered(MurmelCache cache) {
        return LISTENERS.contains(cache);
    }

    public static Collection<MurmelCache> getListeners() {
        return Collections.unmodifiableCollection(LISTENERS);
    }

    public static <K> void fire(RefreshEvent<K> event) {
        if (!RECENT.add(event)) return; // Prevent duplicate events
        DEBOUNCER.schedule(() -> RECENT.remove(event), 100, TimeUnit.MILLISECONDS);

        for (RefreshListener listener : LISTENERS) {
            try {
                listener.onRefresh(event);
            } catch (Exception e) {
                LoggerFactory.getLogger(RefreshListener.class).error("Listener {} failed to handle refresh event: {}", listener, event, e);
            }
        }
    }

    public static <K> void fireSingle(String cacheName, K key) {
        fire(new RefreshEvent<>(cacheName, key));
    }

    public static <K> void fireSingle(RefreshType type, K key) {
        fire(new RefreshEvent<>(type, key));
    }

    public static void fireCache(String cacheName) {
        fire(new RefreshEvent<>(cacheName, null));
    }

    public static void fireCache(RefreshType type) {
        fire(new RefreshEvent<>(type, null));
    }

    public static void fireAll() {
        fire(new RefreshEvent<>(RefreshType.ALL, null));
    }
}
