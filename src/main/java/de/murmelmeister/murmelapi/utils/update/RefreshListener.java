package de.murmelmeister.murmelapi.utils.update;

/**
 * The RefreshListener interface represents a functional interface that defines a single method
 * {@link #onRefreshOccurred()} for notifying when a refresh event has occurred.
 * <p>
 * This interface is typically used in conjunction with the {@link RefreshUtil} class, which provides
 * a way to manage refresh listeners and trigger the onRefreshOccurred event.
 * <p>
 * To use this interface, implement the onRefreshOccurred method with the desired functionality
 * to be executed when a refresh occurs. Then, register the listener using the {@link RefreshUtil#setRefreshListener(RefreshListener)}
 * method. When a refresh event happens, the onRefreshOccurred method of all registered listeners
 * will be invoked.
 * <p>
 * This interface is marked as a functional interface, which means it can be used with lambda expressions.
 *
 * @see RefreshUtil
 */
@FunctionalInterface
public interface RefreshListener {
    void onRefreshOccurred();
}
