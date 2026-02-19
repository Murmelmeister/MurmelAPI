package de.murmelmeister.murmelapi.maintenance.whitelist;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;

import java.time.Duration;

public final class MaintenanceWhitelistProviderImpl implements MaintenanceWhitelistProvider {
    private static final String TABLE_NAME = "maintenance_whitelist";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final MaintenanceWhitelistCache cache;
    private final RefreshType all = RefreshType.MAINTENANCE_WHITELISTS;
    private final RefreshType single = RefreshType.SINGLE_MAINTENANCE_WHITELIST;

    public MaintenanceWhitelistProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new MaintenanceWhitelistCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

}
