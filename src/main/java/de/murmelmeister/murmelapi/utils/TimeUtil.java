package de.murmelmeister.murmelapi.utils;


import de.murmelmeister.murmelapi.language.message.MessageService;
import de.murmelmeister.murmelapi.language.message.MurmelMessage;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for time-related operations.
 */
public final class TimeUtil {
    private static final Pattern TIME_PATTERN = Pattern.compile("^(\\d+)([smhdwMy])$");

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

    public static String formatDuration(MessageService messageService, int languageId, long totalSeconds) {
        if (totalSeconds <= 0) return "0 seconds";

        Duration duration = Duration.ofSeconds(totalSeconds);
        long totalDays = duration.toDays();
        long years = totalDays / 365;
        long days = totalDays % 365;
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        StringBuilder builder = new StringBuilder();
        if (years > 0) builder.append(years).append(" ").append(years == 1 ?
                messageService.getMessage(MurmelMessage.TIME_YEAR_SINGULAR, languageId) : messageService.getMessage(MurmelMessage.TIME_YEAR_PLURAL, languageId)).append(" ");
        if (days > 0) builder.append(days).append(" ").append(days == 1 ?
                messageService.getMessage(MurmelMessage.TIME_DAY_SINGULAR, languageId) : messageService.getMessage(MurmelMessage.TIME_DAY_PLURAL, languageId)).append(" ");
        if (hours > 0) builder.append(hours).append(" ").append(hours == 1 ?
                messageService.getMessage(MurmelMessage.TIME_HOUR_SINGULAR, languageId) : messageService.getMessage(MurmelMessage.TIME_HOUR_PLURAL, languageId)).append(" ");
        if (minutes > 0) builder.append(minutes).append(" ").append(minutes == 1 ?
                messageService.getMessage(MurmelMessage.TIME_MINUTE_SINGULAR, languageId) : messageService.getMessage(MurmelMessage.TIME_MINUTE_PLURAL, languageId)).append(" ");
        if (seconds > 0) builder.append(seconds).append(" ").append(seconds == 1 ?
                messageService.getMessage(MurmelMessage.TIME_SECOND_SINGULAR, languageId) : messageService.getMessage(MurmelMessage.TIME_SECOND_PLURAL, languageId));
        return builder.toString().trim();
    }

    public static String formatDurationForScoreboard(MessageService messageService, int languageId, long totalSeconds) {
        if (totalSeconds < 3600) return "0 hours";

        Duration duration = Duration.ofSeconds(totalSeconds);
        long totalDays = duration.toDays();
        long years = totalDays / 365;
        long days = totalDays % 365;
        long hours = duration.toHours() % 24;

        StringBuilder builder = new StringBuilder();
        if (years > 0) builder.append(years).append(" ").append(years == 1 ?
                messageService.getMessage(MurmelMessage.TIME_YEAR_SINGULAR, languageId) : messageService.getMessage(MurmelMessage.TIME_YEAR_PLURAL, languageId)).append(" ");
        if (days > 0) builder.append(days).append(" ").append(days == 1 ?
                messageService.getMessage(MurmelMessage.TIME_DAY_SINGULAR, languageId) : messageService.getMessage(MurmelMessage.TIME_DAY_PLURAL, languageId)).append(" ");
        if (hours > 0) builder.append(hours).append(" ").append(hours == 1 ?
                messageService.getMessage(MurmelMessage.TIME_HOUR_SINGULAR, languageId) : messageService.getMessage(MurmelMessage.TIME_HOUR_PLURAL, languageId));
        return builder.toString().trim();
    }
}
