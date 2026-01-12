package main.commands;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import main.database.Database;
import main.model.ticket.Ticket;
import main.utils.InputData;

import java.util.List;

/**
 * Command to undo the assignment of a ticket.
 */
public class UndoAssignTicketCommand implements Command {
    private final Database db;
    private final InputData input;

    /**
     * Constructor for UndoAssignTicketCommand.
     *
     * @param db    The database instance.
     * @param input The input data containing command parameters.
     */
    public UndoAssignTicketCommand(final Database db, final InputData input) {
        this.db = db;
        this.input = input;
    }

    /**
     * Executes the undo assign ticket command.
     *
     * @param outputs The list of outputs to append results to.
     */
    @Override
    public void execute(final List<ObjectNode> outputs) {
        String username = input.getUsername();
        int ticketId = input.getTicketId();
        String timestamp = input.getTimestamp();

        Ticket ticket = db.getTicket(ticketId);

        // 1. Status Validation (Restriction 1)
        if (ticket == null) {
            // Safety check: if ticket does not exist, ignore command
            return;
        }
        if (!"IN_PROGRESS".equals(ticket.getStatus())) {
            ObjectNode error = JsonNodeFactory.instance.objectNode();
            // Note: input expects "undoAssign", not "undoAssignTicket" in JSON error
            error.put("command", "undoAssign");
            error.put("username", username);
            error.put("timestamp", timestamp);
            error.put("error", "Only IN_PROGRESS tickets can be unassigned.");
            outputs.add(error);
            return;
        }

        // --- Effective Undo ---
        ticket.setAssignee("");
        ticket.setStatus("OPEN");
        ticket.setAssignedAt(""); // Reset assignment date

        // 1. Log DE-ASSIGNED
        ticket.addHistory(new Ticket.HistoryEntry("DE-ASSIGNED", username, timestamp));
    }
}