package de.murmelmeister.murmelapi.language.message;

public record LocalizedMessage(int languageId, String message) {
    public static LocalizedMessage of(int languageId, String message) {
        return new LocalizedMessage(languageId, message);
    }

    public LocalizedMessage format(Object... args) {
        return new LocalizedMessage(languageId, String.format(message, args));
    }
}
