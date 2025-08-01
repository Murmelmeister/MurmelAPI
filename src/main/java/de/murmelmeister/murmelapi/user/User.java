package de.murmelmeister.murmelapi.user;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @param firstLogin Nullable
 */
public record User(int id, UUID mojangId, String username, LocalDateTime firstLogin, boolean systemUser,
                   boolean debugUser, boolean debugEnabled, int languageId) {

    public boolean debugMode() {
        return debugUser && debugEnabled;
    }

    public User withUpdateMeta(String username, LocalDateTime firstLogin,
                               Boolean debugUser, Boolean debugEnabled, Integer languageId) {
        return new User(id, mojangId,
                username != null ? username : this.username,
                firstLogin != null ? firstLogin : this.firstLogin,
                systemUser,
                debugUser != null ? debugUser : this.debugUser,
                debugEnabled != null ? debugEnabled : this.debugEnabled,
                languageId != null ? languageId : this.languageId);
    }
}
