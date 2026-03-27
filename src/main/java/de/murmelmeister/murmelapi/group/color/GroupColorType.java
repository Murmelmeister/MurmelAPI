package de.murmelmeister.murmelapi.group.color;

import org.jetbrains.annotations.NotNull;

import java.util.*;

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

    // fast lookup maps to avoid O(n) searches
    private static final Map<Integer, GroupColorType> BY_ID;
    private static final Map<String, GroupColorType> BY_NAME;

    static {
        Map<Integer, GroupColorType> idMap = new HashMap<>();
        Map<String, GroupColorType> nameMap = new HashMap<>();
        for (GroupColorType type : VALUES) {
            idMap.put(type.id, type);
            nameMap.put(type.name.toLowerCase(Locale.ROOT), type);
        }
        BY_ID = Collections.unmodifiableMap(idMap);
        BY_NAME = Collections.unmodifiableMap(nameMap);
    }

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

    /**
     * Fast lookup that returns an Optional for the given id.
     */
    public static @NotNull Optional<GroupColorType> fromId(int id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    /**
     * Fast lookup that returns an Optional for the given name (case-insensitive).
     */
    public static @NotNull Optional<GroupColorType> fromName(@NotNull String name) {
        Objects.requireNonNull(name, "name must not be null");
        return Optional.ofNullable(BY_NAME.get(name.toLowerCase(Locale.ROOT)));
    }

    @Override
    public String toString() {
        return name;
    }
}
