package de.murmelmeister.murmelapi.group.color;

/**
 * The GroupColorType enum represents the types of group color customization
 * categories. Each type corresponds to a specific category of group color settings,
 * particularly for Chat, Tab, or Team configurations.
 * <p>
 * This enum is used as a parameter in methods of the {@link GroupColor} interface
 * and its implementation {@link GroupColorProvider} to specify which
 * category (e.g., Chat, Tab, or Team) is being accessed or modified.
 * <p>
 * Each constant in the enum holds an associated name that describes
 * the category and is used for accessing the corresponding database column or functionality.
 */
public enum GroupColorType {
    CHAT("chat"),
    CHAT_MESSAGE("chatMessage"),
    TAB("tab"),
    TEAM("team");
    private final String name;

    GroupColorType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
