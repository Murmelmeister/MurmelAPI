package de.murmelmeister.murmelapi.user.color;

import java.util.List;

public interface UserPrefixColorProvider {
    void refreshCache();

    UserPrefixColor findById(int userId, String colorId);

    List<UserPrefixColor> findAll();

    UserPrefixColor create(int userId, String colorId, boolean active);

    int delete(int userId, String colorId);

    UserPrefixColor update(int userId, String colorId, boolean active);
}
