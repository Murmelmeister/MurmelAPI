package de.murmelmeister.murmelapi.database;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface ResultSetProcessor<T> {
    T process(ResultSet resultSet) throws SQLException;
}
