package de.murmelmeister.murmelapi.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SettingsService {
    private final Logger logger = LoggerFactory.getLogger(SettingsService.class);
    private final SettingsProvider provider;
    private final Gson gson;

    public SettingsService(SettingsProvider provider) {
        this.provider = provider;
        this.gson = new GsonBuilder().disableHtmlEscaping().create();
    }

    public <T> T get(String tagId, Class<T> type, T defaultValue) {
        Settings settings = provider.get(tagId);
        if (settings == null) return defaultValue;

        try {
            return gson.fromJson(settings.json(), type);
        } catch (Exception e) {
            logger.warn("Invalid JSON for setting {}: {}", tagId, settings.json(), e);
            return defaultValue;
        }
    }

    public <T> T get(String tagId, Class<T> type) {
        return get(tagId, type, null);
    }

    public <T> T get(Settings settings, Class<T> type) {
        return get(settings.tagId(), type);
    }

    public <T> Settings set(String tagId, T value) {
        String json = gson.toJson(value);
        return provider.upsert(new Settings(tagId, json, null));
    }

    public int delete(String tagId) {
        return provider.delete(tagId);
    }
}
