package de.murmelmeister.murmelapi.utils.update;

/**
 * Listener interface for cache refresh events.
 * Implement this interface to handle cache refresh notifications.
 */
@FunctionalInterface
public interface RefreshListener {
    void onRefresh(RefreshEvent<?> event);
}
