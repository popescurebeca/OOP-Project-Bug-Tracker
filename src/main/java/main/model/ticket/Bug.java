package main.model.ticket;

import main.visitor.Visitor;

public class Bug extends Ticket {
    private String severity;
    // alte câmpuri (frequency etc.)

    public Bug(int id, String type, String title, String description, String priority, String status, String reportedBy, String createdAt) {
        // AICI TREBUIE SA FIE EXACT ORDINEA DIN TICKET
        super(id, type, title, description, priority, status, reportedBy, createdAt);
    }

    public void setSeverity(String severity) { this.severity = severity; }
    public String getSeverity() { return severity; }

    @Override
    public void accept(Visitor v) {
        v.visit(this); // Dacă ai eroarea "visit(Bug) not defined", trebuie să o definești în interfața Visitor
    }
}