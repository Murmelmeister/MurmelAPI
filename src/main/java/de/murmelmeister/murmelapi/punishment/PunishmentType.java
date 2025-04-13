package de.murmelmeister.murmelapi.punishment;

import de.murmelmeister.murmelapi.MurmelAPI;
import de.murmelmeister.murmelapi.database.Database;

/**
 * The PunishmentType enum represents various types of punishments that can be applied.
 * Each punishment type is associated with a unique ID, a display name, and an indicator whether it is IP-based.
 * Additionally, this enum provides methods for retrieving punishment details, mapping from strings,
 * and setting up punishment type records in the database.
 */
public enum PunishmentType {
    BAN(1, "Ban", false),
    MUTE(2, "Mute", false),
    KICK(3, "Kick", false),
    CLAN(4, "Clan", false),
    IP_BAN(5, "IP-Ban", true),
    IP_MUTE(6, "IP-Mute", true),
    IP_KICK(7, "IP-Kick", true),
    IP_CLAN(8, "IP-Clan", true);
    public static final PunishmentType[] VALUES = values();
    private static final String TABLE_NAME = "punishment_types";
    private static final Database DATABASE = MurmelAPI.getDatabase();

    private final int id;
    private final String name;
    private final boolean ipType;

    /**
     * Constructs a new PunishmentType with the specified id, display name, and IP type flag.
     *
     * @param id     The unique identifier for the punishment type.
     * @param name   The display name of the punishment type.
     * @param ipType {@code true} if the punishment type is IP-based; {@code false} otherwise.
     */
    PunishmentType(int id, String name, boolean ipType) {
        this.id = id;
        this.name = name;
        this.ipType = ipType;
    }

    /**
     * Retrieves the unique identifier of this punishment type.
     *
     * @return The punishment type id.
     */
    public int getId() {
        return id;
    }

    /**
     * Retrieves the display name of this punishment type.
     *
     * @return The punishment type name.
     */
    public String getName() {
        return name;
    }

    /**
     * Checks whether this punishment type is IP-based.
     *
     * @return {@code true} if the punishment type is IP-based; {@code false} otherwise.
     */
    public boolean isIpType() {
        return ipType;
    }

    /**
     * Returns a PunishmentType enum constant corresponding to the specified name.
     * The comparison is case-insensitive.
     *
     * @param name The name to convert.
     * @return The matching PunishmentType if found; {@code null} otherwise.
     */
    public static PunishmentType fromString(String name) {
        for (PunishmentType type : VALUES)
            if (type.name().equalsIgnoreCase(name.toUpperCase()))
                return type;
        return null;
    }

    /**
     * Checks whether the specified punishment type already exists in the database.
     * This is a private method used internally during setup.
     *
     * @param type The PunishmentType to check.
     * @return {@code true} if the punishment type exists; {@code false} otherwise.
     */
    private static boolean exists(PunishmentType type) {
        return DATABASE.existsCallable(Procedure.GET.getName(), type.getId());
    }

    /**
     * Sets up the punishment types in the database.
     * This method creates the punishment_types table and loads the associated stored procedures.
     * Afterward, it iterates through all PunishmentType values and inserts them into the table if they do not exist.
     *
     * @param database The Database instance used for setup.
     */
    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "id INT PRIMARY KEY, " +
                                         "name VARCHAR(100) UNIQUE, " +
                                         "ipType BOOLEAN");
        Procedure.loadAll(database);
        for (PunishmentType type : VALUES)
            if (!exists(type))
                database.updateCallable(Procedure.CREATE.getName(), type.getId(), type.getName(), type.isIpType());
    }

    private enum Procedure {
        CREATE("punishmentTypes_create", "p_id INT, p_name VARCHAR(100), p_ipType BOOLEAN",
                "INSERT INTO [TABLE] VALUES (p_id, p_name, p_ipType);"),
        GET("punishmentTypes_get", "p_id INT", "SELECT * FROM [TABLE] WHERE id=p_id;");
        private static final Procedure[] VALUES = values();

        private final String name;
        private final String query;

        Procedure(String name, String input, String query) {
            this.name = name;
            this.query = Database.getProcedureQuery(name, input, query);
        }

        public String getName() {
            return name;
        }

        public String getQuery() {
            return query.replace("[TABLE]", TABLE_NAME);
        }

        private static void loadAll(Database database) {
            for (Procedure procedure : VALUES)
                database.update(procedure.getQuery());
        }
    }
}
