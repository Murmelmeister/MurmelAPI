package de.murmelmeister.murmelapi.utils;

import de.murmelmeister.murmelapi.language.message.MessageService;
import de.murmelmeister.murmelapi.language.message.MurmelMessage;

import java.time.Duration;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing and formatting time-based values used across the plugin.
 */
public final class TimeUtil {
    private static final Pattern TIME_PATTERN = Pattern.compile("^(\\d+)([smhdwMy])$");

    /**
     * Parses a short duration token (e.g. {@code 5m}, {@code 2h}) into seconds.
     *
     * @param time Duration token to parse
     * @return Parsed seconds, or a negative sentinel value if the input is invalid
     */
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
     * Formats the supplied duration into a localized, human-readable string.
     * <p>
     * Filters act as an exclusion list: any listed units are omitted from the result.
     *
     * @param messageService Service used to resolve localized unit labels
     * @param languageId     Identifier of the target language
     * @param totalSeconds   Duration to format
     * @param filters        Optional time units to exclude from the output
     * @return Localized duration string
     */
    public static String formatDuration(MessageService messageService, int languageId, long totalSeconds, TimeFilterUtil... filters) {
        if (totalSeconds <= 0) return "0 seconds";

        Duration duration = Duration.ofSeconds(totalSeconds);
        long totalDays = duration.toDays();
        long years = totalDays / 365;
        long days = totalDays % 365;
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        Set<TimeFilterUtil> excludedFilters = filters == null || filters.length == 0
                ? EnumSet.noneOf(TimeFilterUtil.class)
                : EnumSet.copyOf(Arrays.asList(filters));

        StringBuilder builder = new StringBuilder();
        boolean appended = false;

        if (!excludedFilters.contains(TimeFilterUtil.YEARS)) {
            if (years > 0) {
                builder.append(years).append(" ").append(years == 1 ?
                        messageService.getMessage(MurmelMessage.TIME_YEAR_SINGULAR.getTag(), languageId)
                        : messageService.getMessage(MurmelMessage.TIME_YEAR_PLURAL.getTag(), languageId)).append(" ");
                appended = true;
            }
        }
        if (!excludedFilters.contains(TimeFilterUtil.DAYS)) {
            if (days > 0) {
                builder.append(days).append(" ").append(days == 1 ?
                        messageService.getMessage(MurmelMessage.TIME_DAY_SINGULAR.getTag(), languageId)
                        : messageService.getMessage(MurmelMessage.TIME_DAY_PLURAL.getTag(), languageId)).append(" ");
                appended = true;
            }
        }
        if (!excludedFilters.contains(TimeFilterUtil.HOURS)) {
            if (hours > 0) {
                builder.append(hours).append(" ").append(hours == 1 ?
                        messageService.getMessage(MurmelMessage.TIME_HOUR_SINGULAR.getTag(), languageId)
                        : messageService.getMessage(MurmelMessage.TIME_HOUR_PLURAL.getTag(), languageId)).append(" ");
                appended = true;
            }
        }
        if (!excludedFilters.contains(TimeFilterUtil.MINUTES)) {
            if (minutes > 0) {
                builder.append(minutes).append(" ").append(minutes == 1 ?
                        messageService.getMessage(MurmelMessage.TIME_MINUTE_SINGULAR.getTag(), languageId)
                        : messageService.getMessage(MurmelMessage.TIME_MINUTE_PLURAL.getTag(), languageId)).append(" ");
                appended = true;
            }
        }
        if (!excludedFilters.contains(TimeFilterUtil.SECONDS)) {
            if (seconds > 0) {
                builder.append(seconds).append(" ").append(seconds == 1 ?
                        messageService.getMessage(MurmelMessage.TIME_SECOND_SINGULAR.getTag(), languageId)
                        : messageService.getMessage(MurmelMessage.TIME_SECOND_PLURAL.getTag(), languageId));
                appended = true;
            }
        }

        if (!appended) {
            TimeFilterUtil fallbackFilter = Arrays.stream(TimeFilterUtil.values())
                    .filter(filter -> !excludedFilters.contains(filter))
                    .reduce((first, second) -> second)
                    .orElse(null);

            if (fallbackFilter == null) return "0 seconds";

            MurmelMessage pluralMessage = switch (fallbackFilter) {
                case YEARS -> MurmelMessage.TIME_YEAR_PLURAL;
                case DAYS -> MurmelMessage.TIME_DAY_PLURAL;
                case HOURS -> MurmelMessage.TIME_HOUR_PLURAL;
                case MINUTES -> MurmelMessage.TIME_MINUTE_PLURAL;
                case SECONDS -> MurmelMessage.TIME_SECOND_PLURAL;
            };
            builder.append("0 ").append(messageService.getMessage(pluralMessage.getTag(), languageId));
            return builder.toString();
        }

        return builder.toString().trim();
    }
}
