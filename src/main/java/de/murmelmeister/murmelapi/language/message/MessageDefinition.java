package de.murmelmeister.murmelapi.language.message;

import java.util.Map;

/**
 * Defines the contract for a set of localized messages identified by a tag.
 * <p>
 * Implementations of this interface typically represent an enumeration of
 * message keys, each of which may supply text in multiple language variants.
 * </p>
 */
public interface MessageDefinition {
    /**
     * Returns the unique key (or “tag”) for this message definition.
     * <p>
     * This tag is typically used to look up persisted messages in a store
     * or to reference the message in code.
     * </p>
     *
     * @return A non-null, uppercase identifier for this message (e.g. "PREFIX_COMMAND")
     */
    String getTag();

    /**
     * Retrieves the message text for the specified language.
     * <p>
     * If the implementation does not support the requested language ID, it
     * may choose to fall back to a default language (for example, English).
     * </p>
     *
     * @param languageId The numerical identifier of the desired language (e.g., 1 = English, 2 = German)
     * @return The localized message text
     */
    String getMessage(int languageId);

    /**
     * Returns an unmodifiable map of all supported language IDs to their
     * corresponding message texts.
     * <p>
     * The keys in the map are language identifiers (integers), and the values
     * are the localized message strings.
     * </p>
     *
     * @return A map such as {@code {1="English", 2="Englisch"}}
     */
    Map<Integer, String> getMessagesMap();

    /**
     * Provides an array of all available message definitions of this type.
     * <p>
     * This mimics the {@code values()} method found in enums, allowing
     * iteration over all defined message keys.
     * </p>
     *
     * @return A non-null array of all {@code MessageDefinition} instances
     * @throws UnsupportedOperationException if not overridden by the implementing class
     */
    static MessageDefinition[] getValues() {
        Object[] constants = MessageDefinition.class.getEnumConstants();
        if (constants == null)
            throw new UnsupportedOperationException("getValues() can only be called on enums.");
        return new MessageDefinition[constants.length];
    }

    /**
     * A simple pair type holding a (languageId, messageText) tuple.
     * <p>
     * This record is often used in enum constructors or builder methods
     * to supply multiple language variants in a concise form.
     * </p>
     */
    record LanguageMessage(int languageId, String message) {
        public static LanguageMessage of(int languageId, String message) {
            return new LanguageMessage(languageId, message);
        }
    }
}
