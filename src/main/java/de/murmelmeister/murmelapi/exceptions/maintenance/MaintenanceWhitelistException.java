package de.murmelmeister.murmelapi.exceptions.maintenance;

public class MaintenanceWhitelistException extends MaintenanceException {
    public MaintenanceWhitelistException(String message) {
        super(message);
    }

    public MaintenanceWhitelistException(String message, Throwable cause) {
        super(message, cause);
    }

    public MaintenanceWhitelistException(Throwable cause) {
        super(cause);
    }
}
