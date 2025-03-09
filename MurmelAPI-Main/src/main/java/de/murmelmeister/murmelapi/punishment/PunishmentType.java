package de.murmelmeister.murmelapi.punishment;

import de.murmelmeister.murmelapi.MurmelAPI;
import de.murmelmeister.murmelapi.database.Database;

/**
 * Represents the type of punishment.
 */
public enum PunishmentType {
    BAN(1, "Ban", false),
    MUTE(2, "Mute", false),
    IP_BAN(3, "IP-Ban", true);
    public static final PunishmentType[] VALUES = values();
    private static final String TABLE_NAME = "PunishmentTypes";
    private static final Database DATABASE = MurmelAPI.getDatabase();

    private final int id;
    private final String name;
    private final boolean typeIp;

    PunishmentType(int id, String name, boolean typeIp) {
        this.id = id;
        this.name = name;
        this.typeIp = typeIp;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isTypeIp() {
        return typeIp;
    }

    /**
     * Converts a string representation of a punishment type to its corresponding enum constant.
     *
     * @param value The string representation of the punishment type
     * @return The corresponding {@code PunishmentType} if a match is found, or {@code null} if no match exists
     */
    public static PunishmentType fromString(String value) {
        for (PunishmentType type : VALUES)
            if (type.name().equals(value)) return type;
        return null;
    }

    /**
     * Checks if a specified {@link PunishmentType} exists in the database.
     *
     * @param type The punishment type to check for existence in the database.
     * @return {@code true} if the specified punishment type exists in the database, {@code false} otherwise.
     */
    public static boolean exists(PunishmentType type) {
        return DATABASE.exists(Procedure.GET_TYPE.getName(), type.getId());
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "ID INT PRIMARY KEY, TypeName TEXT, TypeIP BOOL");
        Procedure.loadAll(database);
        for (PunishmentType type : VALUES)
            if (!exists(type))
                database.callUpdate(Procedure.CREATE_TYPE.getName(), type.getId(), type.getName(), type.isTypeIp());
    }


    private enum Procedure {
        CREATE_TYPE("PunishmentTypes_Create", "tid INT, tn TEXT, tip BOOL", "INSERT INTO [TABLE] VALUES (tid,tn,tip);"),
        GET_TYPE("PunishmentTypes_Get", "tid INT", "SELECT ID FROM [TABLE] WHERE ID=tid;");
        private static final Procedure[] VALUES = values();

        private final String name;
        private final String query;

        Procedure(final String name, final String input, final String query) {
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
            for (Procedure procedure : VALUES) database.update(procedure.getQuery());
        }
    }
}
