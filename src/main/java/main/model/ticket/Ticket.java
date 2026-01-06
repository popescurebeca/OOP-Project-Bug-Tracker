package main.model.ticket;

import com.fasterxml.jackson.annotation.JsonProperty;
import main.visitor.Visitable;
import main.visitor.Visitor;
import java.util.ArrayList;
import java.util.List;

public abstract class Ticket implements Visitable {
    private int id;
    private String type;
    private String title;
    private String description; // Adăugat description

    @JsonProperty("businessPriority")
    private String priority;

    private String status;
    private String createdAt;

    // Câmpuri inițializate gol
    private String assignedAt = "";
    private String solvedAt = "";
    private String assignedTo = "";
    private String reportedBy = "";

    private List<String> comments = new ArrayList<>();

    // CONSTRUCTORUL CORECT (8 parametri)
    public Ticket(int id, String type, String title, String description, String priority, String status, String reportedBy, String createdAt) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.reportedBy = reportedBy;
        this.createdAt = createdAt;
    }



    // Getters
    public int getId() { return id; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getPriority() { return priority; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
    public String getReportedBy() { return reportedBy; }
    public String getAssignedTo() { return assignedTo; }
    public String getAssignedAt() { return assignedAt; }
    public String getSolvedAt() { return solvedAt; }
    public List<String> getComments() { return comments; }

    // Setters
    public void setPriority(String priority) { this.priority = priority; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public abstract void accept(Visitor v);
}