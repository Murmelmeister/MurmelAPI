package de.murmelmeister.murmelapi.utils.update;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The RefreshUtil class provides utility methods for managing refresh events and notifying listeners.
 * <p>
 * This class contains static methods for setting a refresh listener, marking as refreshed, and checking and resetting the refresh status.
 * It uses an AtomicBoolean to keep track of the refresh status and an ArrayList to store the registered listeners.
 * <p>
 * To use this class, first implement the RefreshListener interface with the desired functionality to be executed when a refresh occurs.
 * Call the setRefreshListener method to register the listener. When a refresh event happens, the onRefreshOccurred method of all registered listeners will be invoked.
 * The markAsRefreshed method is used to set the refresh status to true and invoke the onRefreshOccurred method of all registered listeners.
 * The checkAndResetRefreshStatus method is used to check the refresh status and reset it to false.
 * <p>
 * Note that the RefreshListener interface is marked as a functional interface, which means it can be used with lambda expressions.
 *
 * @see RefreshListener
 */
public final class RefreshUtil {
    private static final AtomicBoolean HAS_REFRESH = new AtomicBoolean(false);
    private static final List<RefreshListener> LISTENERS = new ArrayList<>();

    /**
     * Sets a refresh listener for the RefreshUtil class.
     * <p>
     * This method adds the specified listener to the list of registered listeners for refresh events.
     * When a refresh event occurs, the registered listeners will be notified by invoking their
     * {@link RefreshListener#onRefreshOccurred()} method.
     *
     * @param listener the refresh listener to be added
     * @see RefreshListener
     * @see RefreshUtil
     */
    public static void setRefreshListener(final RefreshListener listener) {
        LISTENERS.add(listener);
    }

    /**
     * Sets the refresh status as refreshed and triggers the {@link RefreshListener#onRefreshOccurred()} method for all registered listeners.
     * This method is used to indicate that a refresh event has occurred and notify all registered listeners about it.
     * <p>
     * The refresh status is managed by an {@link AtomicBoolean} variable that is initially set to false.
     * When {@link #markAsRefreshed} is called, the refresh status is set to true using {@link AtomicBoolean#set}.
     * <p>
     * For each listener in the registered listeners list, the {@link RefreshListener#onRefreshOccurred()} method is invoked.
     * This allows all registered listeners to perform the desired functionality when a refresh event occurs.
     *
     * @see RefreshUtil
     */
    public static void markAsRefreshed() {
        HAS_REFRESH.set(true);
        for (final RefreshListener listener : LISTENERS) listener.onRefreshOccurred();
    }

    /**
     * Checks the refresh status and resets it to false.
     * <p>
     * This method returns the current refresh status by invoking {@link AtomicBoolean#getAndSet} on the {@link AtomicBoolean} variable {@code HAS_REFRESH}.
     * The refresh status is then reset to false.
     *
     * @return the current refresh status before resetting it
     * @see RefreshUtil
     */
    public static boolean checkAndResetRefreshStatus() {
        return HAS_REFRESH.getAndSet(false);
    }
}
