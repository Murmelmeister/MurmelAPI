package de.murmelmeister.murmelapi.group.color;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The GroupColorType enum represents the types of group color customization
 * categories. Each type corresponds to a specific category of group color settings,
 * particularly for Chat, Tab, or Team configurations.
 * <p>
 * This enum is used as a parameter in methods of the {@link GroupColorProvider} interface
 * and its implementation {@link GroupColorProviderImpl} to specify which
 * category (e.g., Chat, Tab, or Team) is being accessed or modified.
 * <p>
 * Each constant in the enum holds an associated name that describes
 * the category and is used for accessing the corresponding database column or functionality.
 */
public enum GroupColorType {
    CHAT_PREFIX(1, "chat_prefix"),
    CHAT_SUFFIX(2, "chat_suffix"),
    CHAT_COLOR(3, "chat_color"),
    CHAT_MESSAGE(4, "chat_message"),
    TAB_PREFIX(5, "tab_prefix"),
    TAB_SUFFIX(6, "tab_suffix"),
    TAB_COLOR(7, "tab_color"),
    TEAM_PREFIX(8, "team_prefix"),
    TEAM_SUFFIX(9, "team_suffix"),
    TEAM_COLOR(10, "team_color");
    private static final GroupColorType[] VALUES = values();

    private final int id;
    private final String name;

    GroupColorType(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static @Nullable GroupColorType fromId(int id) {
        for (GroupColorType type : VALUES)
            if (type.getId() == id)
                return type;
        return null;
    }

    public static @Nullable GroupColorType fromName(@NotNull String name) {
        for (GroupColorType type : VALUES)
            if (type.getName().equalsIgnoreCase(name))
                return type;
        return null;
    }
}
