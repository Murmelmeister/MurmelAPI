package de.murmelmeister.murmelapi.friend;

import de.murmelmeister.murmelapi.exceptions.friend.FriendException;
import de.murmelmeister.murmelapi.friend.favorite.FriendFavorite;
import de.murmelmeister.murmelapi.friend.request.FriendRequest;
import de.murmelmeister.murmelapi.friend.settings.FriendSettings;
import de.murmelmeister.murmelapi.friend.ship.Friendship;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.session.UserSessionProvider;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

final class FriendServiceImpl implements FriendService {
    private final FriendProvider friendProvider;
    private final UserProvider userProvider;

    private final UserSessionProvider sessionProvider;
    // Requests remain valid for 30 days.
    private static final int REQUEST_VALIDITY_DAYS = 30;

    FriendServiceImpl(FriendProvider friendProvider, UserProvider userProvider, UserSessionProvider sessionProvider) {
        this.friendProvider = Objects.requireNonNull(friendProvider, "friendProvider");
        this.userProvider = Objects.requireNonNull(userProvider, "userProvider");
        this.sessionProvider = Objects.requireNonNull(sessionProvider, "sessionProvider");
    }

    @Override
    public boolean areFriends(int userId, int targetId) {
        return getFriendship(userId, targetId).isPresent();
    }

    @Override
    public @NotNull Optional<Friendship> getFriendship(int userId, int targetId) {
        if (userId == targetId) return Optional.empty();
        return friendProvider.findFriendship(userId, targetId)
                .or(() -> friendProvider.findFriendship(targetId, userId));
    }

    @Override
    public @NotNull Collection<User> getFriends(int userId) {
        return friendProvider.findFriendships().stream()
                .filter(entry -> entry.userOne() == userId || entry.userTwo() == userId)
                .map(entry -> entry.userOne() == userId ? entry.userTwo() : entry.userOne())
                .distinct().map(userProvider::findById).flatMap(Optional::stream)
                .collect(Collectors.toList());
    }

    @Override
    public @NotNull Collection<User> getOnlineFriends(int userId) {
        return getFriends(userId).stream()
                .filter(user -> sessionProvider.findByUserId(user.id()).isPresent())
                .collect(Collectors.toList());
    }

    @Override
    public int getFriendCount(int userId) {
        return getFriends(userId).size();
    }

    @Override
    public @NotNull FriendResult removeFriend(int userId, int targetId) {
        FriendResult validation = validateUsers(userId, targetId);
        if (validation != FriendResult.SUCCESS) return validation;
        Optional<Friendship> friendship = getFriendship(userId, targetId);
        if (friendship.isEmpty()) return FriendResult.NOT_FRIENDS;
        if (friendProvider.deleteFriendship(friendship.get().userOne(), friendship.get().userTwo()) == 0)
            return FriendResult.NOT_FRIENDS;
        friendProvider.deleteFavorite(userId, targetId);
        friendProvider.deleteFavorite(targetId, userId);
        return FriendResult.SUCCESS;
    }

    @Override
    public boolean hasPendingRequest(int senderId, int receiverId) {
        return getPendingRequest(senderId, receiverId).isPresent();
    }

    @Override
    public @NotNull Optional<FriendRequest> getPendingRequest(int senderId, int receiverId) {
        return friendProvider.findRequest(senderId, receiverId).filter(request -> !request.isExpired());
    }

    @Override
    public @NotNull Collection<FriendRequest> getIncomingRequests(int userId) {
        return friendProvider.findRequests().stream()
                .filter(request -> request.receiverId() == userId && !request.isExpired())
                .collect(Collectors.toList());
    }

    @Override
    public @NotNull Collection<FriendRequest> getOutgoingRequests(int userId) {
        return friendProvider.findRequests().stream()
                .filter(request -> request.senderId() == userId && !request.isExpired())
                .collect(Collectors.toList());
    }

    @Override
    public int getIncomingRequestCount(int userId) {
        return getIncomingRequests(userId).size();
    }

    @Override
    public int getOutgoingRequestCount(int userId) {
        return getOutgoingRequests(userId).size();
    }

    @Override
    public @NotNull FriendResult sendFriendRequest(int senderId, int receiverId) {
        FriendResult validation = validateUsers(senderId, receiverId);
        if (validation != FriendResult.SUCCESS) return validation;
        if (isBlockedEitherWay(senderId, receiverId)) return FriendResult.BLOCKED;
        if (areFriends(senderId, receiverId)) return FriendResult.ALREADY_FRIENDS;
        if (!getSettings(receiverId).allowRequests()) return FriendResult.REQUESTS_DISABLED;
        if (hasPendingRequest(senderId, receiverId)) return FriendResult.REQUEST_ALREADY_EXISTS;
        if (hasPendingRequest(receiverId, senderId)) return FriendResult.INCOMING_REQUEST_EXISTS;
        deleteExpiredRequest(senderId, receiverId);
        deleteExpiredRequest(receiverId, senderId);
        friendProvider.createRequest(senderId, receiverId, LocalDateTime.now().plusDays(REQUEST_VALIDITY_DAYS))
                .orElseThrow(() -> new FriendException("Failed to create friend request"));
        return FriendResult.SUCCESS;
    }

