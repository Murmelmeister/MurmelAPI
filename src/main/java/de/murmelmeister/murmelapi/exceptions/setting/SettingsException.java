package de.murmelmeister.murmelapi.exceptions.setting;

import de.murmelmeister.murmelapi.exceptions.MurmelException;

public class SettingsException extends MurmelException {
    public SettingsException(String message) {
        super(message);
    }

    public SettingsException(String message, Throwable cause) {
        super(message, cause);
    }

    public SettingsException(Throwable cause) {
        super(cause);
    }
}
