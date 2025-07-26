package de.murmelmeister.murmelapi.utils.update;

public final class RefreshEvent<K> {
    private final String type;
    private final K key;

    public RefreshEvent(String type, K key) {
        this.type = type;
        this.key = key;
    }

    public RefreshEvent(RefreshType type, K key) {
        this.type = type.getName();
        this.key = key;
    }

    public String getType() {
        return type;
    }

    public K getKey() {
        return key;
    }
}
