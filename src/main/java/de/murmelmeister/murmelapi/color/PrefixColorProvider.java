package de.murmelmeister.murmelapi.color;

import java.util.List;

public interface PrefixColorProvider {
    void refreshCache();

    PrefixColor findById(String id);

    List<PrefixColor> findAll();

    PrefixColor create(String id, String color, boolean animated, int createdBy);

    int delete(String id);

    PrefixColor update(String id, String color, boolean animated, int changedBy);
}
