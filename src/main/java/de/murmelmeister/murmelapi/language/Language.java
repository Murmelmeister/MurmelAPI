package de.murmelmeister.murmelapi.language;

import java.util.Locale;

/**
 * Represents a language with an ID and a {@link Locale}.
 * This class is used to manage different languages in the application.
 */
public record Language(int id, Locale locale) {
    public Language withLocale(Locale locale) {
        return new Language(id, locale != null ? locale : this.locale);
    }

    public String languageTag() {
        return locale.toLanguageTag();
    }
}
