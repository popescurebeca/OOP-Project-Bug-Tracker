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


    Priority(final int value) {
        this.value = value;
    }


    public int getValue() {
        return value;
    }


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
