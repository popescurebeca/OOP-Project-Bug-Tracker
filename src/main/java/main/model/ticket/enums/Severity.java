package main.model.ticket.enums;

/**
 * Enum representing the severity of a bug.
 */
public enum Severity {
    MINOR(1),
    MODERATE(2),
    SEVERE(3);

    private final int value;

    /**
     * Constructor for Severity.
     *
     * @param value The integer value of the severity.
     */
    Severity(final int value) {
        this.value = value;
    }

    /**
     * Gets the integer value.
     *
     * @return The value.
     */
    public int getValue() {
        return value;
    }
}
