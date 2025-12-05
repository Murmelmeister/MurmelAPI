package de.murmelmeister.murmelapi.settings;

import java.util.List;

public interface SettingsProvider {
    void refreshCache();

    Settings findById(String tag);

    List<Settings> findAll();

    Settings upsert(Settings settings);

    int delete(String tagId);
}
