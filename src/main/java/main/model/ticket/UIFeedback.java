package main.model.ticket;

import main.visitor.Visitor;

public class UIFeedback extends Ticket {
    private int usabilityScore; // 1-10
    private String uiElementId;

    public UIFeedback(int id, String type, String title, String description, String priority, String status,
                      String reportedBy, String createdAt) {
        super(id, type, title, description, priority, status, reportedBy, createdAt);
    }

    @Override
    public void accept(Visitor v) {
        v.visit(this);
    }

    public int getUsabilityScore() { return usabilityScore; }
    public void setUsabilityScore(int usabilityScore) { this.usabilityScore = usabilityScore; }

    public String getUiElementId() { return uiElementId; }
    public void setUiElementId(String uiElementId) { this.uiElementId = uiElementId; }
}