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
     * Retrieves all messages associated with this definition, indexed by language ID.
     * <p>
     * This method provides a map of language IDs to their corresponding message texts.
     * </p>
     *
     * @return A map where keys are language IDs and values are the corresponding message texts
     */
    Map<Integer, String> getMessages();
}
