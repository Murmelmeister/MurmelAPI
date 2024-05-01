package de.murmelmeister.murmelapi.utils;


/**
 * Utility class for time-related operations.
 */
public final class TimeUtil {
    /**
     * Formats the given time string into a long value representing the time in milliseconds.
     * The time string should be a number followed by a time unit. The following time units are supported:
     * <p>
     * - "s" for seconds
     * <p>
     * - "m" for minutes
     * <p>
     * - "h" for hours
     * <p>
     * - "d" for days
     * <p>
     * - "w" for weeks
     * <p>
     * - "M" for months (approximated to 30 days)
     * <p>
     * - "y" for years (approximated to 365 days)
     * <p>
     * If the time string is "-1", it is considered as permanent and the method returns -1.
     * If the time string starts with "-", it is considered as invalid and the method returns -2.
     * If the time string does not end with a valid time unit, the method returns -3.
     *
     * @param args the time string to format
     * @return the formatted time in milliseconds, or -1, -2, -3 for the special cases described above
     * @throws RuntimeException if the time string (excluding the last character) cannot be parsed as a long value
     */
    public static long formatTime(String args) {
        if (args.equals("-1")) return -1L; // Permanent
        if (args.startsWith("-")) return -2L; // No negative value
        try {
            String format = args.substring(args.length() - 1);
            long duration = Long.parseLong(args.substring(0, args.length() - 1));
            return getTime(format, duration);
        } catch (NumberFormatException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the time in milliseconds based on the given format and duration.
     *
     * @param format   the time unit
     * @param duration the duration
     * @return the time in milliseconds
     */
    private static long getTime(String format, long duration) {
        long time;
        switch (format) {
            case "s" -> time = duration * 1000L;
            case "m" -> time = duration * 1000L * 60L;
            case "h" -> time = duration * 1000L * 60L * 60L;
            case "d" -> time = duration * 1000L * 60L * 60L * 24L;
            case "w" -> time = duration * 1000L * 60L * 60L * 24L * 7L;
            case "M" -> time = duration * 1000L * 60L * 60L * 24L * 30L;
            case "y" -> time = duration * 1000L * 60L * 60L * 24L * 365L;
            default -> time = -3L; // Wrong valid format
        }
        return time;
    }
}
