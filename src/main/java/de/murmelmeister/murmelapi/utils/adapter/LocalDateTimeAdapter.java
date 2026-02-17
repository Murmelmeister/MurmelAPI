package de.murmelmeister.murmelapi.utils.adapter;

import com.google.gson.*;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.time.LocalDateTime;

public final class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
    @Override
    public @NotNull LocalDateTime deserialize(@NotNull JsonElement json, Type typeOfT, JsonDeserializationContext context) throws
            JsonParseException {
        return LocalDateTime.parse(json.getAsString());
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.toString());
    }
}
