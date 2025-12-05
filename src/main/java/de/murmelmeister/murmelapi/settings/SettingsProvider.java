package de.murmelmeister.murmelapi.settings;

import java.util.List;

public interface SettingsProvider {
    void refreshCache();

    Settings get(String tag);

    List<Settings> getAll();

    Settings upsert(Settings settings);

    int delete(String tagId);
}
