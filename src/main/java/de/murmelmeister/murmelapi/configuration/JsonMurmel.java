package de.murmelmeister.murmelapi.configuration;

import com.google.gson.*;
import de.murmelmeister.murmelapi.exceptions.JsonMurmelException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class JsonMurmel {
    private final Path path;
    private final Gson gson;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public JsonMurmel(String fileName) {
        this.path = Paths.get(fileName);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public <T> T getValue(String key, Class<T> type) {
        lock.readLock().lock();
        try {
            if (Files.notExists(path))
                return null;

            try {
                String content = Files.readString(path); // The max file size is 2GB
                JsonObject root = JsonParser.parseString(content).getAsJsonObject();

                // Split the key into parts and navigate to the correct location in the JSON object
                String[] keys = key.split("\\.");
                JsonElement current = root;
                for (String part : keys) {
                    if (current == null || current.isJsonNull() || !current.isJsonObject())
                        return null; // Key isn't found or not an object
                    current = current.getAsJsonObject().get(part);
                }

                JsonElement element = current;
                if (element == null || element.isJsonNull())
                    return null; // Key isn't found
                return gson.fromJson(element, type);
            } catch (IOException e) {
                throw new JsonMurmelException("Error reading file: " + path, e);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public <T> T getValue(String key, Class<T> type, T fallback) {
        T value = getValue(key, type);
        if (value != null)
            return value;
        setValue(key, fallback);
        return fallback;
    }

    public <T> void setValue(String key, T value) {
        lock.writeLock().lock();
        try {
            JsonObject root;
            // Read the existing JSON file or create an object if it doesn't exist
            if (Files.exists(path)) {
                try {
                    String content = Files.readString(path); // The max file size is 2GB
                    root = JsonParser.parseString(content).getAsJsonObject();
                } catch (IOException e) {
                    throw new JsonMurmelException("Error reading file: " + path, e);
                }
            } else root = new JsonObject();

            // Split the key into parts and navigate to the correct location in the JSON object
            String[] keys = key.split("\\.");
            JsonObject current = root;
            for (int i = 0; i < keys.length - 1; i++) {
                String part = keys[i];
                if (!current.has(part) || !current.get(part).isJsonObject())
                    current.add(part, new JsonObject());
                current = current.getAsJsonObject(part);
            }

            String lastKey = keys[keys.length - 1];
            current.add(lastKey, gson.toJsonTree(value));

            // Write the updated JSON back to the file
            try {
                if (path.getParent() != null)
                    Files.createDirectories(path.getParent());

                Files.write(path, gson.toJson(root).getBytes());
            } catch (IOException e) {
                throw new JsonMurmelException("Error writing to file: " + path, e);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeKey(String key) {
        lock.writeLock().lock();
        try {
            JsonObject root;
            if (Files.exists(path)) {
                try {
                    String content = Files.readString(path); // The max file size is 2GB
                    root = JsonParser.parseString(content).getAsJsonObject();
                } catch (IOException e) {
                    throw new JsonMurmelException("Error reading file: " + path, e);
                }
            } else return;

            // Split the key into parts and navigate to the correct location in the JSON object
            String[] keys = key.split("\\.");
            int depth = keys.length;

            JsonObject[] nodeStack = new JsonObject[depth];
            String[] keyStack = new String[depth];

            JsonObject current = root;
            for (int i = 0; i < depth - 1; i++) {
                String part = keys[i];
                if (!current.has(part) || !current.get(part).isJsonObject())
                    return; // Key isn't found or not an object
                nodeStack[i] = current;
                keyStack[i] = part;
                current = current.getAsJsonObject(part);
            }

            nodeStack[depth - 1] = current;
            keyStack[depth - 1] = keys[depth - 1];

            String lastKey = keys[depth - 1];
            if (current.has(lastKey)) {
                current.remove(lastKey);

                // If the current node is empty after removing the key, remove it from its parent
                for (int level = depth - 1; level > 0; level--) {
                    JsonObject removed = nodeStack[level];
                    if (!removed.isEmpty()) break;
                    JsonObject parent = nodeStack[level - 1];
                    String parentKey = keyStack[level - 1];
                    parent.remove(parentKey);
                }

                // Write the updated JSON back to the file
                try {
                    Files.write(path, gson.toJson(root).getBytes());
                } catch (IOException e) {
                    throw new JsonMurmelException("Error writing to file: " + path, e);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}
