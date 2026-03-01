package de.murmelmeister.murmelapi.utils.update;

/**
 * Enum representing different types of refresh operations.
 * <p>
 * This enum is used to specify the type of refresh operation that should be performed.
 * It includes options for refreshing all data, refreshing global data, and refreshing permissions.
 */
public enum RefreshType {
    ALL("all"),
    SETTINGS("settings"),
    MESSAGES("messages"),
    LANGUAGES("languages"),
    PUNISHMENT_REASONS("punishment_reasons"),
    PUNISHMENT_AUDITS("punishment_audits"),
    PUNISHMENT_USERS("punishment_users"),
    PUNISHMENT_IPS("punishment_ips"),
    USERS("users"),
    USER_LOGINS("user_logins"),
    USER_SESSIONS("user_sessions"),
    GROUPS("groups"),
    GROUP_COLORS("group_colors"),
    PERMISSIONS("permissions"),
    PARENTS("parents"),
    CLANS("clans"),
    CLAN_MEMBERS("clan_members"),
    CLAN_GROUPS("clan_groups"),
    CLAN_PARENTS("clan_parents"),
    CLAN_PERMISSIONS("clan_permissions"),
    PREFIX_COLORS("prefix_colors"),
    USER_PREFIX_COLORS("user_prefix_colors"),
    INVENTORY_TYPES("inventory_types"),
    USER_INVENTORIES("user_inventories"),
    USER_STATS("user_stats"),
    MAINTENANCES("maintenances"),
    MAINTENANCE_WHITELISTS("maintenance_whitelists"),
    USER_EXCUSES("user_excuses"),
    SINGLE_USER("single_user"),
    SINGLE_USER_LOGIN("single_user_login"),
    SINGLE_USER_SESSION("single_user_session"),
    SINGLE_PUNISHMENT_REASON("single_punishment_reason"),
    SINGLE_PUNISHMENT_AUDIT("single_punishment_audit"),
    SINGLE_PUNISHMENT_USER("single_punishment_user"),
    SINGLE_PUNISHMENT_IP("single_punishment_ip"),
    SINGLE_MESSAGE("single_message"),
    SINGLE_LANGUAGE("single_language"),
    SINGLE_GROUP("single_group"),
    SINGLE_GROUP_COLOR("single_group_color"),
    SINGLE_PERMISSION("single_permission"),
    SINGLE_PARENT("single_parent"),
    SINGLE_SETTING("single_setting"),
    SINGLE_CLAN("single_clan"),
    SINGLE_CLAN_MEMBER("single_clan_member"),
    SINGLE_CLAN_GROUP("single_clan_group"),
    SINGLE_CLAN_PARENT("single_clan_parent"),
    SINGLE_CLAN_PERMISSION("single_clan_permission"),
    SINGLE_PREFIX_COLOR("single_prefix_color"),
    SINGLE_USER_PREFIX_COLOR("single_user_prefix_color"),
    SINGLE_INVENTORY_TYPE("single_inventory_type"),
    SINGLE_USER_INVENTORY("single_user_inventory"),
    SINGLE_USER_STAT("single_user_stat"),
    SINGLE_MAINTENANCE("single_maintenance"),
    SINGLE_MAINTENANCE_WHITELIST("single_maintenance_whitelist"),
    SINGLE_USER_EXCUSE("single_user_excuse")
    ;
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
