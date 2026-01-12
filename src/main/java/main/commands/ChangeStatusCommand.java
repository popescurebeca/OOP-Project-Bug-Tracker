package main.commands;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import main.database.Database;
import main.model.ticket.Ticket;
import main.utils.InputData;

import java.util.List;

/**
 * Command to change the status of a ticket (e.g., IN_PROGRESS -> RESOLVED).
 */
public class ChangeStatusCommand implements Command {
    private final Database db;
    private final InputData input;

    /**
     * Constructor for ChangeStatusCommand.
     *
     * @param db    The database instance.
     * @param input The input data containing command parameters.
     */
    public ChangeStatusCommand(final Database db, final InputData input) {
        this.db = db;
        this.input = input;
    }

    /**
     * Executes the change status command.
     *
     * @param outputs The list of outputs to append results to.
     */
    @Override
    public void execute(final List<ObjectNode> outputs) {
        String username = input.getUsername();
        int ticketId = input.getTicketId();
        String timestamp = input.getTimestamp();

        Ticket ticket = db.getTicket(ticketId);

        // Safety check
        if (ticket == null) {
            return;
        }

        // Restriction 1: The ticket must be assigned to the developer executing the command
        String assignee = ticket.getAssignee();
        if (assignee == null || !assignee.equals(username)) {
            ObjectNode error = JsonNodeFactory.instance.objectNode();
            error.put("command", "changeStatus");
            error.put("username", username);
            error.put("timestamp", timestamp);
            error.put("error", "Ticket " + ticketId
                    + " is not assigned to developer " + username + ".");
            outputs.add(error);
            return;
        }

        // Restriction 3: If status is CLOSED, the command is ignored.
        String currentStatus = ticket.getStatus();

        if ("CLOSED".equals(currentStatus)) {
            return; // Ignore
        }

        String oldStatus = ticket.getStatus();

        // Transition logic
        switch (currentStatus) {
            case "IN_PROGRESS":
                ticket.setStatus("RESOLVED");
                break;
            case "RESOLVED":
                ticket.setStatus("CLOSED");
                break;
            default:
                // If OPEN, theoretically changeStatus shouldn't happen without assign,
                // but if it happens, we assume it stays or logic is handled elsewhere.
                break;
        }

        String newStatus = ticket.getStatus();

        if ("RESOLVED".equals(newStatus) || "CLOSED".equals(newStatus)) {
            ticket.setSolvedAt(input.getTimestamp());
        }

        if (!oldStatus.equals(newStatus)) {
            ticket.addHistory(new Ticket.HistoryEntry("STATUS_CHANGED", username, timestamp)
                    .setFromTo(oldStatus, newStatus));
        }
    }
}