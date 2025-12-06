package de.murmelmeister.murmelapi.language;

import java.util.List;

public interface LanguageProvider {
    void refreshCache();

    Language findById(int id);

    Language findByCode(String code);

    List<Language> findAll();

    Language create(String code);

    int delete(int id);

    Language update(int id, String code);

    Language upsert(Language language);
}
