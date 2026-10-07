package de.murmelmeister.murmelapi.friend;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.friend.FriendException;
import de.murmelmeister.murmelapi.friend.favorite.FriendFavorite;
import de.murmelmeister.murmelapi.friend.request.FriendRequest;
import de.murmelmeister.murmelapi.friend.settings.FriendSettings;
import de.murmelmeister.murmelapi.friend.ship.Friendship;
import de.murmelmeister.murmelapi.user.block.UserBlock;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.sql.Types;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

final class FriendProviderImpl implements FriendProvider {
    private static final String TABLE_FRIEND_SHIP = "user_friendships";
    private static final String TABLE_FRIEND_REQUEST = "user_friend_requests";
    private static final String TABLE_FRIEND_FAVORITE = "user_friend_favorites";
    private static final String TABLE_FRIEND_SETTING = "user_friend_settings";
    private static final String TABLE_USER_BLOCKED = "user_blocks_list";

    @Language("MariaDB")
    private static final String SQL_CREATE_FRIEND_SHIP = """
            INSERT INTO %s (user_one, user_two)
            VALUES (?, ?)
            RETURNING user_one, user_two, created_at
            """.formatted(TABLE_FRIEND_SHIP);

    @Language("MariaDB")
    private static final String SQL_CREATE_FRIEND_REQUEST = """
            INSERT INTO %s (sender_id, receiver_id, expires_at)
            VALUES (?, ?, ?)
            RETURNING sender_id, receiver_id, created_at, expires_at
            """.formatted(TABLE_FRIEND_REQUEST);

    @Language("MariaDB")
    private static final String SQL_CREATE_FRIEND_FAVORITE = """
            INSERT INTO %s (owner_id, friend_id)
            VALUES (?, ?)
            RETURNING owner_id, friend_id, created_at
            """.formatted(TABLE_FRIEND_FAVORITE);

    @Language("MariaDB")
    private static final String SQL_CREATE_FRIEND_SETTING = """
            INSERT INTO %s (user_id)
            VALUES (?)
            RETURNING user_id, allow_requests, show_server, allow_follow, login_notify
            """.formatted(TABLE_FRIEND_SETTING);

    @Language("MariaDB")
    private static final String SQL_CREATE_USER_BLOCKED = """
            INSERT INTO %s (user_id, blocked_id)
            VALUES (?, ?)
            RETURNING user_id, blocked_id, created_at
            """.formatted(TABLE_USER_BLOCKED);

    @Language("MariaDB")
    private static final String SQL_DELETE_FRIEND_SHIP = "DELETE FROM %s WHERE user_one = ? AND user_two = ?".formatted(TABLE_FRIEND_SHIP);

    @Language("MariaDB")
    private static final String SQL_DELETE_FRIEND_REQUEST = "DELETE FROM %s WHERE sender_id = ? AND receiver_id = ?".formatted(TABLE_FRIEND_REQUEST);

    @Language("MariaDB")
    private static final String SQL_DELETE_FRIEND_FAVORITE = "DELETE FROM %s WHERE owner_id = ? AND friend_id = ?".formatted(TABLE_FRIEND_FAVORITE);

    @Language("MariaDB")
    private static final String SQL_DELETE_FRIEND_SETTING = "DELETE FROM %s WHERE user_id = ?".formatted(TABLE_FRIEND_SETTING);

    @Language("MariaDB")
    private static final String SQL_DELETE_USER_BLOCKED = "DELETE FROM %s WHERE user_id = ? AND blocked_id = ?".formatted(TABLE_USER_BLOCKED);

    @Language("MariaDB")
    private static final String SQL_UPDATE_FRIEND_SETTING = """
            UPDATE %s
            SET allow_requests = ?,
                show_server = ?,
                allow_follow = ?,
                login_notify = ?
            WHERE user_id = ?
            """.formatted(TABLE_FRIEND_SETTING);

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final FriendCache cache;

    private final RefreshType singleFriendship = RefreshType.SINGLE_FRIEND_SHIP;
    private final RefreshType singleRequest = RefreshType.SINGLE_FRIEND_REQUEST;
    private final RefreshType singleFavorite = RefreshType.SINGLE_FRIEND_FAVORITE;
    private final RefreshType singleSetting = RefreshType.SINGLE_FRIEND_SETTING;
    private final RefreshType singleBlocked = RefreshType.SINGLE_USER_BLOCKED;

