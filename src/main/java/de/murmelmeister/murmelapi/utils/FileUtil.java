package de.murmelmeister.murmelapi.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;

import java.io.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manage the files
 * (Thread-safe)
 */
public final class FileUtil {
    private static final Map<String, ReentrantLock> FILE_LOCKS = new ConcurrentHashMap<>();
    private static final Map<String, File> FILES = new ConcurrentHashMap<>();
    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();

    /**
     * Returns the lock associated with the specified file name. If a lock does not exist for the file name, a new lock is created and associated with the file name.
     * The method uses a ConcurrentHashMap to store the file locks, ensuring thread-safe access to the locks.
     *
     * @param fileName the name of the file
     * @return the lock associated with the file name
     */
    private static ReentrantLock getLockForFile(String fileName) {
        FILE_LOCKS.putIfAbsent(fileName, new ReentrantLock());
        return FILE_LOCKS.get(fileName);
    }

    /**
     * Creates a new file with the specified path and file name. If the parent directory does not exist, it will be created.
     * If the file already exists, no new file will be created.
     * This method is thread-safe, as it locks the file using a unique lock obtained from the getLockForFile method.
     *
     * @param logger   the logger to log warning and error messages
     * @param path     the path to the parent directory where the file will be created
     * @param fileName the name of the file to be created
     * @return the created file
     * @throws RuntimeException if an I/O error occurs during file creation
     * @see FileUtil#getLockForFile(String)
     */
    public static File createFile(Logger logger, String path, String fileName) {
        ReentrantLock lock = getLockForFile(fileName);
        lock.lock();
        try {
            File file = new File(path, fileName);
            File parent = file.getParentFile();
            if (!parent.exists()) {
                boolean exist = parent.mkdirs();
                if (!exist) logger.warn("Cloud not create the directory: {}", parent);
            }

            if (!file.exists()) {
                try {
                    boolean exist = file.createNewFile();
                    if (!exist) logger.error("Cloud not create the file: {}", file);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            FILES.put(fileName, file);
            return file;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Loads JSON data from a specified file and deserializes it into an object of the given class.
     * This method ensures thread-safe access to the file using a unique lock associated with the file name.
     *
     * @param <T>      the type of the object to be deserialized
     * @param logger   the logger to log warning and error messages
     * @param path     the path to the parent directory where the file is located
     * @param fileName the name of the file containing the JSON content
     * @param clazz    the class of the object to be deserialized
     * @return an object of type T deserialized from the JSON content of the file
     */
    public static <T> T loadJson(Logger logger, String path, String fileName, Class<T> clazz) {
        ReentrantLock lock = getLockForFile(fileName);
        lock.lock();
        try {
            File file;
            if (!FILES.containsKey(fileName))
                file = createFile(logger, path, fileName);
            else file = FILES.get(fileName);
            return fromJson(file, clazz);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Serializes the specified object into its JSON representation and writes it to a file with the given name.
     * The file must be pre-loaded into the FILES map, otherwise an error will be logged.
     * This method ensures thread-safe access by acquiring a lock associated with the file.
     *
     * @param <T>     the type of the object to be serialized
     * @param logger  the logger to log error messages
     * @param fileName the name of the file to which the JSON representation of the object will be written
     * @param json    the object to be serialized to JSON format
     */
    public static <T> void saveJson(Logger logger, String fileName, T json) {
        ReentrantLock lock = getLockForFile(fileName);
        lock.lock();
        try {
            if (!FILES.containsKey(fileName)) {
                logger.error("No such file loaded: {}", fileName);
                return;
            }
            File file = FILES.get(fileName);
            toJson(file, json);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Checks if a JSON file with the specified name exists at the given path.
     * This method ensures thread-safe access by using a lock associated with the file name.
     *
     * @param path the path to the directory containing the file
     * @param fileName the name of the file to check for existence
     * @return true if the file exists, false otherwise
     */
    public static boolean existsJson(String path, String fileName) {
        ReentrantLock lock = getLockForFile(fileName);
        lock.lock();
        try {
            if (!FILES.containsKey(fileName)) {
                File file = new File(path, fileName);
                return file.exists();
            }
            return FILES.get(fileName).exists();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Load a file.
     *
     * @param file File
     * @return Properties
     */
    public static Properties loadProperties(File file) {
        ReentrantLock lock = getLockForFile(file.getName());
        lock.lock();
        try (FileInputStream stream = new FileInputStream(file)) {
            Properties properties = new Properties(System.getProperties());
            properties.load(stream);
            return properties;
        } catch (IOException e) {
            throw new RuntimeException("Unable to load properties from file: " + file.getName(), e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Serializes the specified object into its JSON representation and writes it to the provided file.
     * This method ensures thread-safe file writing by acquiring a lock associated with the file name.
     *
     * @param <T>  the type of the object to be serialized
     * @param file the file to which the JSON representation of the object will be written
     * @param json the object to be serialized to JSON format
     */
    public static <T> void toJson(File file, T json) {
        ReentrantLock lock = getLockForFile(file.getName());
        lock.lock();
        try (Writer writer = new FileWriter(file)) {
            GSON.toJson(json, writer);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write JSON to file: " + file.getName(), e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Deserializes the JSON content of a specified file into an object of the given class.
     *
     * @param file  the file containing the JSON content
     * @param clazz the class of the object to be deserialized
     * @param <T>   the type of the object to be deserialized
     * @return an object of type T deserialized from the JSON content of the file
     * @throws RuntimeException if an I/O error occurs during file reading or deserialization
     */
    public static <T> T fromJson(File file, Class<T> clazz) {
        ReentrantLock lock = getLockForFile(file.getName());
        lock.lock();
        try (Reader reader = new FileReader(file)) {
            return GSON.fromJson(reader, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON from file: " + file.getName(), e);
        } finally {
            lock.unlock();
        }
    }
}
