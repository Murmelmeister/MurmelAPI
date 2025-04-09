package de.murmelmeister.murmelapi.database;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A functional interface for processing a {@link ResultSet}.
 *
 * @param <T> The type of the result.
 */
@FunctionalInterface
public interface ResultSetProcessor<T> {
    T process(ResultSet resultSet) throws SQLException;
}
