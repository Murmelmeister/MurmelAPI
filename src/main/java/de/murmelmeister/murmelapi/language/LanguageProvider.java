package de.murmelmeister.murmelapi.language;

import java.util.List;

public interface LanguageProvider {
    void refreshCache();

    Language get(int id);

    Language get(String code);

    List<Language> getLanguages();

    Language create(String code);

    int delete(int id);

    Language update(int id, String code);
}
