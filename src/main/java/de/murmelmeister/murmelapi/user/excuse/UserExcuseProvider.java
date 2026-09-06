package de.murmelmeister.murmelapi.user.excuse;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserExcuseProvider {
    void refreshCache();

    @NotNull Optional<UserExcuse> findById(int id);

    @NotNull
    @Unmodifiable
    List<UserExcuse> findByUserId(int userId);

    @NotNull
    @Unmodifiable
    List<UserExcuse> findAll();

    @NotNull Optional<UserExcuse> create(int userId, @NotNull LocalDate startDate, int extraDays, @Nullable String reason, int createdBy);

    @NotNull Optional<UserExcuse> update(int id, @NotNull LocalDate startDate, int extraDays, @Nullable String reason, int changedBy);

    @ApiStatus.Internal
    static @NotNull UserExcuseProvider of(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        return new UserExcuseProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
    }
}
