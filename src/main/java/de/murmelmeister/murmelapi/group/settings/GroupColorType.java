package de.murmelmeister.murmelapi.group.settings;

/**
 * Group color type to manage the group color type.
 */
public enum GroupColorType {
    CHAT("Chat"),
    TAB("Tab"),
    TAG("Tag");
    private final String name;

    GroupColorType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
