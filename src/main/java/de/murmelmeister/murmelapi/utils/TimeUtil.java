package de.murmelmeister.murmelapi.utils;


import de.murmelmeister.murmelapi.time.PlayTime;
import de.murmelmeister.murmelapi.time.PlayTimeType;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for time-related operations.
 */
public final class TimeUtil {
    private static final Pattern TIME_PATTERN = Pattern.compile("^(\\d+)([smhdwMy])$");

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
    @Deprecated
    public static long formatTime(String args) {
        if (args.equals("-1")) return -1L; // Permanent
        if (args.startsWith("-")) return -2L; // No negative value

        Matcher matcher = TIME_PATTERN.matcher(args);
        if (!matcher.matches()) return -3L; // Invalid format

        long duration = Long.parseLong(matcher.group(1));
        String format = matcher.group(2);
        return getTime(format, duration);
    }

    public static long parseDurationInSeconds(String time) {
        if ("-1".equals(time)) return -1L; // Permanent
        if (time.startsWith("-")) return -2L; // No negative value

        Matcher matcher = TIME_PATTERN.matcher(time);
        if (!matcher.matches()) return -3L; // Invalid format

        long duration = Long.parseLong(matcher.group(1));
        return switch (matcher.group(2)) {
            case "s" -> duration;
            case "m" -> TimeUnit.MINUTES.toSeconds(duration);
            case "h" -> TimeUnit.HOURS.toSeconds(duration);
            case "d" -> TimeUnit.DAYS.toSeconds(duration);
            case "w" -> TimeUnit.DAYS.toSeconds(duration * 7L);
            case "M" -> TimeUnit.DAYS.toSeconds(duration * 30L); // Approximation for months
            case "y" -> TimeUnit.DAYS.toSeconds(duration * 365L); // Approximation for years
            default -> -4L; // Wrong valid format
        };
    }

    /**
     * Returns the time in milliseconds based on the given format and duration.
     *
     * @param format   the time unit
     * @param duration the duration
     * @return the time in milliseconds
     */
    @Deprecated
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


    /**
     * Formats the time value based on the given PlayTime and user ID.
     *
     * @param playTime The PlayTime object to get the time from.
     * @param userId   The ID of the user.
     * @return The formatted time value as a string.
     */
    @Deprecated
    public static String formatTimeValue(PlayTime playTime, int userId) {
        int currentTime = playTime.getTime(userId);
        return formatTimeValue(currentTime);
    }

    @Deprecated
    private static String formatTimeValue(int currentTime) {
        if (currentTime == 0) return "0 seconds";

        int years = PlayTimeType.YEARS.fromSeconds(currentTime);
        currentTime %= (PlayTimeType.YEARS.getMultiplier());

        int days = PlayTimeType.DAYS.fromSeconds(currentTime);
        currentTime %= (PlayTimeType.DAYS.getMultiplier());

        int hours = PlayTimeType.HOURS.fromSeconds(currentTime);
        currentTime %= (PlayTimeType.HOURS.getMultiplier());

        int minutes = PlayTimeType.MINUTES.fromSeconds(currentTime);
        int seconds = currentTime % PlayTimeType.MINUTES.getMultiplier();

        return (years != 0 ? getTimeValue(years, PlayTimeType.YEARS) + " " : "")
               + (days != 0 ? getTimeValue(days, PlayTimeType.DAYS) + " " : "")
               + (hours != 0 ? getTimeValue(hours, PlayTimeType.HOURS) + " " : "")
               + (minutes != 0 ? getTimeValue(minutes, PlayTimeType.MINUTES) + " " : "")
               + (seconds != 0 ? getTimeValue(seconds, PlayTimeType.SECONDS) : "").trim();
    }

