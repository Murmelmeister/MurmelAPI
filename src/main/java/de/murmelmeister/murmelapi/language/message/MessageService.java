package de.murmelmeister.murmelapi.language.message;

import de.murmelmeister.murmelapi.language.LanguageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

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

    public void checkAndLoad(MessageDefinition[] values) {
        Arrays.stream(values).forEach(value -> {
            String tag = value.getTag();
            value.getMessages().forEach((languageId, messageText) -> {
                if (messageProvider.get(tag, languageId) == null) {
                    messageProvider.create(tag, languageId, messageText);
                }
            });
        });
    }

    /**
     * Retrieves the persisted message text for the given definition and language.
     * <p>
     * Attempts to fetch the message via {@link MessageProviderImpl#get(String, int)}.
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
            message = messageProvider.get(messages.getTag(), languageProvider.get("english").id());
            return message.message();
        }
        return message.message();
    }
}
