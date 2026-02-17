package de.murmelmeister.murmelapi.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SettingsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SettingsService.class);
    private final SettingsProvider provider;
    private final Gson gson;

    public SettingsService(@NotNull SettingsProvider provider) {
        this.provider = provider;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public <T> T get(@NotNull String tagId, @NotNull Class<T> type, T defaultValue) {
        Settings settings = provider.findById(tagId);
        if (settings == null) return defaultValue;

        try {
            return gson.fromJson(settings.json(), type);
        } catch (Exception e) {
            LOGGER.warn("Invalid JSON for setting {}: {}", tagId, settings.json(), e);
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
