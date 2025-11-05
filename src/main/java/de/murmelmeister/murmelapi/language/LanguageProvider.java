package de.murmelmeister.murmelapi.language;

import java.util.List;
import java.util.Locale;

public interface LanguageProvider {
    void closeCache();

    void refreshCache();

    Language get(int id);

    Language get(Locale locale);

    List<Language> getLanguages();

    Language create(Locale locale);

    int delete(int id);

    Language update(int id, Locale locale);
}
