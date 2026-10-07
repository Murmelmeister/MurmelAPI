package de.murmelmeister.murmelapi.friend;

import de.murmelmeister.murmelapi.friend.favorite.FriendFavorite;
import de.murmelmeister.murmelapi.friend.request.FriendRequest;
import de.murmelmeister.murmelapi.friend.settings.FriendSettings;
import de.murmelmeister.murmelapi.friend.ship.Friendship;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.session.UserSessionProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;

public interface FriendService {
    // Friendship
    boolean areFriends(int userId, int targetId);

    @NotNull Optional<Friendship> getFriendship(int userId, int targetId);

    @NotNull Collection<User> getFriends(int userId);

    @NotNull Collection<User> getOnlineFriends(int userId);

    int getFriendCount(int userId);

    @NotNull FriendResult removeFriend(int userId, int targetId);

    // FriendRequests – senderId -> receiverId
    boolean hasPendingRequest(int senderId, int receiverId);

    @NotNull Optional<FriendRequest> getPendingRequest(
            int senderId, int receiverId);

    @NotNull Collection<FriendRequest> getIncomingRequests(int userId);

    @NotNull Collection<FriendRequest> getOutgoingRequests(int userId);

    int getIncomingRequestCount(int userId);

    int getOutgoingRequestCount(int userId);

    @NotNull FriendResult sendFriendRequest(int senderId, int receiverId);

    @NotNull FriendResult acceptFriendRequest(int receiverId, int senderId);

    @NotNull FriendResult denyFriendRequest(int receiverId, int senderId);

    @NotNull FriendResult cancelFriendRequest(int senderId, int receiverId);

    // Favorites
    boolean isFavorite(int ownerId, int friendId);

    @NotNull Optional<FriendFavorite> getFavorite(int ownerId, int friendId);

    @NotNull Collection<User> getFavorites(int ownerId);

    int getFavoriteCount(int ownerId);

    @NotNull FriendResult addFavorite(int ownerId, int friendId);

    @NotNull FriendResult removeFavorite(int ownerId, int friendId);

    // Blocks – userId block targetId
    boolean isBlocked(int userId, int targetId);

    // Check both ways
    boolean isBlockedEitherWay(int userId, int targetId);

    @NotNull Collection<User> getBlockedUsers(int userId);

    int getBlockedUserCount(int userId);

    @NotNull FriendResult blockUser(int userId, int targetId);

    @NotNull FriendResult unblockUser(int userId, int targetId);

    // Settings – liefert bei fehlendem Eintrag die Schema-Defaults
    @NotNull
    FriendSettings getSettings(int userId);

    @NotNull FriendResult updateSettings(
            int userId,
            boolean allowRequests,
            boolean showServer,
            boolean allowFollow,
            boolean loginNotify);

    @NotNull FriendResult setAllowRequests(int userId, boolean enabled);

    @NotNull FriendResult setShowServer(int userId, boolean enabled);

    @NotNull FriendResult setAllowFollow(int userId, boolean enabled);

    @NotNull FriendResult setLoginNotify(int userId, boolean enabled);

    @NotNull FriendResult resetSettings(int userId);

    // Berechtigungen zwischen Freunden
    boolean canViewServer(int viewerId, int targetId);

    boolean canFollow(int followerId, int targetId);

    boolean shouldNotifyLogin(int recipientId, int loggedInUserId);

    @ApiStatus.Internal
    static @NotNull FriendService of(@NotNull FriendProvider provider, @NotNull UserProvider userProvider,
                                    @NotNull UserSessionProvider sessionProvider) {
        return new FriendServiceImpl(provider, userProvider, sessionProvider);
    }
}
