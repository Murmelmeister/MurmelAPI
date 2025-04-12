package de.murmelmeister.murmelapi.utils;

/**
 * Utility class for string operations.
 */
public final class StringUtil {
    /**
     * Checks if a {@link String} starts with another {@link String} ignoring case sensitivity.
     *
     * @param str    the input {@link String} to check
     * @param prefix the prefix to check against
     * @return {@code true} if the input {@link String} starts with the specified prefix, ignoring case sensitivity,
     * or {@code false} otherwise. Returns {@code false} if either the input {@link String} or the prefix is null.
     */
    public static boolean startsWithIgnoreCase(final String str, final String prefix) {
        return str != null &&
               prefix != null &&
               str.length() >= prefix.length() &&
               str.regionMatches(true, 0, prefix, 0, prefix.length());
    }
}
