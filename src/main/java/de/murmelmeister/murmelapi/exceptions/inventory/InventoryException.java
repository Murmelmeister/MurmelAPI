package de.murmelmeister.murmelapi.exceptions.inventory;

import de.murmelmeister.murmelapi.exceptions.MurmelException;

public class InventoryException extends MurmelException {
    public InventoryException(String message) {
        super(message);
    }

    public InventoryException(String message, Throwable cause) {
        super(message, cause);
    }

    public InventoryException(Throwable cause) {
        super(cause);
    }
}
