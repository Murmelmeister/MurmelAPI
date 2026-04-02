package de.murmelmeister.murmelapi.user.inventory;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface UserInventoryProvider {
    void refreshCache();

    @NotNull Optional<UserInventory> findById(int userId, int inventoryId);

    @NotNull
    @Unmodifiable
    List<UserInventory> findAll();

    @NotNull Optional<UserInventory> upsert(int userId, int inventoryId, @NotNull String value);

    int delete(int userId, int inventoryId);

    @ApiStatus.Internal
    static @NotNull UserInventoryProvider of(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        return new UserInventoryProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
    }
}
