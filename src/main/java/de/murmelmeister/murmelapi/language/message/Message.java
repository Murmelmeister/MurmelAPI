package de.murmelmeister.murmelapi.language.message;

/**
 * Represents a message in the Murmel API.
 * Each message has an ID, a tag, a language ID, and the actual message content.
 */
public record Message(int id, String tagId, int languageId, String message) {
    public Message withUpdateMeta(String tagId, Integer languageId, String message) {
        return new Message(id,
                tagId != null ? tagId : this.tagId,
                languageId != null ? languageId : this.languageId,
                message != null ? message : this.message);
    }
}
