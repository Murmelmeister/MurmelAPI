package de.murmelmeister.murmelapi.utils.update;

/**
 * Listener interface for cache refresh events.
 * Implement this interface to handle cache refresh notifications.
 */
@FunctionalInterface
public interface RefreshListener {
    /**
     * Called when a cache refresh occurs.
     *
     * @param cacheName The name of the cache that was refreshed.
     */
    void onRefreshOccurred(String cacheName);
}
