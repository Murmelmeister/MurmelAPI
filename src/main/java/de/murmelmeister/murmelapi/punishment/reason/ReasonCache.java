package de.murmelmeister.murmelapi.punishment.reason;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class ReasonCache {
    private final ConcurrentHashMap<Integer, Reason> cache = new ConcurrentHashMap<>();

    public Reason get(int id) {
        return cache.get(id);
    }

    public void put(Reason reason) {
        cache.put(reason.getId(), reason);
    }

    public void remove(int id) {
        cache.remove(id);
    }

    public void clear() {
        cache.clear();
    }

    public boolean isEmpty() {
        return cache.isEmpty();
    }

    public List<Reason> getReasons() {
        return List.copyOf(cache.values());
    }
}
