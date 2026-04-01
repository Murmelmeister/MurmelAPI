package de.murmelmeister.murmelapi.language;

import org.jetbrains.annotations.NotNull;

public interface LanguageType {
    int id();

    @NotNull String code();

    static @NotNull LanguageType of(int id, @NotNull String code) {
        return new LanguageTypeImpl(id, code);
    }
}
