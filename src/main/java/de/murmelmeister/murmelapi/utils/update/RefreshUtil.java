package de.murmelmeister.murmelapi.utils.update;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class RefreshUtil {
    private static final List<RefreshListener> LISTENERS = new ArrayList<>();
    private static final ReadWriteLock LOCK = new ReentrantReadWriteLock();
    private static final Lock READ_LOCK = LOCK.readLock();
    private static final Lock WRITE_LOCK = LOCK.writeLock();

    public static void setRefreshListener(final RefreshListener listener) {
        WRITE_LOCK.lock();
        try {
            LISTENERS.add(listener);
        } finally {
            WRITE_LOCK.unlock();
        }
    }

    public static void markAsRefreshed(String cacheName) {
        READ_LOCK.lock();
        try {
            for (final RefreshListener listener : LISTENERS)
                listener.onRefreshOccurred(cacheName);
        } finally {
            READ_LOCK.unlock();
        }
    }

    public static void globalRefresh() {
        markAsRefreshed("global");
    }
}
