package de.murmelmeister.murmelapi.language.message;

import de.murmelmeister.murmelapi.language.Language;
import de.murmelmeister.murmelapi.language.LanguageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.murmelmeister.murmelapi.MurmelAPI.ENGLISH_CODE;

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

    public String getMessage(String key, int languageId) {
        Language language = languageProvider.findById(languageId);
        if (language != null) {
            Message message = messageProvider.get(key, language.id());
            if (message != null)
                return message.message();

            logger.warn("Message with tag '{}' and language ID '{}' not found.", key, language.id());
        }

        Language defaultLanguage = languageProvider.findByCode(ENGLISH_CODE);
        if (defaultLanguage != null) {
            Message fallback = messageProvider.get(key, defaultLanguage.id());
            if (fallback != null)
                return fallback.message();
        }

        return null;
    }

    public String getMessage(String key, String code) {
        Language language = languageProvider.findByCode(code);
        if (language != null) {
            Message message = messageProvider.get(key, language.id());
            if (message != null)
                return message.message();

            logger.warn("Message with tag '{}' and language ID '{}' not found.", key, language.id());
        }

        Language defaultLanguage = languageProvider.findByCode(ENGLISH_CODE);
        if (defaultLanguage != null) {
            Message fallback = messageProvider.get(key, defaultLanguage.id());
            if (fallback != null)
                return fallback.message();
        }

        return null;
    }
}
