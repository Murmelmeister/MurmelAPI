package de.murmelmeister.murmelapi.permission;

import java.util.List;

public final class ParentsProvider implements Parents {
    @Override
    public boolean existsParent(int id, int parentId) {
        return false;
    }

    @Override
    public void addParent(int id, int creatorId, int parentId, long time) {

    }

    @Override
    public void removeParent(int id, int parentId) {

    }

    @Override
    public void clearParent(int id) {

    }

    @Override
    public List<Integer> getParentIds(int id) {
        return List.of();
    }
}