    @Override
    public @NotNull FriendResult acceptFriendRequest(int receiverId, int senderId) {
        FriendResult validation = validateUsers(receiverId, senderId);
        if (validation != FriendResult.SUCCESS) return validation;
        if (isBlockedEitherWay(receiverId, senderId)) return FriendResult.BLOCKED;
        if (areFriends(receiverId, senderId)) return FriendResult.ALREADY_FRIENDS;
        Optional<FriendRequest> request = friendProvider.findRequest(senderId, receiverId);
        if (request.isEmpty()) return FriendResult.REQUEST_NOT_FOUND;
        if (request.get().isExpired()) {
            friendProvider.deleteRequest(senderId, receiverId);
            return FriendResult.REQUEST_EXPIRED;
        }
        friendProvider.createFriendship(Math.min(senderId, receiverId), Math.max(senderId, receiverId))
                .orElseThrow(() -> new FriendException("Failed to create friendship"));
        friendProvider.deleteRequest(senderId, receiverId);
        friendProvider.deleteRequest(receiverId, senderId);
        return FriendResult.SUCCESS;
    }

    @Override
    public @NotNull FriendResult denyFriendRequest(int receiverId, int senderId) {
        return removeRequest(senderId, receiverId);
    }

    @Override
    public @NotNull FriendResult cancelFriendRequest(int senderId, int receiverId) {
        return removeRequest(senderId, receiverId);
    }

    @Override
    public boolean isFavorite(int ownerId, int friendId) {
        return getFavorite(ownerId, friendId).isPresent();
    }

    @Override
    public @NotNull Optional<FriendFavorite> getFavorite(int ownerId, int friendId) {
        return friendProvider.findFavorite(ownerId, friendId);
    }

    @Override
    public @NotNull Collection<User> getFavorites(int ownerId) {
        return friendProvider.findFavorites().stream().filter(entry -> entry.ownerId() == ownerId)
                .map(entry -> userProvider.findById(entry.friendId())).flatMap(Optional::stream)
                .collect(Collectors.toList());
    }

    @Override
    public int getFavoriteCount(int ownerId) {
        return getFavorites(ownerId).size();
    }

    @Override
    public @NotNull FriendResult addFavorite(int ownerId, int friendId) {
        FriendResult validation = validateUsers(ownerId, friendId);
        if (validation != FriendResult.SUCCESS) return validation;
        if (isBlockedEitherWay(ownerId, friendId)) return FriendResult.BLOCKED;
        if (!areFriends(ownerId, friendId)) return FriendResult.NOT_FRIENDS;
        if (isFavorite(ownerId, friendId)) return FriendResult.ALREADY_FAVORITE;
        friendProvider.createFavorite(ownerId, friendId)
                .orElseThrow(() -> new FriendException("Failed to create favorite"));
        return FriendResult.SUCCESS;
    }

    @Override
    public @NotNull FriendResult removeFavorite(int ownerId, int friendId) {
        FriendResult validation = validateUsers(ownerId, friendId);
        if (validation != FriendResult.SUCCESS) return validation;
        return friendProvider.deleteFavorite(ownerId, friendId) > 0
                ? FriendResult.SUCCESS : FriendResult.NOT_FAVORITE;
    }

    @Override
    public boolean isBlocked(int userId, int targetId) {
        return friendProvider.findBlocked(userId, targetId).isPresent();
    }

    @Override
    public boolean isBlockedEitherWay(int userId, int targetId) {
        return isBlocked(userId, targetId) || isBlocked(targetId, userId);
    }

    @Override
    public @NotNull Collection<User> getBlockedUsers(int userId) {
        return friendProvider.findBlockedList().stream().filter(entry -> entry.userId() == userId)
                .map(entry -> userProvider.findById(entry.blockedId())).flatMap(Optional::stream)
                .collect(Collectors.toList());
    }

    @Override
    public int getBlockedUserCount(int userId) {
        return getBlockedUsers(userId).size();
    }