    public FriendProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new FriendCache(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public @NotNull Optional<Friendship> findFriendship(int userOne, int userTwo) {
        return Optional.ofNullable(cache.getFriendship(userOne, userTwo));
    }

    @Override
    public @NotNull Optional<FriendRequest> findRequest(int senderId, int receiverId) {
        return Optional.ofNullable(cache.getFriendRequest(senderId, receiverId));
    }

    @Override
    public @NotNull Optional<FriendFavorite> findFavorite(int ownerId, int friendId) {
        return Optional.ofNullable(cache.getFriendFavorite(ownerId, friendId));
    }

    @Override
    public @NotNull Optional<FriendSettings> findSettings(int userId) {
        return Optional.ofNullable(cache.getFriendSettings(userId));
    }

    @Override
    public @NotNull Optional<UserBlock> findBlocked(int userId, int blockedId) {
        return Optional.ofNullable(cache.getBlocked(userId, blockedId));
    }

    @Override
    public @NotNull @Unmodifiable Collection<Friendship> findFriendships() {
        return cache.getFriendships();
    }

    @Override
    public @NotNull @Unmodifiable Collection<FriendRequest> findRequests() {
        return cache.getFriendRequests();
    }

    @Override
    public @NotNull @Unmodifiable Collection<FriendFavorite> findFavorites() {
        return cache.getFriendFavorites();
    }

    @Override
    public @NotNull @Unmodifiable Collection<FriendSettings> findSettings() {
        return cache.getFriendSettings();
    }

    @Override
    public @NotNull @Unmodifiable Collection<UserBlock> findBlockedList() {
        return cache.getBlockedList();
    }

    @Override
    public @NotNull Optional<Friendship> createFriendship(int userOne, int userTwo) {
        if (userOne < CONSOLE_USER_ID)
            throw new IllegalArgumentException("userOne must be greater than " + CONSOLE_USER_ID);
        if (userTwo < CONSOLE_USER_ID)
            throw new IllegalArgumentException("userTwo must be greater than " + CONSOLE_USER_ID);
        if (userOne == userTwo)
            throw new IllegalArgumentException("userOne '" + userOne + "' and userTwo '" + userTwo + "' must be different");

        Friendship friendship = MurmelExceptionWrapper.dbWrap(
                "Failed to create friendship (userOne=" + userOne + ", userTwo=" + userTwo + ")",
                () -> database.query(SQL_CREATE_FRIEND_SHIP, null, Friendship::resultSet, stmt -> {
                    stmt.setInt(1, userOne);
                    stmt.setInt(2, userTwo);
                }),
                FriendException::new
        );

        if (friendship == null) return Optional.empty();
        refreshProvider.fireSingle(singleFriendship, new FriendCache.FriendshipKey(userOne, userTwo));
        return Optional.of(friendship);
    }

    @Override
    public @NotNull Optional<FriendRequest> createRequest(int senderId, int receiverId, @NotNull LocalDateTime expires) {
        if (senderId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("senderId must be greater than " + CONSOLE_USER_ID);
        if (receiverId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("receiverId must be greater than " + CONSOLE_USER_ID);
        if (senderId == receiverId)
            throw new IllegalArgumentException("senderId '" + senderId + "' and receiverId '" + receiverId + "' must be different");
        Objects.requireNonNull(expires, "expires must not be null");

        FriendRequest request = MurmelExceptionWrapper.dbWrap(
                "Failed to create friend request (senderId=" + senderId + ", receiverId=" + receiverId + ", expires=" + expires + ")",
                () -> database.query(SQL_CREATE_FRIEND_REQUEST, null, FriendRequest::resultSet, stmt -> {
                    stmt.setInt(1, senderId);
                    stmt.setInt(2, receiverId);
                    stmt.setObject(3, expires, Types.TIMESTAMP);
                }),
                FriendException::new
        );

        if (request == null) return Optional.empty();
        refreshProvider.fireSingle(singleRequest, new FriendCache.FriendRequestKey(senderId, receiverId));
        return Optional.of(request);
    }

    @Override
    public @NotNull Optional<FriendFavorite> createFavorite(int ownerId, int friendId) {
        if (ownerId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("ownerId must be greater than " + CONSOLE_USER_ID);
        if (friendId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("friendId must be greater than " + CONSOLE_USER_ID);
        if (ownerId == friendId)
            throw new IllegalArgumentException("ownerId '" + ownerId + "' and friendId '" + friendId + "' must be different");

        FriendFavorite favorite = MurmelExceptionWrapper.dbWrap(
                "Failed to create friend favorite (ownerId=" + ownerId + ", friendId=" + friendId + ")",
                () -> database.query(SQL_CREATE_FRIEND_FAVORITE, null, FriendFavorite::resultSet, stmt -> {
                    stmt.setInt(1, ownerId);
                    stmt.setInt(2, friendId);
                }),
                FriendException::new
        );

        if (favorite == null) return Optional.empty();
        refreshProvider.fireSingle(singleFavorite, new FriendCache.FriendFavoriteKey(ownerId, friendId));
        return Optional.of(favorite);
    }

    @Override
    public @NotNull Optional<FriendSettings> createSettings(int userId) {
        if (userId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("userId must be greater than " + CONSOLE_USER_ID);

        FriendSettings settings = MurmelExceptionWrapper.dbWrap(
                "Failed to create friend setting (userId=" + userId + ")",
                () -> database.query(SQL_CREATE_FRIEND_SETTING, null, FriendSettings::resultSet,
                        stmt -> stmt.setInt(1, userId)),
                FriendException::new
        );

        if (settings == null) return Optional.empty();
        refreshProvider.fireSingle(singleSetting, new FriendCache.FriendSettingKey(userId));
        return Optional.of(settings);
    }

    @Override
    public @NotNull Optional<UserBlock> createBlocked(int userId, int blockedId) {
        if (userId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("userId must be greater than " + CONSOLE_USER_ID);
        if (blockedId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("blockedId must be greater than " + CONSOLE_USER_ID);
        if (userId == blockedId)
            throw new IllegalArgumentException("userId '" + userId + "' and blockedId '" + blockedId + "'  must be different");

        UserBlock blocked = MurmelExceptionWrapper.dbWrap(
                "Failed to create a blocked user (userId=" + userId + ", blockedId=" + blockedId + ")",
                () -> database.query(SQL_CREATE_USER_BLOCKED, null, UserBlock::resultSet, stmt -> {
                    stmt.setInt(1, userId);
                    stmt.setInt(2, blockedId);
                }),
                FriendException::new
        );

        if (blocked == null) return Optional.empty();
        refreshProvider.fireSingle(singleBlocked, new FriendCache.BlockedKey(userId, blockedId));
        return Optional.of(blocked);
    }

    @Override
    public int deleteFriendship(int userOne, int userTwo) {
        if (userOne < CONSOLE_USER_ID)
            throw new IllegalArgumentException("userOne must be greater than " + CONSOLE_USER_ID);
        if (userTwo < CONSOLE_USER_ID)
            throw new IllegalArgumentException("userTwo must be greater than " + CONSOLE_USER_ID);
        if (userOne == userTwo)
            throw new IllegalArgumentException("userOne '" + userOne + "' and userTwo '" + userTwo + "' must be different");

        Friendship existing = cache.getFriendship(userOne, userTwo);
        if (existing == null) return 0;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete friendship (userOne=" + userOne + ", userTwo=" + userTwo + ")",
                () -> database.update(SQL_DELETE_FRIEND_SHIP, stmt -> {
                    stmt.setInt(1, userOne);
                    stmt.setInt(2, userTwo);
                }),
                FriendException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(singleFriendship, new FriendCache.FriendshipKey(existing.userOne(), existing.userTwo()));
        return row;
    }

    @Override
    public int deleteRequest(int senderId, int receiverId) {
        if (senderId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("senderId must be greater than " + CONSOLE_USER_ID);
        if (receiverId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("receiverId must be greater than " + CONSOLE_USER_ID);
        if (senderId == receiverId)
            throw new IllegalArgumentException("senderId '" + senderId + "' and receiverId '" + receiverId + "' must be different");

        FriendRequest existing = cache.getFriendRequest(senderId, receiverId);
        if (existing == null) return 0;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete friend request (senderId=" + senderId + ", receiverId=" + receiverId + ")",
                () -> database.update(SQL_DELETE_FRIEND_REQUEST, stmt -> {
                    stmt.setInt(1, senderId);
                    stmt.setInt(2, receiverId);
                }),
                FriendException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(singleRequest, new FriendCache.FriendRequestKey(existing.senderId(), existing.receiverId()));
        return row;
    }

    @Override
    public int deleteFavorite(int ownerId, int friendId) {
        if (ownerId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("ownerId must be greater than " + CONSOLE_USER_ID);
        if (friendId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("friendId must be greater than " + CONSOLE_USER_ID);
        if (ownerId == friendId)
            throw new IllegalArgumentException("ownerId '" + ownerId + "' and friendId '" + friendId + "' must be different");

        FriendFavorite existing = cache.getFriendFavorite(ownerId, friendId);
        if (existing == null) return 0;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete friend favorite (ownerId=" + ownerId + ", friendId=" + friendId + ")",
                () -> database.update(SQL_DELETE_FRIEND_FAVORITE, stmt -> {
                    stmt.setInt(1, ownerId);
                    stmt.setInt(2, friendId);
                }),
                FriendException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(singleFavorite, new FriendCache.FriendFavoriteKey(existing.ownerId(), existing.friendId()));
        return row;
    }

    @Override
    public int deleteSettings(int userId) {
        if (userId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("userId must be greater than " + CONSOLE_USER_ID);

        FriendSettings existing = cache.getFriendSettings(userId);
        if (existing == null) return 0;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete friend setting (userId=" + userId + ")",
                () -> database.update(SQL_DELETE_FRIEND_SETTING,
                        stmt -> stmt.setInt(1, userId)),
                FriendException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(singleSetting, new FriendCache.FriendSettingKey(existing.userId()));
        return row;
    }

    @Override
    public int deleteBlocked(int userId, int blockedId) {
        if (userId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("userId must be greater than " + CONSOLE_USER_ID);
        if (blockedId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("blockedId must be greater than " + CONSOLE_USER_ID);
        if (userId == blockedId)
            throw new IllegalArgumentException("userId '" + userId + "' and blockedId '" + blockedId + "'  must be different");

        UserBlock existing = cache.getBlocked(userId, blockedId);
        if (existing == null) return 0;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete a blocked user (userId=" + userId + ", blockedId=" + blockedId + ")",
                () -> database.update(SQL_DELETE_USER_BLOCKED, stmt -> {
                    stmt.setInt(1, userId);
                    stmt.setInt(2, blockedId);
                }),
                FriendException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(singleBlocked, new FriendCache.BlockedKey(existing.userId(), existing.blockedId()));
        return row;
    }

    @Override
    public @NotNull Optional<FriendSettings> updateSettings(int userId, @NotNull FriendSettings settings) {
        if (userId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("userId must be greater than " + CONSOLE_USER_ID);
        Objects.requireNonNull(settings, "settings must not be null");

        FriendSettings existing = cache.getFriendSettings(userId);
        if (existing == null) return Optional.empty();

        if (existing.allowRequests() == settings.allowRequests()
                && existing.showServer() == settings.showServer()
                && existing.allowFollow() == settings.allowFollow()
                && existing.loginNotify() == settings.loginNotify())
            return Optional.of(existing);

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to update friend setting (userId=" + userId + ")",
                () -> database.update(SQL_UPDATE_FRIEND_SETTING, stmt -> {
                    stmt.setBoolean(1, settings.allowRequests());
                    stmt.setBoolean(2, settings.showServer());
                    stmt.setBoolean(3, settings.allowFollow());
                    stmt.setBoolean(4, settings.loginNotify());
                    stmt.setInt(5, userId);
                }),
                FriendException::new
        );

        if (row != 1) return Optional.empty();
        refreshProvider.fireSingle(singleSetting, new FriendCache.FriendSettingKey(userId));
        return Optional.of(cache.getFriendSettings(userId));
    }
}
