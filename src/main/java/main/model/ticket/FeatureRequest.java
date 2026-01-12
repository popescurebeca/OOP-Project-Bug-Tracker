package main.model.ticket;

import main.model.Priority;
import main.visitor.Visitor;

/**
 * Represents a Feature Request ticket in the system.
 */
public final class FeatureRequest extends Ticket {
    private String customerDemand; // LOW, MEDIUM, HIGH, VERY_HIGH
    private String businessValue;  // S, M, L, XL

    /**
     * Constructor for FeatureRequest.
     *
     * @param id          The unique ID of the ticket.
     * @param type        The type of the ticket (FEATURE_REQUEST).
     * @param title       The title of the ticket.
     * @param description The description of the ticket.
     * @param priority    The priority of the ticket.
     * @param status      The status of the ticket.
     * @param reportedBy  The username of the reporter.
     * @param createdAt   The creation timestamp.
     */
    public FeatureRequest(final int id, final String type, final String title,
                          final String description, final Priority priority, final String status,
                          final String reportedBy, final String createdAt) {
        super(id, type, title, description, priority, status, reportedBy, createdAt);
    }

    /**
     * Accepts a visitor for processing this ticket.
     *
     * @param v The visitor instance.
     */
    @Override
    public void accept(final Visitor v) {
        v.visit(this);
    }

    /**
     * Gets the customer demand.
     *
     * @return The customer demand level.
     */
    public String getCustomerDemand() {
        return customerDemand;
    }

    /**
     * Sets the customer demand.
     *
     * @param customerDemand The customer demand level.
     */
    public void setCustomerDemand(final String customerDemand) {
        this.customerDemand = customerDemand;
    }

    /**
     * Gets the business value.
     *
     * @return The business value.
     */
    public String getBusinessValue() {
        return businessValue;
    }

    /**
     * Sets the business value.
     *
     * @param businessValue The business value.
     */
    public void setBusinessValue(final String businessValue) {
        this.businessValue = businessValue;
    }
}
