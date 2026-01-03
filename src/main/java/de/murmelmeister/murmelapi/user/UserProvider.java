package de.murmelmeister.murmelapi.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @Nullable User findById(int userId);

    @Nullable User findByMojangId(@Nullable UUID uuid);

    @Nullable User findByUsername(@Nullable String username);

    @NotNull List<User> findAll();

    @NotNull List<UUID> findMojangIds();

    @NotNull List<String> findUsernames();

    @Nullable User create(@NotNull UUID uuid, @NotNull String username);

    int delete(int userId);

    @Nullable User update(int userId, @NotNull String username, @Nullable LocalDateTime firstLogin, boolean debugUser, boolean debugEnabled, int languageIdr);
}
