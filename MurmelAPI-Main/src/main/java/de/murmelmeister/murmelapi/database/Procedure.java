package de.murmelmeister.murmelapi.database;

public enum Procedure {
    GENERIC_INSERT("Generic_Insert", "IN tableName VARCHAR(255), IN columns TEXT, IN value TEXT", """
            SET @numValues = LENGTH(columns) - LENGTH(REPLACE(columns, ',', '')) + 1;
            \s
            SET @placeholder = REPEAT('?,', @numValues - 1);
            SET @placeholder = CONCAT(@placeholder, '?');
            \s
            SET @query = CONCAT('INSERT INTO ', tableName, ' (', columns, ') VALUES (', @placeholder, ')');
            PREPARE stmt FROM @query;
            SET @param = value;
            EXECUTE stmt USING @param;
            DEALLOCATE PREPARE stmt;
            """),
    GENERIC_UPDATE("Generic_Update", "IN tableName VARCHAR(255), IN setClause TEXT, IN conditionClause TEXT, IN value TEXT", """
            SET @setClauseDynamic = REPLACE(setClause, ',', ' = ?,') + ' = ?';
            SET @conditionClauseDynamic = REPLACE(conditionClause, ',', ' = ?,') + ' = ?';
            \s
            SET @query = CONCAT('UPDATE ', tableName, ' SET ', @setClauseDynamic, ' WHERE ', @conditionClauseDynamic);
            \s
            PREPARE stmt FROM @query;
            SET @params = values;
            EXECUTE stmt USING @params;
            DEALLOCATE PREPARE stmt;
            """),
    GENERIC_DELETE("Generic_Delete", "IN tableName VARCHAR(255), IN conditionClause TEXT, IN value TEXT", """
            SET @conditionClauseDynamic = REPLACE(conditionClause, ',', ' = ?,') + ' = ?';
            \s
            SET @query = CONCAT('DELETE FROM ', tableName, ' WHERE ', @conditionClauseDynamic);
            \s
            PREPARE stmt FROM @query;
            SET @params = value;
            EXECUTE stmt USING @params;
            DEALLOCATE PREPARE stmt;
            """),
    GENERIC_SELECT("Generic_Select", "IN tableName VARCHAR(255), IN columns TEXT, IN conditionClause TEXT, IN value TEXT", """
            DECLARE baseQuery TEXT;
            \s
            SET baseQuery = CONCAT('SELECT ', columns, ' FROM ', tableName);
            \s
            IF conditionClause IS NOT NULL AND conditionClause != '' THEN
                SET @conditionClauseDynamic = REPLACE(conditionClause, ',', ' = ?,') + ' = ?';
                SET @query = CONCAT(baseQuery, ' WHERE ', @conditionClauseDynamic);
                PREPARE stmt FROM @query;
                SET @params = value;
                EXECUTE stmt USING @params;
            ELSE
                SET @query = baseQuery;
                PREPARE stmt FROM @query;
                EXECUTE stmt;
            END IF;
            \s
            DEALLOCATE PREPARE stmt;
            """),
    GENERIC_ALTER("Generic_Alter", "IN tableName VARCHAR(255), IN actionType VARCHAR(10), IN columnDefinition", """
            SET @query = CONCAT('ALTER TABLE ', tableName, ' ', actionType, ' ', columnDefinition);
            PREPARE stmt FROM @query;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
            """),
    GENERIC_CREATE_TABLE("Generic_CreateTable", "IN tableName VARCHAR(255), IN columnsAndTypes TEXT", """
            SET @query = CONCAT('CREATE TABLE ', tableName, ' (', columnsAndTypes, ')');
            PREPARE stmt FROM @query;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
            """),
    ;
    private static final Procedure[] VALUES = values();

    private final String name;
    private final String query;

    Procedure(final String name, final String input, final String query) {
        this.name = name;
        this.query = Database.getProcedureQueryWithoutObjects(name, input, query);
    }

    public String getName() {
        return name;
    }

    public String getQuery() {
        return query;
    }

    public static void loadAll() {
        for (Procedure procedure : VALUES) Database.update(procedure.getQuery());
    }
}
