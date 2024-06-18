package de.murmelmeister.murmelapi.utils;

import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
        var lock = getLockForFile(fileName);
        lock.lock();
        try {
            var file = new File(path, fileName);
            var parent = file.getParentFile();
            if (!parent.exists()) {
                var exist = parent.mkdirs();
                if (!exist) logger.warn("Cloud not create the directory: {}", parent);
            }

            if (!file.exists()) {
                try {
                    var exist = file.createNewFile();
                    if (!exist) logger.error("Cloud not create the file: {}", file);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return file;
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
        var properties = new Properties(System.getProperties());
        try (var stream = new FileInputStream(file)) {
            properties.load(stream);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load properties from file: " + file.getName(), e);
        }
        return properties;
    }
}
