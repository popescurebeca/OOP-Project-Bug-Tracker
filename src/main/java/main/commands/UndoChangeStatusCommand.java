package main.commands;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import main.database.Database;
import main.model.ticket.Ticket;
import main.utils.InputData;

import java.util.List;

/**
 * Command to undo the last status change of a ticket.
 */
public class UndoChangeStatusCommand implements Command {
    private final Database db;
    private final InputData input;

    /**
     * Constructor for UndoChangeStatusCommand.
     *
     * @param db    The database instance.
     * @param input The input data containing command parameters.
     */
    public UndoChangeStatusCommand(final Database db, final InputData input) {
        this.db = db;
        this.input = input;
    }

    /**
     * Executes the undo change status command.
     *
     * @param outputs The list of outputs to append results to.
     */
    @Override
    public void execute(final List<ObjectNode> outputs) {
        String username = input.getUsername();
        int ticketId = input.getTicketId();
        String timestamp = input.getTimestamp();

        Ticket ticket = db.getTicket(ticketId);

        if (ticket == null) {
            return;
        }

        // Restriction 1: Check Assignee
        String assignee = ticket.getAssignee();
        if (assignee == null || !assignee.equals(username)) {
            ObjectNode error = JsonNodeFactory.instance.objectNode();
            // Note the command name in error
            error.put("command", "undoChangeStatus");
            error.put("username", username);
            error.put("timestamp", timestamp);
            error.put("error", "Ticket " + ticketId
                    + " is not assigned to developer " + username + ".");
            outputs.add(error);
            return;
        }

        String currentStatus = ticket.getStatus();

        // Restriction 2: If status is IN_PROGRESS, command is ignored.
        if ("IN_PROGRESS".equals(currentStatus)) {
            return; // Ignore
        }

        String oldStatus = ticket.getStatus();

        // Reverse transition logic
        switch (currentStatus) {
            case "CLOSED":
                ticket.setStatus("RESOLVED");
                break;
            case "RESOLVED":
                ticket.setStatus("IN_PROGRESS");
                // If returning to work, clear resolved date (if field exists)
                // ticket.setSolvedAt("");
                break;
            default:
                break;
        }

        String newStatus = ticket.getStatus();
        if (!oldStatus.equals(newStatus)) {
            ticket.addHistory(new Ticket.HistoryEntry("STATUS_CHANGED", username, timestamp)
                    .setFromTo(oldStatus, newStatus));
        }
    }
}