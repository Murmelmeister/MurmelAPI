package de.murmelmeister.murmelapi.clan;

import java.util.List;
import java.util.UUID;

public interface ClanProvider {
    void refreshCache();

    Clan findById(UUID id);

    Clan findByName(String name);

    Clan findByOwner(int ownerId);

    List<Clan> findAll();

    Clan create(String name, String tag, String sign, String description, int ownerId, int createdBy);

    int delete(UUID id);

    Clan update(UUID id, String name, String tag, String sign, String description, int ownerId, int changedBy);
}
