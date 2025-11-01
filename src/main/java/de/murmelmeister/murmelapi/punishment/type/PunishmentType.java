package de.murmelmeister.murmelapi.punishment.type;

import de.murmelmeister.library.database.Database;

public enum PunishmentType {
    BAN(1, "Ban", false),
    MUTE(2, "Mute", false),
    KICK(3, "Kick", false),
    CLAN(4, "Clan", false),
    IP_BAN(5, "IP-Ban", true),
    IP_MUTE(6, "IP-Mute", true),
    IP_KICK(7, "IP-Kick", true),
    IP_CLAN(8, "IP-Clan", true);
    private static final String TABLE_NAME = "punishment_types";
    public static final PunishmentType[] VALUES = values();

    private final int id;
    private final String name;
    private final boolean ipType;

    PunishmentType(int id, String name, boolean ipType) {
        this.id = id;
        this.name = name;
        this.ipType = ipType;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isIpType() {
        return ipType;
    }

    public static PunishmentType fromId(int id) {
        for (PunishmentType type : VALUES)
            if (type.getId() == id)
                return type;
        return null;
    }

    public static PunishmentType fromName(String name) {
        for (PunishmentType type : VALUES)
            if (type.getName().equalsIgnoreCase(name))
                return type;
        return null;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "id INT PRIMARY KEY, " +
                "name VARCHAR(100) NOT NULL UNIQUE, " +
                "ip_type BOOLEAN NOT NULL DEFAULT FALSE"
        );
    }

    public static void createDefaultTypes(Database database) {
        String sql = "INSERT IGNORE INTO " + TABLE_NAME + " (id, name, ip_type) VALUES (?, ?, ?)";
        for (PunishmentType type : VALUES)
            database.update(sql, stmt -> {
                stmt.setInt(1, type.getId());
                stmt.setString(2, type.getName());
                stmt.setBoolean(3, type.isIpType());
            });
    }
}
