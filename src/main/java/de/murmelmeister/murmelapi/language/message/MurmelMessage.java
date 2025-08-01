package de.murmelmeister.murmelapi.language.message;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static de.murmelmeister.murmelapi.language.message.LocalizedMessage.of;

public enum MurmelMessage implements MessageDefinition {
    TIME_SECOND_SINGULAR(
            of(1, "second"),
            of(2, "Sekunde")
    ),
    TIME_SECOND_PLURAL(
            of(1, "seconds"),
            of(2, "Sekunden")
    ),
    TIME_MINUTE_SINGULAR(
            of(1, "minute"),
            of(2, "Minute")
    ),
    TIME_MINUTE_PLURAL(
            of(1, "minutes"),
            of(2, "Minuten")
    ),
    TIME_HOUR_SINGULAR(
            of(1, "hour"),
            of(2, "Stunde")
    ),
    TIME_HOUR_PLURAL(
            of(1, "hours"),
            of(2, "Stunden")
    ),
    TIME_DAY_SINGULAR(
            of(1, "day"),
            of(2, "Tag")
    ),
    TIME_DAY_PLURAL(
            of(1, "days"),
            of(2, "Tage")
    ),
    TIME_YEAR_SINGULAR(
            of(1, "year"),
            of(2, "Jahr")
    ),
    TIME_YEAR_PLURAL(
            of(1, "years"),
            of(2, "Jahre")
    ),
    DATE_TIME_FORMAT(
            of(1, "dd.MM.yyyy HH:mm:ss"),
            of(2, "dd.MM.yyyy HH:mm:ss")
    ),
    ;
    private static final MurmelMessage[] VALUES = values();

    private final Map<Integer, String> messages;

    MurmelMessage(LocalizedMessage... entries) {
        Map<Integer, String> messageMap = new HashMap<>();
        for (LocalizedMessage entry : entries)
            messageMap.put(entry.languageId(), entry.message());
        this.messages = Collections.unmodifiableMap(messageMap);
    }

    @Override
    public String getTag() {
        return name().toUpperCase();
    }

    @Override
    public Map<Integer, String> getMessages() {
        return messages;
    }

    public static void loadMessages(MessageService service) {
        service.checkAndLoad(VALUES);
    }
}
