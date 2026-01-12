package main.model.ticket.enums;

/**
 * Enum representing the business value of a ticket.
 */
public enum BusinessValue {
    S(1),
    M(3),
    L(6),
    XL(10);

    private final int value;

    /**
     * Constructor for BusinessValue.
     *
     * @param value The integer value of the business value.
     */
    BusinessValue(final int value) {
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
