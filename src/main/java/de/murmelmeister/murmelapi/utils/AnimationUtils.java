package de.murmelmeister.murmelapi.utils;

import java.util.List;

/**
 * Utility class for animating strings with various effects such as bounce and color cycling.
 * This class provides static methods to apply these animations to input strings based on
 * numerical values, allowing for dynamic visual effects in a string.
 */
public final class AnimationUtils {
    /**
     * Animates a bounce effect on the given input string based on the raw value provided.
     * The bounce effect dynamically trims the string's length, creating a visual contraction
     * and expansion effect.
     *
     * @param input The input string to apply the bounce animation to. Cannot be null.
     * @param raw   A numerical value used to determine the current state of the bounce animation.
     *              This value can be positive or negative.
     * @return A substring of the input string, with its length dynamically adjusted
     * based on the calculated bounce cycle. If the input string is empty, it is returned as-is.
     */
    public static String animateBounce(String input, long raw) {
        if (input == null) return null;
        int length = input.length();
        long cycle = length * 2L;
        if (cycle == 0) return input;

        long index = raw % cycle;
        if (index < 0) index += cycle;

        int subLength = (index <= length)
                ? (int) (length - index)
                : (int) (index - length);
        subLength = Math.max(0, Math.min(subLength, length));
        return input.substring(0, subLength);
    }

    /**
     * Animates the input string by prepending a color from the provided list based on the given raw position.
     * The selected color cycles through the list of colors based on the raw position.
     *
     * @param colors The list of color strings to use for animation; must not be null or empty
     * @param input  The original string to be animated; if null, the input is returned as is
     * @param raw    The raw position value used to determine the cycling index for the color
     * @return The input string prepended with the selected color from the list; if the input is null or the list is empty, the original input is returned
     */
    public static String animateColor(List<String> colors, String input, long raw) {
        if (colors == null || colors.isEmpty() || input == null) return input;

        int size = colors.size();
        int idx = (int) (raw % size);
        if (idx < 0) idx += size;

        return colors.get(idx) + input;
    }

    /**
     * Animates a given input string by appending color codes from the provided color list
     * in a cyclic manner. Each character of the input string is prefixed with a color
     * code determined by the current position in the cycle.
     *
     * @param colors A list of color codes to cycle through
     * @param input  The string to be animated
     * @param raw    The initial position used to determine the starting point in the cycle
     * @return The animated string with color codes prefixed to each character
     */
    public static String animatePerColorCycle(List<String> colors, String input, long raw) {
        if (colors == null || colors.isEmpty() || input == null) return input;
        StringBuilder sb = new StringBuilder();
        int size = colors.size();

        for (int i = 0; i < input.length(); i++) {
            long pos = raw + i;
            int colorIndex = (int) (pos % size);
            if (colorIndex < 0) colorIndex += size;
            String color = colors.get(colorIndex);
            sb.append(color).append(input.charAt(i));
        }

        return sb.toString();
    }

    /**
     * Combines multiple animation effects on a given input string. This method applies
     * a bounce animation to dynamically adjust the length of the input string, followed
     * by a color cycle animation that prefixes each character of the resulting string
     * with a color code from the provided list.
     *
     * @param colors The list of color codes to use for the color cycle animation; must not be null or empty
     * @param input  The input strings to animate; if null, the result will also be null
     * @param raw    A numerical value used to determine the starting state for both the bounce and color cycle animations
     * @return The input string with both bounce and color cycle animations applied, or null if the input string is null
     */
    public static String animateFull(List<String> colors, String input, long raw) {
        String bounced = animateBounce(input, raw);
        return animatePerColorCycle(colors, bounced, raw);
    }
}
