package de.murmelmeister.murmelapi.settings;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SettingsService {
    private final Logger logger = LoggerFactory.getLogger(SettingsService.class);
    private final SettingsProvider provider;
    private final Gson gson;

    public SettingsService(@NotNull SettingsProvider provider, @NotNull Gson gson) {
        this.provider = provider;
        this.gson = gson;
    }

    public <T> T get(@NotNull String tagId, @NotNull Class<T> type, T defaultValue) {
        Settings settings = provider.findById(tagId);
        if (settings == null) return defaultValue;

        try {
            return gson.fromJson(settings.json(), type);
        } catch (Exception e) {
            logger.warn("Invalid JSON for setting {}: {}", tagId, settings.json(), e);
            return defaultValue;
        }
    }

    public <T> T get(@NotNull String tagId, @NotNull Class<T> type) {
        return get(tagId, type, null);
    }

    public <T> T get(@NotNull Settings settings, @NotNull Class<T> type) {
        return get(settings.tagId(), type);
    }

    public <T> Settings set(@NotNull String tagId, @NotNull T value) {
        String json = gson.toJson(value);
        return provider.upsert(tagId, json);
    }

    public int delete(@NotNull String tagId) {
        return provider.delete(tagId);
    }
}
