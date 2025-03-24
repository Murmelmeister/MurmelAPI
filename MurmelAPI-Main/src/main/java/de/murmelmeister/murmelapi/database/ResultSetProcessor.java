package de.murmelmeister.murmelapi.database;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface ResultSetProcessor {
    void process(ResultSet resultSet) throws SQLException;
}
