package de.murmelmeister.murmelapi.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

// Thanks Minestom: https://github.com/Minestom/Minestom/blob/master/src/main/java/net/minestom/server/utils/mojang/MojangUtils.java
public final class MojangUtils {
    private static final String FROM_UUID_URL = "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false";
    private static final String FROM_USERNAME_URL = "https://api.mojang.com/users/profiles/minecraft/%s";

    public static UUID getUUID(String username) throws IOException, URISyntaxException {
        return UUID.fromString(retrieve(FROM_USERNAME_URL.replace("%s", username)).get("id")
                .getAsString()
                .replaceFirst(
                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                        "$1-$2-$3-$4-$5"
                ));
    }

    public static String getUsername(UUID playerUUID) throws IOException, URISyntaxException {
        return retrieve(FROM_UUID_URL.replace("%s", playerUUID.toString())).get("name").getAsString();
    }

    public static JsonObject fromUuid(UUID uuid) {
        return fromUuid(uuid.toString());
    }

    public static JsonObject fromUuid(String uuid) {
        try {
            return retrieve(FROM_UUID_URL.replace("%s", uuid));
        } catch (IOException | URISyntaxException e) {
            return null;
        }
    }

    public static JsonObject fromUsername(String username) {
        try {
            return retrieve(FROM_USERNAME_URL.replace("%s", username));
        } catch (IOException | URISyntaxException e) {
            return null;
        }
    }

    private static JsonObject retrieve(String url) throws IOException, URISyntaxException {
        final String response = URLUtils.getText(url);
        if (response.isEmpty()) throw new IOException("The Mojang API is down :(");
        JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
        if (jsonObject.has("errorMessage")) throw new IOException(jsonObject.get("errorMessage").getAsString());
        return jsonObject;
    }
}
