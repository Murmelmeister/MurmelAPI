package de.murmelmeister.murmelapi.language;

import java.util.List;

public interface LanguageProvider {
    void closeCache();

    void refreshCache();

    Language get(int id);

    Language get(String name);

    List<Language> getLanguages();

    Language create(String name);

    int delete(int id);

    Language update(int id, String name);
}
