package de.murmelmeister.murmelapi.settings;

import java.util.List;

public interface SettingsProvider {
    void refreshCache();

    Settings findById(String tag);

    List<Settings> findAll();

    Settings create(String tagId, String json);

    Settings update(String tagId, String json);

    Settings upsert(Settings settings);

    int delete(String tagId);
}
