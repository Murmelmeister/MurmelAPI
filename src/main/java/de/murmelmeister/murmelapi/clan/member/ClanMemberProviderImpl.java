package de.murmelmeister.murmelapi.clan.member;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;

public final class ClanMemberProviderImpl implements ClanMemberProvider {
    private static final String TABLE_NAME = "clan_member";

    private final Database database;
    private final ClanMemberCache cache;
    private final RefreshType all = RefreshType.CLAN_MEMBERS;
    private final RefreshType single = RefreshType.SINGLE_CLAN_MEMBER;

    public ClanMemberProviderImpl(Database database, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.cache = new ClanMemberCache(database, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        RefreshUtil.fireCache(all);
    }
}
