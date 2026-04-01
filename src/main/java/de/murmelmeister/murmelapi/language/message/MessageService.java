package de.murmelmeister.murmelapi.language.message;

import de.murmelmeister.murmelapi.language.LanguageType;
import de.murmelmeister.murmelapi.language.LanguageTypeProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

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
    private final LanguageTypeProvider languageProvider;
    private final MessageProvider messageProvider;

    public MessageService(@NotNull LanguageTypeProvider languageProvider, @NotNull MessageProvider provider) {
        this.languageProvider = languageProvider;
        this.messageProvider = provider;
    }

    public @NotNull String getMessage(@NotNull String key, int languageId) {
        Objects.requireNonNull(key, "key cannot be null");

        Optional<LanguageType> language = languageProvider.findById(languageId);
        if (language.isPresent()) {
            LanguageType languageType = language.get();
            Optional<Message> message = messageProvider.findMessage(key, languageType.id());
            if (message.isPresent())
                return message.get().message();

            logger.warn("Message with tag '{}' and language ID '{}' not found.", key, languageType.id());
        }

        Optional<LanguageType> defaultLanguage = languageProvider.findByCode(ENGLISH_CODE);
        if (defaultLanguage.isPresent()) {
            Optional<Message> fallback = messageProvider.findMessage(key, defaultLanguage.get().id());
            if (fallback.isPresent())
                return fallback.get().message();
        }

        throw new IllegalArgumentException("Message with tag '" + key + "' not found in any language.");
    }

    public @NotNull String getMessage(@NotNull String key, @NotNull String code) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(code, "code cannot be null");

        Optional<LanguageType> language = languageProvider.findByCode(code);
        if (language.isPresent()) {
            LanguageType languageType = language.get();
            Optional<Message> message = messageProvider.findMessage(key, languageType.id());
            if (message.isPresent())
                return message.get().message();

            logger.warn("Message with tag '{}' and language ID '{}' not found.", key, languageType.id());
        }

        Optional<LanguageType> defaultLanguage = languageProvider.findByCode(ENGLISH_CODE);
        if (defaultLanguage.isPresent()) {
            Optional<Message> fallback = messageProvider.findMessage(key, defaultLanguage.get().id());
            if (fallback.isPresent())
                return fallback.get().message();
        }

        throw new IllegalArgumentException("Message with tag '" + key + "' not found in any language.");
    }
}
