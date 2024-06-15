package de.murmelmeister.murmelapi.utils;

import java.util.regex.Pattern;

/**
 * Utility class for string operations.
 */
public final class StringUtil {
    public static final Pattern VALID_SQL_PATTERN = Pattern.compile("^[a-zA-Z0-9-_.?!*<>:/]+$");

    /**
     * Checks if a {@link String} starts with another {@link String} ignoring case sensitivity.
     *
     * @param str    the input {@link String} to check
     * @param prefix the prefix to check against
     * @return {@code true} if the input {@link String} starts with the specified prefix, ignoring case sensitivity,
     *         or {@code false} otherwise. Returns {@code false} if either the input {@link String} or the prefix is null.
     */
    public static boolean startsWithIgnoreCase(final String str, final String prefix) {
        return str != null &&
               prefix != null &&
               str.length() >= prefix.length() &&
               str.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    /**
     * Checks if the given value is a valid SQL argument.
     *
     * @param value the value to check
     * @return the same value passed as the parameter if it is valid
     * @throws IllegalArgumentException if the value contains invalid characters. Only alphanumerics and -_.?!*<>:/ are allowed.
     */
    public static <T> T checkArgumentSQL(T value) {
        if (!VALID_SQL_PATTERN.matcher(value.toString()).matches())
            throw new IllegalArgumentException(value + " contains invalid characters. Only alphanumerics and -_.?!*<>:/ are allowed.");
        return value;
    }
}
