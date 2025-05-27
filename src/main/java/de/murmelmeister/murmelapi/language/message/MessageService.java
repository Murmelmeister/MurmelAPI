package de.murmelmeister.murmelapi.language.message;

import de.murmelmeister.murmelapi.language.LanguageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service layer for managing application messages, handling loading, synchronization,
 * and retrieval of localized message texts.
 * <p>
 * This class delegates persistence operations to a {@link MessageProvider} and
 * orchestrates the initialization (loading) and consistency check between in-code
 * message definitions and the persisted store. It also provides a fallback mechanism
 * to a default language if a requested message is missing.
 * </p>
 */
public final class MessageService {
    private final Logger logger = LoggerFactory.getLogger(MessageService.class);
    private final LanguageProvider languageProvider;
    private final MessageProvider messageProvider;

    public MessageService(LanguageProvider languageProvider, MessageProvider provider) {
        this.languageProvider = languageProvider;
        this.messageProvider = provider;
    }

    /**
     * Ensures that every message definition in the provided array is present
     * in the persistent store and in the provider’s cache.
     * <p>
     * This method performs the following steps:
     * <ol>
     *   <li>Calls {@link MessageProvider#loadData()} to clear and reload the cache
     *       with all existing messages.</li>
     *   <li>Constructs a set of existing keys in the format {@code "tag_languageId"}.</li>
     *   <li>Iterates over each {@link MessageDefinition} in {@code values}, and for each
     *       supported language entry:
     *     <ul>
     *       <li>Skips the entry if its key is already in the loaded set.</li>
     *       <li>Skips the entry if {@link MessageProvider#get(String, int)} returns non-null.</li>
     *       <li>Otherwise, creates a new record via
     *           {@link MessageProvider#create(String, int, String)}.</li>
     *     </ul>
     *   </li>
     * </ol>
     * </p>
     *
     * @param values An array of {@link MessageDefinition} instances whose tag/language
     *               combinations should be validated and inserted if missing
     * @see MessageProvider#loadData()
     * @see MessageProvider#get(String, int)
     * @see MessageProvider#create(String, int, String)
     */
    public void checkAndLoad(MessageDefinition[] values) {
        Set<String> existingKeys = messageProvider.loadData().stream()
                .map(message -> message.getTag() + "_" + message.getLanguageId())
                .collect(Collectors.toSet());

        for (MessageDefinition messages : values) {
            String tag = messages.getTag();
            for (Map.Entry<Integer, String> entry : messages.getMessagesMap().entrySet()) {
                int languageId = entry.getKey();
                String messageText = entry.getValue();
                String key = tag + "_" + languageId;
                if (existingKeys.contains(key)) continue;
                if (messageProvider.get(tag, languageId) != null) continue;
                messageProvider.create(tag, languageId, messageText);
            }
        }
    }

    /**
     * Retrieves the persisted message text for the given definition and language.
     * <p>
     * Attempts to fetch the message via {@link MessageProvider#get(String, int)}.
     * If no persisted entry is found, logs a warning and falls back to the default
     * English text from the provider.
     * </p>
     *
     * @param messages   The {@link MessageDefinition} enum instance identifying the message tag
     * @param languageId The desired language identifier
     * @return The localized message text, never {@code null}
     */
    public String getMessage(MessageDefinition messages, int languageId) {
        Message message = messageProvider.get(messages.getTag(), languageId);
        if (message == null) {
            logger.warn("Message with tag '{}' and language ID '{}' not found.", messages.getTag(), languageId);
            message = messageProvider.get(messages.getTag(), languageProvider.get("english").getId());
            return message.getMessage();
        }
        return message.getMessage();
    }
}
