package de.murmelmeister.murmelapi.language;

/**
 * Represents a language with an ID and a name.
 * This class is used to manage different languages in the application.
 */
public class Language {
    private final int id;
    private String name;

    public Language(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
