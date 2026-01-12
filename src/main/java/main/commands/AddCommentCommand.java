package main.commands;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import main.database.Database;
import main.model.ticket.Ticket;
import main.model.user.User;
import main.utils.InputData;

import java.util.List;

/**
 * Command to add a comment to a ticket.
 */
public class AddCommentCommand implements Command {
    private static final int MIN_COMMENT_LENGTH = 10;
    private final Database db;
    private final InputData input;

    /**
     * Constructor for AddCommentCommand.
     *
     * @param db    The database instance.
     * @param input The input data containing command parameters.
     */
    public AddCommentCommand(final Database db, final InputData input) {
        this.db = db;
        this.input = input;
    }

    /**
     * Executes the add comment command.
     *
     * @param outputs The list of outputs to append results to.
     */
    @Override
    public void execute(final List<ObjectNode> outputs) {
        String username = input.getUsername();
        int ticketId = input.getTicketId();
        String timestamp = input.getTimestamp();
        String content = input.getComment();

        Ticket ticket = db.getTicket(ticketId);
        User user = db.findUserByUsername(username);

        // Restriction 1: Ticket exists
        if (ticket == null) {
            return;
        }

        // If user does not exist
        if (user == null) {
            return;
        }

        // Restriction 2: Anonymous ticket
        if (ticket.isAnonymous()) {
            addError(outputs, "Comments are not allowed on anonymous tickets.");
            return;
        }

        if (content == null || content.length() < MIN_COMMENT_LENGTH) {
            addError(outputs, "Comment must be at least 10 characters long.");
            return;
        }

        String role = String.valueOf(user.getRole()).toUpperCase();

        // Specific restrictions based on role
        if ("REPORTER".equals(role)) {

            if ("CLOSED".equals(ticket.getStatus())) {
                addError(outputs, "Reporters cannot comment on CLOSED tickets.");
                return;
            }

            if (!username.equals(ticket.getReportedBy())) {
                ObjectNode error = JsonNodeFactory.instance.objectNode();
                error.put("command", "addComment");
                error.put("username", username);
                error.put("timestamp", timestamp);
                error.put("error", "Reporter " + username
                        + " cannot comment on ticket " + ticketId + ".");
                outputs.add(error);
                return;
            }

        } else if ("DEVELOPER".equals(role) || "EMPLOYEE".equals(role)) {
            // Developer or Employee restrictions
            if (!username.equals(ticket.getAssignee())) {
                ObjectNode error = JsonNodeFactory.instance.objectNode();
                error.put("command", "addComment");
                error.put("username", username);
                error.put("timestamp", timestamp);
                error.put("error", "Ticket " + ticketId
                        + " is not assigned to the developer " + username + ".");
                outputs.add(error);
                return;
            }
        }

        // Success: add comment
        ticket.addComment(username, content, timestamp);
    }

    /**
     * Adds an error message to the outputs.
     *
     * @param outputs The list of outputs.
     * @param msg     The error message.
     */
    private void addError(final List<ObjectNode> outputs, final String msg) {
        ObjectNode error = JsonNodeFactory.instance.objectNode();
        error.put("command", "addComment");
        error.put("username", input.getUsername());
        error.put("timestamp", input.getTimestamp());
        error.put("error", msg);
        outputs.add(error);
    }
}
