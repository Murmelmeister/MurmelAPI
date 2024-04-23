package de.murmelmeister.murmelapi.utils;

import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Manage the files
 */
public final class FileUtil {
    /**
     * Create a file.
     *
     * @param logger   Logger
     * @param path     Path of the file
     * @param fileName Name of the file
     * @return File
     */
    public static File createFile(Logger logger, String path, String fileName) {
        File file = new File(path, fileName);
        File parent = file.getParentFile();
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
    }

    /**
     * Load a file.
     *
     * @param file   File
     * @return Properties
     */
    public static Properties loadProperties(File file) {
        try {
            Properties properties = new Properties(System.getProperties());
            properties.load(file.toURI().toURL().openStream());
            return properties;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
