package de.murmelmeister.murmelapi.configuration;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * MurmelConfiguration is a configuration management class that extends {@link MurmelMemory}
 * for handling YAML configuration files. It provides functionality to load and
 * save configurations in a thread-safe manner.
 */
public class MurmelConfiguration extends MurmelMemory {
    private final Yaml yaml;
    private final ReadWriteLock lock = new ReentrantReadWriteLock(true);
    private final Lock writeLock = lock.writeLock();

    public MurmelConfiguration() {
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setPrettyFlow(true);
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setMaxAliasesForCollections(Integer.MAX_VALUE);
        loaderOptions.setCodePointLimit(Integer.MAX_VALUE);
        Representer representer = new Representer(dumperOptions);
        Constructor constructor = new Constructor(loaderOptions);
        this.yaml = new Yaml(constructor, representer, dumperOptions, loaderOptions);
    }

    /**
     * Loads the configuration data from the specified file.
     *
     * @param file the file to load the data from
     */
    public void load(File file) {
        writeLock.lock();
        try (Reader reader = new FileReader(file)) {
            Map<String, Object> loadedData = yaml.load(reader);
            data.clear();
            if (loadedData != null) {
                flattenData("", loadedData);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Saves the configuration data to the specified file.
     *
     * @param file the file to save the data to
     */
    public void save(File file) {
        writeLock.lock();
        try (Writer writer = new FileWriter(file)) {
            Map<String, Object> nestedData = nestData(data);
            yaml.dump(nestedData, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Loads the configuration data from the specified file and returns a MurmelConfiguration object.
     *
     * @param file the file to load the data from
     * @return a MurmelConfiguration object with the loaded configuration data
     */
    public static MurmelConfiguration loadConfiguration(File file) {
        MurmelConfiguration config = new MurmelConfiguration();
        config.load(file);
        return config;
    }

    /**
     * Recursively flattens a nested map by concatenating keys with a specified prefix and stores
     * the resulting key-value pairs into a flat structure.
     *
     * @param prefix the prefix to be used for the keys in the flattened map
     * @param map the nested map that needs to be flattened
     */
    @SuppressWarnings("unchecked")
    private void flattenData(String prefix, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            if (entry.getValue() instanceof Map) {
                flattenData(key, (Map<String, Object>) entry.getValue());
            } else {
                data.put(key, entry.getValue());
            }
        }
    }

    /**
     * Converts a map with dot-separated keys into a nested map structure.
     *
     * @param flatData a map where keys are dot-separated paths indicating the nesting structure
     * @return a nested map constructed based on the dot-separated keys
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> nestData(Map<String, Object> flatData) {
        Map<String, Object> nestedData = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : flatData.entrySet()) {
            String[] path = entry.getKey().split("\\.");
            Map<String, Object> currentMap = nestedData;
            for (int i = 0; i < path.length - 1; i++) {
                currentMap = (Map<String, Object>) currentMap.computeIfAbsent(path[i], k -> new LinkedHashMap<>());
            }
            currentMap.put(path[path.length - 1], entry.getValue());
        }
        return nestedData;
    }
}
