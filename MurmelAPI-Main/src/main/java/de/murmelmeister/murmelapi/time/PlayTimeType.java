package de.murmelmeister.murmelapi.time;

/**
 * Represents different units of time that can be used to measure or convert play time.
 * Each unit has a name and a multiplier that defines its relationship to seconds.
 * <p>
 * This enum provides methods for converting time values between the specific units
 * and seconds, allowing for flexibility in handling time-related operations.
 */
public enum PlayTimeType {
    SECONDS("Seconds", 1),
    MINUTES("Minutes", 60),
    HOURS("Hours", 3600),
    DAYS("Days", 86400),
    YEARS("Years", 31536000);

    private final String name;
    private final int multiplier;

    PlayTimeType(String name, int multiplier) {
        this.name = name;
        this.multiplier = multiplier;
    }

    public String getName() {
        return name;
    }

    public int getMultiplier() {
        return multiplier;
    }

    public int toSeconds(int time) {
        return time * multiplier;
    }

    public int fromSeconds(int seconds) {
        return seconds / multiplier;
    }
}
