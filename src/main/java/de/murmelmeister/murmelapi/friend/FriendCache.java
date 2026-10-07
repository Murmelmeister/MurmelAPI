package de.murmelmeister.murmelapi.friend;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.friend.favorite.FriendFavorite;
import de.murmelmeister.murmelapi.friend.request.FriendRequest;
import de.murmelmeister.murmelapi.friend.settings.FriendSettings;
import de.murmelmeister.murmelapi.friend.ship.Friendship;
import de.murmelmeister.murmelapi.user.block.UserBlock;
import de.murmelmeister.murmelapi.utils.CacheUtil;
import de.murmelmeister.murmelapi.utils.MurmelCache;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

final class FriendCache implements MurmelCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(FriendCache.class);

    @Language("MariaDB")
    private static final String SQL_FRIEND_SHIP = "SELECT * FROM %s WHERE user_one = ? AND user_two = ?";
    @Language("MariaDB")
    private static final String SQL_FRIEND_REQUEST = "SELECT * FROM %s WHERE sender_id = ? AND receiver_id = ?";
    @Language("MariaDB")
    private static final String SQL_FRIEND_FAVORITE = "SELECT * FROM %s WHERE owner_id = ? AND friend_id = ?";
    @Language("MariaDB")
    private static final String SQL_FRIEND_SETTING = "SELECT * FROM %s WHERE user_id = ?";
    @Language("MariaDB")
    private static final String SQL_BLOCKED = "SELECT * FROM %s WHERE user_id = ? AND blocked_id = ?";

    private final Database database;
    private final Gson gson;
    private final RefreshProvider refreshProvider;
    private final Long fetchLimit;

    private final LoadingCache<@NotNull FriendshipKey, Friendship> friendshipCache;
    private final LoadingCache<@NotNull FriendRequestKey, FriendRequest> friendRequestCache;
    private final LoadingCache<@NotNull FriendFavoriteKey, FriendFavorite> friendFavoriteCache;
    private final LoadingCache<@NotNull FriendSettingKey, FriendSettings> friendSettingsCache;
    private final LoadingCache<@NotNull BlockedKey, UserBlock> blockedCache;

    public FriendCache(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.gson = gson;
        this.refreshProvider = refreshProvider;
        this.fetchLimit = fetchLimit;
        this.friendshipCache = CacheUtil.buildCacheRefresh(this::loadFriendship, cacheCapacity, refreshInterval);
        this.friendRequestCache = CacheUtil.buildCacheRefresh(this::loadFriendRequest, cacheCapacity, refreshInterval);
        this.friendFavoriteCache = CacheUtil.buildCacheRefresh(this::loadFriendFavorite, cacheCapacity, refreshInterval);
        this.friendSettingsCache = CacheUtil.buildCacheRefresh(this::loadFriendSettings, cacheCapacity, refreshInterval);
        this.blockedCache = CacheUtil.buildCacheRefresh(this::loadBlocked, cacheCapacity, refreshInterval);
        this.refreshProvider.register(this);
    }

    @Override
    public void onRefresh(@NotNull RefreshEvent<?> event) {
        String cacheName = event.type();

        if (RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            clear();
            return;
        }

        Object key = event.key();
        if (RefreshType.SINGLE_FRIEND_SHIP.getName().equalsIgnoreCase(cacheName)) {
            if (key instanceof FriendshipKey friendshipKey)
                remove(friendshipKey);
            else if (key instanceof String json) {
                try {
                    final FriendshipKey friendshipKey = gson.fromJson(json, FriendshipKey.class);

                    if (friendshipKey == null) {
                        LOGGER.warn("Failed to parse JSON for single to null: {}", json);
                        return;
                    }

                    remove(friendshipKey);
                } catch (JsonSyntaxException e) {
                    LOGGER.warn("Failed to parse JSON for single refresh: {}", json, e);
                }
            }
        } else if (RefreshType.SINGLE_FRIEND_REQUEST.getName().equalsIgnoreCase(cacheName)) {
            if (key instanceof FriendRequestKey friendRequestKey)
                remove(friendRequestKey);
            else if (key instanceof String json) {
                try {
                    final FriendRequestKey friendRequestKey = gson.fromJson(json, FriendRequestKey.class);

                    if (friendRequestKey == null) {
                        LOGGER.warn("Failed to parse JSON for single to null: {}", json);
                        return;
                    }

                    remove(friendRequestKey);
                } catch (JsonSyntaxException e) {
                    LOGGER.warn("Failed to parse JSON for single refresh: {}", json, e);
                }
            }
        } else if (RefreshType.SINGLE_FRIEND_FAVORITE.getName().equalsIgnoreCase(cacheName)) {
            if (key instanceof FriendFavoriteKey friendFavoriteKey)
                remove(friendFavoriteKey);
            else if (key instanceof String json) {
                try {
                    final FriendFavoriteKey friendFavoriteKey = gson.fromJson(json, FriendFavoriteKey.class);

                    if (friendFavoriteKey == null) {
                        LOGGER.warn("Failed to parse JSON for single to null: {}", json);
                        return;
                    }

                    remove(friendFavoriteKey);
                } catch (JsonSyntaxException e) {
                    LOGGER.warn("Failed to parse JSON for single refresh: {}", json, e);
                }
            }
        } else if (RefreshType.SINGLE_FRIEND_SETTING.getName().equalsIgnoreCase(cacheName)) {
            if (key instanceof FriendSettingKey friendSettingKey)
                remove(friendSettingKey);
            else if (key instanceof String json) {
                try {
                    final FriendSettingKey friendSettingKey = gson.fromJson(json, FriendSettingKey.class);

                    if (friendSettingKey == null) {
                        LOGGER.warn("Failed to parse JSON for single to null: {}", json);
                        return;
                    }

                    remove(friendSettingKey);
                } catch (JsonSyntaxException e) {
                    LOGGER.warn("Failed to parse JSON for single refresh: {}", json, e);
                }
            }
        } else if (RefreshType.SINGLE_USER_BLOCKED.getName().equalsIgnoreCase(cacheName)) {
            if (key instanceof BlockedKey blockedKey)
                remove(blockedKey);
            else if (key instanceof String json) {
                try {
                    final BlockedKey blockedKey = gson.fromJson(json, BlockedKey.class);

                    if (blockedKey == null) {
                        LOGGER.warn("Failed to parse JSON for single to null: {}", json);
                        return;
                    }

                    remove(blockedKey);
                } catch (JsonSyntaxException e) {
                    LOGGER.warn("Failed to parse JSON for single refresh: {}", json, e);
                }
            }
        }
    }

    @Override
    public void close() {
        refreshProvider.unregister(this);
        clear();
    }

    private @Nullable Friendship loadFriendship(FriendshipKey key) {
        if (key == null) return null;
        String sql = SQL_FRIEND_SHIP.formatted("user_friendships");
        return CacheUtil.loadSingle(database, sql, fetchLimit, Friendship::resultSet, stmt -> {
            stmt.setInt(1, key.userOne());
            stmt.setInt(2, key.userTwo());
        });
    }

    private @Nullable FriendRequest loadFriendRequest(FriendRequestKey key) {
        if (key == null) return null;
        String sql = SQL_FRIEND_REQUEST.formatted("user_friend_requests");
        return CacheUtil.loadSingle(database, sql, fetchLimit, FriendRequest::resultSet, stmt -> {
            stmt.setInt(1, key.senderId());
            stmt.setInt(2, key.receiverId());
        });
    }

    private @Nullable FriendFavorite loadFriendFavorite(FriendFavoriteKey key) {
        if (key == null) return null;
        String sql = SQL_FRIEND_FAVORITE.formatted("user_friend_favorites");
        return CacheUtil.loadSingle(database, sql, fetchLimit, FriendFavorite::resultSet, stmt -> {
            stmt.setInt(1, key.ownerId());
            stmt.setInt(2, key.friendId());
        });
    }

    private @Nullable FriendSettings loadFriendSettings(FriendSettingKey key) {
        if (key == null) return null;
        String sql = SQL_FRIEND_SETTING.formatted("user_friend_settings");
        return CacheUtil.loadSingle(database, sql, fetchLimit, FriendSettings::resultSet,
                stmt -> stmt.setInt(1, key.userId()));
    }

    private @Nullable UserBlock loadBlocked(BlockedKey key) {
        if (key == null) return null;
        String sql = SQL_BLOCKED.formatted("user_blocks_list");
        return CacheUtil.loadSingle(database, sql, fetchLimit, UserBlock::resultSet, stmt -> {
            stmt.setInt(1, key.userId());
            stmt.setInt(2, key.blockedId());
        });
    }

    public Friendship getFriendship(int userOne, int userTwo) {
        return friendshipCache.get(new FriendshipKey(userOne, userTwo));
    }

    public FriendRequest getFriendRequest(int senderId, int receiverId) {
        return friendRequestCache.get(new FriendRequestKey(senderId, receiverId));
    }

    public FriendFavorite getFriendFavorite(int ownerId, int friendId) {
        return friendFavoriteCache.get(new FriendFavoriteKey(ownerId, friendId));
    }

    public FriendSettings getFriendSettings(int userId) {
        return friendSettingsCache.get(new FriendSettingKey(userId));
    }

    public UserBlock getBlocked(int userId, int blockedId) {
        return blockedCache.get(new BlockedKey(userId, blockedId));
    }

    public @NotNull @Unmodifiable Collection<Friendship> getFriendships() {
        Collection<Friendship> friendships = friendshipCache.asMap().values();
        return friendships.isEmpty() ? Collections.emptyList() : List.copyOf(friendships);
    }

    public @NotNull @Unmodifiable Collection<FriendRequest> getFriendRequests() {
        Collection<FriendRequest> requests = friendRequestCache.asMap().values();
        return requests.isEmpty() ? Collections.emptyList() : List.copyOf(requests);
    }

    public @NotNull @Unmodifiable Collection<FriendFavorite> getFriendFavorites() {
        Collection<FriendFavorite> favorites = friendFavoriteCache.asMap().values();
        return favorites.isEmpty() ? Collections.emptyList() : List.copyOf(favorites);
    }

    public @NotNull @Unmodifiable Collection<FriendSettings> getFriendSettings() {
        Collection<FriendSettings> settings = friendSettingsCache.asMap().values();
        return settings.isEmpty() ? Collections.emptyList() : List.copyOf(settings);
    }

    public @NotNull @Unmodifiable Collection<UserBlock> getBlockedList() {
        Collection<UserBlock> blocks = blockedCache.asMap().values();
        return blocks.isEmpty() ? Collections.emptyList() : List.copyOf(blocks);
    }

    public void remove(@NotNull FriendshipKey key) {
        friendshipCache.invalidate(key);
    }

    public void remove(@NotNull FriendRequestKey key) {
        friendRequestCache.invalidate(key);
    }

    public void remove(@NotNull FriendFavoriteKey key) {
        friendFavoriteCache.invalidate(key);
    }

    public void remove(@NotNull FriendSettingKey key) {
        friendSettingsCache.invalidate(key);
    }

    public void remove(@NotNull BlockedKey key) {
        blockedCache.invalidate(key);
    }

    public void clear() {
        friendshipCache.invalidateAll();
        friendRequestCache.invalidateAll();
        friendFavoriteCache.invalidateAll();
        friendSettingsCache.invalidateAll();
        blockedCache.invalidateAll();
    }

    record FriendshipKey(int userOne, int userTwo) {
    }

    record FriendRequestKey(int senderId, int receiverId) {
    }

    record FriendFavoriteKey(int ownerId, int friendId) {
    }

    record FriendSettingKey(int userId) {
    }

    record BlockedKey(int userId, int blockedId) {
    }
}
