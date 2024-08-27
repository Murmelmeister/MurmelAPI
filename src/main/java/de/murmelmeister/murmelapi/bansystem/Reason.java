package de.murmelmeister.murmelapi.bansystem;

public sealed interface Reason permits ReasonProvider {
    boolean exists(int id);

    void add(String reason);

    void remove(int id);

    void update(int id, String reason);

    String get(int id);
}
