package de.murmelmeister.murmelapi.friend;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.friend.favorite.FriendFavorite;
import de.murmelmeister.murmelapi.friend.request.FriendRequest;
import de.murmelmeister.murmelapi.friend.settings.FriendSettings;
import de.murmelmeister.murmelapi.friend.ship.Friendship;
import de.murmelmeister.murmelapi.user.block.UserBlock;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface FriendProvider {
    @NotNull Optional<Friendship> findFriendship(int userOne, int userTwo);

    @NotNull Optional<FriendRequest> findRequest(int senderId, int receiverId);

    @NotNull Optional<FriendFavorite> findFavorite(int ownerId, int friendId);

    @NotNull Optional<FriendSettings> findSettings(int userId);

    @NotNull Optional<UserBlock> findBlocked(int userId, int blockedId);

    @NotNull
    @Unmodifiable
    Collection<Friendship> findFriendships();

    @NotNull
    @Unmodifiable
    Collection<FriendRequest> findRequests();

    @NotNull
    @Unmodifiable
    Collection<FriendFavorite> findFavorites();

    @NotNull
    @Unmodifiable
    Collection<FriendSettings> findSettings();

    @NotNull
    @Unmodifiable
    Collection<UserBlock> findBlockedList();

    @NotNull Optional<Friendship> createFriendship(int userOne, int userTwo);

    @NotNull Optional<FriendRequest> createRequest(int senderId, int receiverId, @NotNull LocalDateTime expires);

    @NotNull Optional<FriendFavorite> createFavorite(int ownerId, int friendId);

    @NotNull Optional<FriendSettings> createSettings(int userId);

    @NotNull Optional<UserBlock> createBlocked(int userId, int blockedId);

    int deleteFriendship(int userOne, int userTwo);

    int deleteRequest(int senderId, int receiverId);

    int deleteFavorite(int ownerId, int friendId);

    int deleteSettings(int userId);

    int deleteBlocked(int userId, int blockedId);

    @NotNull Optional<FriendSettings> updateSettings(int userId, @NotNull FriendSettings settings);

    @ApiStatus.Internal
    static @NotNull FriendProvider of(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        return new FriendProviderImpl(database, gson, refreshProvider, fetchLimit, cacheCapacity, refreshInterval);
    }
}
