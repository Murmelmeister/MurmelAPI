package de.murmelmeister.murmelapi.punishment.type;

import org.jetbrains.annotations.NotNull;

import java.util.*;

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

    private static final Map<Integer, PunishmentType> BY_ID;
    private static final Map<String, PunishmentType> BY_NAME;

    static {
        Map<Integer, PunishmentType> idMap = new HashMap<>();
        Map<String, PunishmentType> nameMap = new HashMap<>();
        for (PunishmentType type : VALUES) {
            idMap.put(type.id, type);
            nameMap.put(type.name.toLowerCase(Locale.ROOT), type);
        }
        BY_ID = Collections.unmodifiableMap(idMap);
        BY_NAME = Collections.unmodifiableMap(nameMap);
    }

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

    public static @NotNull Optional<PunishmentType> fromId(int id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    public static @NotNull Optional<PunishmentType> fromName(@NotNull String name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(BY_NAME.get(name.toLowerCase(Locale.ROOT)));
    }

    @Override
    public String toString() {
        return name;
    }
}
