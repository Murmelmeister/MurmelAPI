package de.murmelmeister.murmelapi.language;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a language with an ID and a language code, stored as a BCP 47 string.
 * This class is used to manage different languages in the application.
 */
public record Language(
        int id,
        @NotNull String code
) {
    public static @NotNull Builder builder(@NotNull Language language) {
        return new Builder(language);
    }

    public static class Builder {
        private final int id;

        private String code;

        private Builder(@NotNull Language language) {
            this.id = language.id();
            this.code = language.code();
        }

        public Builder code(@NotNull String code) {
            this.code = code;
            return this;
        }

        public @NotNull Language build() {
            return new Language(id, code);
        }
    }
}
