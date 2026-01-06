package main.model.ticket;

import main.visitor.Visitor;

public class FeatureRequest extends Ticket {
    private String customerDemand; // LOW, MEDIUM, HIGH, VERY_HIGH
    private String businessValue;  // S, M, L, XL

    public FeatureRequest(int id, String type, String title, String description, String priority, String status, String reportedBy, String createdAt) {
        super(id, type, title, description, priority, status, reportedBy, createdAt);
    }

    @Override
    public void accept(Visitor v) {
        v.visit(this);
    }

    public String getCustomerDemand() { return customerDemand; }
    public void setCustomerDemand(String customerDemand) { this.customerDemand = customerDemand; }

    public String getBusinessValue() { return businessValue; }
    public void setBusinessValue(String businessValue) { this.businessValue = businessValue; }
}