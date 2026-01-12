package main.model.ticket;

import main.model.Priority;
import main.visitor.Visitor;

/**
 * Represents a Bug ticket in the system.
 */
public class Bug extends Ticket {
    private String severity;
    private String frequency;

    /**
     * Constructor for Bug.
     *
     * @param id          The unique ID of the ticket.
     * @param type        The type of the ticket (BUG).
     * @param title       The title of the ticket.
     * @param description The description of the ticket.
     * @param priority    The priority of the ticket.
     * @param status      The status of the ticket.
     * @param reportedBy  The username of the reporter.
     * @param createdAt   The creation timestamp.
     */
    public Bug(final int id, final String type, final String title, final String description,
               final Priority priority, final String status, final String reportedBy,
               final String createdAt) {
        super(id, type, title, description, priority, status, reportedBy, createdAt);
    }

    /**
     * Sets the severity of the bug.
     *
     * @param severity The severity level.
     */
    public void setSeverity(final String severity) {
        this.severity = severity;
    }

    /**
     * Gets the severity of the bug.
     *
     * @return The severity level.
     */
    public String getSeverity() {
        return severity;
    }

    /**
     * Sets the frequency of the bug.
     *
     * @param frequency The frequency level.
     */
    public void setFrequency(final String frequency) {
        this.frequency = frequency;
    }

    /**
     * Gets the frequency of the bug.
     *
     * @return The frequency level.
     */
    public String getFrequency() {
        return frequency;
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
}