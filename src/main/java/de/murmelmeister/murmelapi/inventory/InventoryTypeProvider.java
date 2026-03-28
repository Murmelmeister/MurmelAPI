package de.murmelmeister.murmelapi.inventory;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface InventoryTypeProvider {
    void refreshCache();

    @NotNull Optional<InventoryType> findById(int id);

    @NotNull
    @Unmodifiable
    List<InventoryType> findAll();

    @NotNull Optional<InventoryType> create(@NotNull String name);

    int delete(int id);

    @ApiStatus.Internal
    static @NotNull InventoryTypeProvider of(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        return new InventoryTypeProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
    }
}
