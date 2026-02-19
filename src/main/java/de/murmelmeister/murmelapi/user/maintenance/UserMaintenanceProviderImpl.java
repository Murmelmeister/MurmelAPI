package de.murmelmeister.murmelapi.user.maintenance;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;

import java.time.Duration;

public final class UserMaintenanceProviderImpl implements UserMaintenanceProvider {
    private static final String TABLE_NAME = "user_maintenance_windows";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final UserMaintenanceCache cache;
    private final RefreshType all = RefreshType.USER_MAINTENANCES;
    private final RefreshType single = RefreshType.SINGLE_USER_MAINTENANCE;

    public UserMaintenanceProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new UserMaintenanceCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }
}
