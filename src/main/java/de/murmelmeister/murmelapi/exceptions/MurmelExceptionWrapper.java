package de.murmelmeister.murmelapi.exceptions;

import de.murmelmeister.library.exceptions.DatabaseException;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public final class MurmelExceptionWrapper {
    public static <T> T dbWrap(@NotNull String action, @NotNull Supplier<T> supplier, @NotNull BiFunction<String, Throwable, ? extends MurmelException> exceptionFunction) {
        Objects.requireNonNull(action, "action cannot be null");
        Objects.requireNonNull(supplier, "supplier cannot be null");
        Objects.requireNonNull(exceptionFunction, "exceptionFunction cannot be null");

        try {
            return supplier.get();
        } catch (MurmelException e) {
            throw e;
        } catch (DatabaseException e) {
            throw exceptionFunction.apply(action, e);
        } catch (RuntimeException e) {
            throw exceptionFunction.apply(action + " (unexpected error)", e);
        }
    }
}
