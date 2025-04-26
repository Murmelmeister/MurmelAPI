package de.murmelmeister.murmelapi.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CacheManager<K, V> {
    private final ConcurrentHashMap<K, CacheValue<V>> cache = new ConcurrentHashMap<>();

    public V get(K key) {
        CacheValue<V> value = cache.get(key);
        if (value != null && !value.isExpired())
            return value.getValue();
        cache.remove(key);
        return null;
    }

    public void put(K key, V value, long ttl, TimeUnit unit) {
        cache.put(key, new CacheValue<>(value, ttl, unit));
    }

    public void remove(K key) {
        cache.remove(key);
    }

    public void clear() {
        cache.clear();
    }

    private static class CacheValue<V> {
        private final V value;
        private final long expiryTime;

        CacheValue(V value, long ttl, TimeUnit unit) {
            this.value = value;
            this.expiryTime = System.currentTimeMillis() + unit.toMillis(ttl);
        }

        V getValue() {
            return value;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
}
