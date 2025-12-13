package de.murmelmeister.murmelapi.clan;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;

public final class ClanProviderImpl implements ClanProvider {
    private static final String TABLE_NAME = "clans";

    private final Database database;
    private final ClanCache cache;
    private final RefreshType all = RefreshType.CLANS;
    private final RefreshType single = RefreshType.SINGLE_CLAN;

    public ClanProviderImpl(Database database, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.cache = new ClanCache(database, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        RefreshUtil.fireCache(all);
    }
}
