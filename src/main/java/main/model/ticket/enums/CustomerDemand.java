package main.model.ticket.enums;

public enum CustomerDemand {
    LOW(1),
    MEDIUM(3),
    HIGH(6),
    VERY_HIGH(10);

    private final int value;

    CustomerDemand(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}