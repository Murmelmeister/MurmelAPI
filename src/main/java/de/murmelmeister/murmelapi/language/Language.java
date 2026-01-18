package de.murmelmeister.murmelapi.language;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a language with an ID and a language code, stored as a BCP 47 string.
 * This class is used to manage different languages in the application.
 */
public record Language(int id, @NotNull String code) {
    public @NotNull Language withCode(String code) {
        return new Language(id, code != null ? code : this.code);
    }
}
