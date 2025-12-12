package de.murmelmeister.murmelapi.punishment.type;

public enum PunishmentType {
    BAN(1, "Ban", false),
    MUTE(2, "Mute", false),
    KICK(3, "Kick", false),
    CLAN(4, "Clan", false),
    IP_BAN(5, "IP-Ban", true),
    IP_MUTE(6, "IP-Mute", true),
    IP_KICK(7, "IP-Kick", true),
    IP_CLAN(8, "IP-Clan", true);
    private static final PunishmentType[] VALUES = values();

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
}
