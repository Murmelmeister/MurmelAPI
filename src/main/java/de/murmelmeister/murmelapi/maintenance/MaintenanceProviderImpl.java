package de.murmelmeister.murmelapi.maintenance;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;

import java.time.Duration;

public final class MaintenanceProviderImpl implements MaintenanceProvider {
    private static final String TABLE_NAME = "maintenances";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final MaintenanceCache cache;
    private final RefreshType all = RefreshType.MAINTENANCES;
    private final RefreshType single = RefreshType.SINGLE_MAINTENANCE;

    public MaintenanceProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new MaintenanceCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }
}
