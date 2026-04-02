package de.murmelmeister.murmelapi.user;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The {@code User} interface provides methods for managing user data in a database.
 * It allows checking for user existence, creating and deleting users, and retrieving user information.
 * This interface is designed to be implemented by classes that provide specific database interactions.
 */
public interface UserProvider {
    void refreshCache();

    @NotNull Optional<User> findById(int userId);

    @NotNull Optional<User> findByMojangId(@NotNull UUID uuid);

    @NotNull Optional<User> findByUsername(@NotNull String username);

    @NotNull
    @Unmodifiable
    List<User> findAll();

    @NotNull
    @Unmodifiable
    List<UUID> findMojangIds();

    @NotNull
    @Unmodifiable
    List<String> findUsernames();

    @NotNull Optional<User> create(@NotNull UUID uuid, @NotNull String username);

    int delete(int userId);

    @NotNull Optional<User> update(int userId, @NotNull String username, @Nullable LocalDateTime firstLogin, boolean debugUser, boolean debugEnabled, int languageId);

    @ApiStatus.Internal
    static @NotNull UserProvider of(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        return new UserProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
    }
}
