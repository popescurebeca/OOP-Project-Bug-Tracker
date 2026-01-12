package main.model.ticket;

import main.model.Priority;
import main.visitor.Visitor;

/**
 * Represents a UI Feedback ticket in the system.
 */
public class UIFeedback extends Ticket {
    private int usabilityScore; // 1-10
    private String uiElementId;
    private String businessValue;

    /**
     * Constructor for UIFeedback.
     *
     * @param id          The unique ID of the ticket.
     * @param type        The type of the ticket (UI_FEEDBACK).
     * @param title       The title of the ticket.
     * @param description The description of the ticket.
     * @param priority    The priority of the ticket.
     * @param status      The status of the ticket.
     * @param reportedBy  The username of the reporter.
     * @param createdAt   The creation timestamp.
     */
    public UIFeedback(final int id, final String type, final String title, final String description,
                      final Priority priority, final String status, final String reportedBy,
                      final String createdAt) {
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

    public int getUsabilityScore() {
        return usabilityScore;
    }

    public void setUsabilityScore(final int usabilityScore) {
        this.usabilityScore = usabilityScore;
    }

    public String getUiElementId() {
        return uiElementId;
    }

    public void setUiElementId(final String uiElementId) {
        this.uiElementId = uiElementId;
    }

    public String getBusinessValue() {
        return businessValue;
    }

    public void setBusinessValue(final String businessValue) {
        this.businessValue = businessValue;
    }
}