package de.murmelmeister.murmelapi.user;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * The {@code User} interface provides methods for managing user data in a database.
 * It allows checking for user existence, creating and deleting users, and retrieving user information.
 * This interface is designed to be implemented by classes that provide specific database interactions.
 */
public interface UserProvider {
    void refreshCache();

    User findById(int userId);

    User findByMojangId(UUID uuid);

    User findByUsername(String username);

    List<User> findAll();

    List<UUID> findMojangIds();

    List<String> findUsernames();

    User create(UUID uuid, String username);

    int delete(int userId);

    User update(int userId, String username, LocalDateTime firstLogin, boolean debugUser, boolean debugEnabled, int languageIdr);
}
