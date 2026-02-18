package de.murmelmeister.murmelapi.utils.adapter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.time.LocalDate;

public final class LocalDateAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
    @Override
    public @NotNull LocalDate deserialize(@NotNull JsonElement json, Type typeOfT, JsonDeserializationContext context) throws
            JsonParseException {
        return LocalDate.parse(json.getAsString());
    }

    @Override
    public @NotNull JsonElement serialize(@NotNull LocalDate src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.toString());
    }
}
