package de.murmelmeister.murmelapi.time;

public enum PlayTimeType {
    SECONDS("Seconds", 1),
    MINUTES("Minutes", 60),
    HOURS("Hours", 3600),
    DAYS("Days", 86400),
    YEARS("Years", 31536000);
    public static final PlayTimeType[] VALUES = values();

    private final String name;
    private final int multiplier;

    PlayTimeType(String name, int multiplier) {
        this.name = name;
        this.multiplier = multiplier;
    }

    public String getName() {
        return name;
    }

    public int toSeconds(int time) {
        return time * multiplier;
    }

    public int fromSeconds(int seconds) {
        return seconds / multiplier;
    }
}
