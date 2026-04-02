package de.murmelmeister.murmelapi.settings;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.function.Consumer;

public interface Settings {
    @NotNull String tagId();

    @NotNull String json();

    @NotNull LocalDateTime updatedAt();

    @NotNull Builder builder();

    @NotNull Settings with(@NotNull Consumer<Builder> consumer);

    static @NotNull Settings of(@NotNull String tagId, @NotNull String json, @NotNull LocalDateTime updatedAt) {
        return new SettingsImpl(tagId, json, updatedAt);
    }

    interface Builder {
        @NotNull Builder json(@NotNull String json);

        @NotNull Builder updatedAt(@NotNull LocalDateTime updatedAt);

        @NotNull Settings build();
    }
}