    @Override
    public @NotNull FriendResult blockUser(int userId, int targetId) {
        FriendResult validation = validateUsers(userId, targetId);
        if (validation != FriendResult.SUCCESS) return validation;
        if (isBlocked(userId, targetId)) return FriendResult.ALREADY_BLOCKED;
        friendProvider.createBlocked(userId, targetId)
                .orElseThrow(() -> new FriendException("Failed to block user"));
        getFriendship(userId, targetId).ifPresent(friendship ->
                friendProvider.deleteFriendship(friendship.userOne(), friendship.userTwo()));
        friendProvider.deleteFavorite(userId, targetId);
        friendProvider.deleteFavorite(targetId, userId);
        friendProvider.deleteRequest(userId, targetId);
        friendProvider.deleteRequest(targetId, userId);
        return FriendResult.SUCCESS;
    }

    @Override
    public @NotNull FriendResult unblockUser(int userId, int targetId) {
        FriendResult validation = validateUsers(userId, targetId);
        if (validation != FriendResult.SUCCESS) return validation;
        return friendProvider.deleteBlocked(userId, targetId) > 0
                ? FriendResult.SUCCESS : FriendResult.NOT_BLOCKED;
    }

    @Override
    public @NotNull FriendSettings getSettings(int userId) {
        return friendProvider.findSettings(userId)
                .orElseGet(() -> FriendSettings.of(userId, true, true, false, true));
    }

    @Override
    public @NotNull FriendResult updateSettings(int userId, boolean allowRequests, boolean showServer, boolean allowFollow, boolean loginNotify) {
        if (userProvider.findById(userId).isEmpty()) return FriendResult.USER_NOT_FOUND;
        if (friendProvider.findSettings(userId).isEmpty())
            friendProvider.createSettings(userId)
                    .orElseThrow(() -> new FriendException("Failed to create settings for user " + userId));
        friendProvider.updateSettings(userId, FriendSettings.of(userId, allowRequests, showServer, allowFollow, loginNotify))
                .orElseThrow(() -> new FriendException("Failed to update settings for user " + userId));
        return FriendResult.SUCCESS;
    }

    @Override
    public @NotNull FriendResult setAllowRequests(int userId, boolean enabled) {
        FriendSettings settings = getSettings(userId);
        return updateSettings(userId, enabled, settings.showServer(), settings.allowFollow(), settings.loginNotify());
    }

    @Override
    public @NotNull FriendResult setShowServer(int userId, boolean enabled) {
        FriendSettings settings = getSettings(userId);
        return updateSettings(userId, settings.allowRequests(), enabled, settings.allowFollow(), settings.loginNotify());
    }

    @Override
    public @NotNull FriendResult setAllowFollow(int userId, boolean enabled) {
        FriendSettings settings = getSettings(userId);
        return updateSettings(userId, settings.allowRequests(), settings.showServer(), enabled, settings.loginNotify());
    }

    @Override
    public @NotNull FriendResult setLoginNotify(int userId, boolean enabled) {
        FriendSettings settings = getSettings(userId);
        return updateSettings(userId, settings.allowRequests(), settings.showServer(), settings.allowFollow(), enabled);
    }

    @Override
    public @NotNull FriendResult resetSettings(int userId) {
        return updateSettings(userId, true, true, false, true);
    }

    @Override
    public boolean canViewServer(int viewerId, int targetId) {
        return areFriends(viewerId, targetId) && !isBlockedEitherWay(viewerId, targetId)
                && getSettings(targetId).showServer();
    }

    @Override
    public boolean canFollow(int followerId, int targetId) {
        return canViewServer(followerId, targetId) && getSettings(targetId).allowFollow();
    }

    @Override
    public boolean shouldNotifyLogin(int recipientId, int loggedInUserId) {
        return areFriends(recipientId, loggedInUserId) && !isBlockedEitherWay(recipientId, loggedInUserId)
                && getSettings(recipientId).loginNotify();
    }

    private FriendResult validateUsers(int userId, int targetId) {
        if (userId == targetId) return FriendResult.SELF_TARGET;
        if (userProvider.findById(userId).isEmpty() || userProvider.findById(targetId).isEmpty())
            return FriendResult.USER_NOT_FOUND;
        return FriendResult.SUCCESS;
    }

    private void deleteExpiredRequest(int senderId, int receiverId) {
        friendProvider.findRequest(senderId, receiverId).filter(FriendRequest::isExpired)
                .ifPresent(request -> friendProvider.deleteRequest(senderId, receiverId));
    }

    private FriendResult removeRequest(int senderId, int receiverId) {
        FriendResult validation = validateUsers(senderId, receiverId);
        if (validation != FriendResult.SUCCESS) return validation;
        Optional<FriendRequest> request = friendProvider.findRequest(senderId, receiverId);
        if (request.isEmpty()) return FriendResult.REQUEST_NOT_FOUND;
        if (friendProvider.deleteRequest(senderId, receiverId) == 0) return FriendResult.REQUEST_NOT_FOUND;
        return request.get().isExpired() ? FriendResult.REQUEST_EXPIRED : FriendResult.SUCCESS;
    }
}
