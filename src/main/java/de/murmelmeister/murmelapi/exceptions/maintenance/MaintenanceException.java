package de.murmelmeister.murmelapi.exceptions.maintenance;

import de.murmelmeister.murmelapi.exceptions.MurmelException;

public class MaintenanceException extends MurmelException {
    public MaintenanceException(String message) {
        super(message);
    }

    public MaintenanceException(String message, Throwable cause) {
        super(message, cause);
    }

    public MaintenanceException(Throwable cause) {
        super(cause);
    }
}
