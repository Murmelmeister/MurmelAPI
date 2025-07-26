package de.murmelmeister.murmelapi.language;

/**
 * Represents a language with an ID and a name.
 * This class is used to manage different languages in the application.
 */
public record Language(int id, String name) {
    public Language withName(String name) {
        return new Language(id, name != null ? name : this.name);
    }
}
