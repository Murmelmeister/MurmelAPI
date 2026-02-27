package de.murmelmeister.murmelapi.exceptions.permission;

import de.murmelmeister.murmelapi.exceptions.MurmelException;

public class ParentException extends MurmelException {
    public ParentException(String message) {
        super(message);
    }

  public ParentException(String message, Throwable cause) {
    super(message, cause);
  }

  public ParentException(Throwable cause) {
    super(cause);
  }
}
