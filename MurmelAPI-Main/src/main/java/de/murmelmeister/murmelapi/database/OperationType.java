package de.murmelmeister.murmelapi.database;

/**
 * Represents the types of database operations that can be performed.
 * <p>
 * This enumeration serves as a classification mechanism for distinguishing
 * between different database actions such as insertion, modification,
 * deletion, retrieval, and schema alterations.
 * <p>
 * The available operation types are:
 * - INSERT: Represents the operation of adding new records.
 * - UPDATE: Represents the operation of modifying existing records.
 * - DELETE: Represents the operation of removing existing records.
 * - SELECT: Represents the operation of retrieving records.
 * - ALTER: Represents the operation of modifying the structure of a database object.
 * - CREATE: Represents the operation of creating new database objects.
 * - DROP: Represents the operation of deleting database objects.
 */
public enum OperationType {
    INSERT,
    UPDATE,
    DELETE,
    SELECT,
    ALTER,
    CREATE,
    DROP;
    private static final OperationType[] VALUES = values();

    public static OperationType fromString(String value) {
        for (OperationType type : VALUES)
            if (type.name().equals(value)) return type;
        return null;
    }
}
