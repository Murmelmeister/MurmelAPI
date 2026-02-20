package de.murmelmeister.murmelapi.user.excuse;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;

import java.time.Duration;

public final class UserExcuseProviderImpl implements UserExcuseProvider {
    private static final String TABLE_NAME = "user_excuses";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final UserExcuseCache cache;
    private final RefreshType all = RefreshType.USER_MAINTENANCES;
    private final RefreshType single = RefreshType.SINGLE_USER_MAINTENANCE;

    public UserExcuseProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new UserExcuseCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }
}
