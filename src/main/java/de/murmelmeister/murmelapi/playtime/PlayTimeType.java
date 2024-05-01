package de.murmelmeister.murmelapi.playtime;

public enum PlayTimeType {
    SECONDS("Seconds"),
    MINUTES("Minutes"),
    HOURS("Hours"),
    DAYS("Days"),
    YEARS("Years");
    private final String name;

    PlayTimeType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