    public static String formatDuration(long totalSeconds) {
        if (totalSeconds <= 0) return "0 seconds";

        Duration duration = Duration.ofSeconds(totalSeconds);
        long totalDays = duration.toDays();
        long years = totalDays / 365;
        long days = totalDays % 365;
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        // TODO: Language support for years, days, hours, minutes, seconds
        StringBuilder builder = new StringBuilder();
        if (years > 0) builder.append(years).append(" ").append(years == 1 ? "year" : "years").append(" ");
        if (days > 0) builder.append(days).append(" ").append(days == 1 ? "day" : "days").append(" ");
        if (hours > 0) builder.append(hours).append(" ").append(hours == 1 ? "hour" : "hours").append(" ");
        if (minutes > 0) builder.append(minutes).append(" ").append(minutes == 1 ? "minute" : "minutes").append(" ");
        if (seconds > 0) builder.append(seconds).append(" ").append(seconds == 1 ? "second" : "seconds");
        return builder.toString().trim();
    }

    /**
     * Formats a duration in milliseconds into a human-readable string representation.
     * The output includes years, days, hours, minutes, and seconds as applicable.
     *
     * @param durationInMilliseconds the time duration to be formatted, in milliseconds
     * @return a formatted string representation of the time duration
     */
    @Deprecated
    public static String formatTimeValue(long durationInMilliseconds) {
        if (durationInMilliseconds <= 0) return "0 seconds";

        long durationInSeconds = durationInMilliseconds / 1000;

        long years = durationInSeconds / (365L * 24 * 60 * 60);
        durationInSeconds %= (365L * 24 * 60 * 60);

        long days = durationInSeconds / (24 * 60 * 60);
        durationInSeconds %= (24 * 60 * 60);

        long hours = durationInSeconds / (60 * 60);
        durationInSeconds %= (60 * 60);

        long minutes = durationInSeconds / 60;
        long seconds = durationInSeconds % 60;

        return (years != 0 ? years + " " + (years == 1 ? "year" : "years") + " " : "")
               + (days != 0 ? days + " " + (days == 1 ? "day" : "days") + " " : "")
               + (hours != 0 ? hours + " " + (hours == 1 ? "hour" : "hours") + " " : "")
               + (minutes != 0 ? minutes + " " + (minutes == 1 ? "minute" : "minutes") + " " : "")
               + (seconds != 0 ? seconds + " " + (seconds == 1 ? "second" : "seconds") : "").trim();
    }

    /**
     * Formats the given play time into a scoreboard time string.
     *
     * @param playTime The PlayTime object to get the time from.
     * @param userId   The ID of the user.
     * @return The formatted time value as a string representing the time in days, hours, and years.
     */
    @Deprecated
    public static String formatScoreboardTime(PlayTime playTime, int userId) {
        int currentTime = playTime.getTime(userId);
        if (currentTime < 3600) return "0 hours";

        int years = PlayTimeType.YEARS.fromSeconds(currentTime);
        currentTime %= (PlayTimeType.YEARS.getMultiplier());

        int days = PlayTimeType.DAYS.fromSeconds(currentTime);
        int hours = currentTime % PlayTimeType.DAYS.getMultiplier();

        return (years != 0 ? getTimeValue(years, PlayTimeType.YEARS) + " " : "")
               + (days != 0 ? getTimeValue(days, PlayTimeType.DAYS) + " " : "")
               + (hours != 0 ? getTimeValue(hours, PlayTimeType.HOURS) : "").trim();
    }

    public static String formatDurationForScoreboard(long totalSeconds) {
        if (totalSeconds < 3600) return "0 hours";

        Duration duration = Duration.ofSeconds(totalSeconds);
        long totalDays = duration.toDays();
        long years = totalDays / 365;
        long days = totalDays % 365;
        long hours = duration.toHours() % 24;

        StringBuilder builder = new StringBuilder();
        if (years > 0) builder.append(years).append(" ").append(years == 1 ? "year" : "years").append(" ");
        if (days > 0) builder.append(days).append(" ").append(days == 1 ? "day" : "days").append(" ");
        if (hours > 0) builder.append(hours).append(" ").append(hours == 1 ? "hour" : "hours");
        return builder.toString().trim();
    }

    /**
     * Returns the formatted time value based on the given time and PlayTimeType.
     *
     * @param time the time value to format
     * @param type the PlayTimeType representing the time unit
     * @return the formatted time value as a string
     */
    @Deprecated
    private static String getTimeValue(int time, PlayTimeType type) {
        return time == 1 ? "1 " + type.getName().replace("s", "").toLowerCase() : time + " " + type.getName().toLowerCase();
    }
}
