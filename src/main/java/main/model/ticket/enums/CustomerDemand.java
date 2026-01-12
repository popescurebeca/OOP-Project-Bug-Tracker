package main.model.ticket.enums;

/**
 * Enum representing the customer demand for a feature.
 */
public enum CustomerDemand {
    LOW(1),
    MEDIUM(3),
    HIGH(6),
    VERY_HIGH(10);

    private final int value;

    /**
     * Constructor for CustomerDemand.
     *
     * @param value The integer value of the demand.
     */
    CustomerDemand(final int value) {
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
