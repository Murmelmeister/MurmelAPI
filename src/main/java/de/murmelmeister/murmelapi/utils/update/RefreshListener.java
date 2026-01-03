package de.murmelmeister.murmelapi.utils.update;

import org.jetbrains.annotations.NotNull;

/**
 * Listener interface for cache refresh events.
 * Implement this interface to handle cache refresh notifications.
 */
@FunctionalInterface
public interface RefreshListener {
    void onRefresh(@NotNull RefreshEvent<?> event);
}
