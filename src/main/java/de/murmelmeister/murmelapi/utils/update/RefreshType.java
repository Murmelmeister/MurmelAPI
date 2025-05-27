package de.murmelmeister.murmelapi.utils.update;

/**
 * Enum representing different types of refresh operations.
 * <p>
 * This enum is used to specify the type of refresh operation that should be performed.
 * It includes options for refreshing all data, refreshing global data, and refreshing permissions.
 */
public enum RefreshType {
    ALL("global"),
    GLOBAL("global"),
    PERMISSIONS("permissions"),
    MESSAGES("messages"),
    LANGUAGES("languages"),
    REASONS("reasons"),;
    private static final RefreshType[] VALUES = values();

    private final String name;

    RefreshType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static RefreshType fromName(String name) {
        for (RefreshType type : VALUES)
            if (type.getName().equalsIgnoreCase(name))
                return type;
        return null;
    }
}
