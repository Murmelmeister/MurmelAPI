package de.murmelmeister.murmelapi.language;

public class Message {
    private final int id;
    private String tag;
    private int languageId;
    private String message;

    public Message(int id, String tag, int languageId, String message) {
        this.id = id;
        this.tag = tag;
        this.languageId = languageId;
        this.message = message;
    }

    public int getId() {
        return id;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public int getLanguageId() {
        return languageId;
    }

    public void setLanguageId(int languageId) {
        this.languageId = languageId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
