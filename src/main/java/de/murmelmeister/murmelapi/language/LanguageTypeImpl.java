package de.murmelmeister.murmelapi.language;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

record LanguageTypeImpl(
        int id,
        @NotNull String code
) implements LanguageType {

    public LanguageTypeImpl {
        Objects.requireNonNull(code, "code must not be null");
        if (code.length() > 32) throw new IllegalArgumentException("code cannot be longer than 32 characters");
    }
}
