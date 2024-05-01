package de.murmelmeister.murmelapi.permission;

import java.util.List;

public sealed interface Parents permits ParentsProvider {
    boolean existsParent(int id, int parentId);

    void addParent(int id, int creatorId, int parentId, long time);

    void removeParent(int id, int parentId);

    void clearParent(int id);

    List<Integer> getParentIds(int id);
}
