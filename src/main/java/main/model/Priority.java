package main.model;

/**
 * Enum representing the priority of a ticket.
 */
public enum Priority {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int value;

    /**
     * Constructor for Priority.
     *
     * @param value The integer value of the priority.
     */
    Priority(final int value) {
        this.value = value;
    }

    /**
     * Gets the integer value of the priority.
     *
     * @return The priority value.
     */
    public int getValue() {
        return value;
    }

    /**
     * Returns the next higher priority.
     * LOW -> MEDIUM -> HIGH -> CRITICAL
     *
     * @return The next priority level.
     */
    public Priority next() {
        switch (this) {
            case LOW:
                return MEDIUM;
            case MEDIUM:
                return HIGH;
            case HIGH:
                return CRITICAL;
            default:
                return CRITICAL;
        }
    }
}
