package de.murmelmeister.murmelapi.utils.update;

public record RefreshEvent<K>(String type, K key) {
    public RefreshEvent(RefreshType type, K key) {
        this(type.getName(), key);
    }
}
