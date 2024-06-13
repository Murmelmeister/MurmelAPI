package de.murmelmeister.murmelapi.utils;

import java.util.regex.Pattern;

/**
 * Utility class for string operations.
 */
public final class StringUtil {
    /**
     * Check if the string starts with the given prefix ignoring the case.
     * If the string is null or shorter than the prefix, the method will return false.
     * Otherwise, the method will return true if the string starts with the prefix ignoring the case.
     *
     * @param str    String to check
     * @param prefix Prefix to check
     * @return True if the string starts with the prefix ignoring the case, otherwise false
     */
    public static boolean startsWithIgnoreCase(final String str, final String prefix) {
        if (str == null) return false;
        if (str.length() < prefix.length()) return false;
        return str.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    /**
     * Check if the value is valid for a SQL query.
     * The value must only contain letters, numbers, and the following characters: - . ? !
     * If the value is not valid, an IllegalArgumentException will be thrown.
     *
     * @param value Value to check
     * @param <T>   Type of the value
     * @return The value if it is valid
     * @throws IllegalArgumentException If the value is not valid
     */
    public static <T> T checkArgumentSQL(T value) {
        Pattern pattern = Pattern.compile("^[a-zA-Z0-9-.?!*<>:/]+$");
        if (!pattern.matcher(value.toString()).matches()) throw new IllegalArgumentException(value + " is not valid");
        return value;
    }
}
