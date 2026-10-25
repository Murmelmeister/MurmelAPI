package de.murmelmeister.murmelapi.redis;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshOrigin;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;

/**
 * The key retains its JSON type
 */
public record RefreshMessage(String sender, String type, JsonElement key) {

    public RefreshMessage {
        if (sender == null || sender.isBlank() || type == null || type.isBlank())
            throw new IllegalArgumentException("Refresh sender and type must not be blank.");
    }

    public static RefreshMessage fromEvent(String sender, RefreshEvent<?> event, Gson gson) {
        return new RefreshMessage(sender, event.type(), event.key() == null ? null : gson.toJsonTree(event.key()));
    }

    public static RefreshMessage decode(String json, Gson gson) {
        JsonObject object = gson.fromJson(json, JsonObject.class);

        if (object == null || !object.has("sender") || !object.has("type"))
            throw new IllegalArgumentException("Invalid refresh message fields.");

        if (!object.get("sender").isJsonPrimitive() || !object.get("sender").getAsJsonPrimitive().isString()
                || !object.get("type").isJsonPrimitive() || !object.get("type").getAsJsonPrimitive().isString())
            throw new IllegalArgumentException("Refresh sender and type must be strings.");

        return new RefreshMessage(object.get("sender").getAsString(), object.get("type").getAsString(), object.get("key"));
    }

    public String keyJson(Gson gson) {
        return key == null || key.isJsonNull() ? null : gson.toJson(key);
    }

    public void apply(RefreshProvider provider, Gson gson) {
        String json = keyJson(gson);
        if (json == null) provider.fireCache(type, RefreshOrigin.REMOTE);
        else provider.fireSingle(type, json, RefreshOrigin.REMOTE);
    }
}
