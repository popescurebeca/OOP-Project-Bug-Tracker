package main.model.ticket.enums;

/**
 * Enum representing the frequency of a bug occurrence.
 */
public enum Frequency {
    RARE(1),
    OCCASIONAL(2),
    FREQUENT(3),
    ALWAYS(4);

    private final int value;

    /**
     * Constructor for Frequency.
     *
     * @param value The integer value of the frequency.
     */
    Frequency(final int value) {
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
