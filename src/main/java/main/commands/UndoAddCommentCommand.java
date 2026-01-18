package main.commands;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import main.database.Database;
import main.model.ticket.Ticket;
import main.utils.InputData;

import java.util.List;

/**
 * Command to undo the last comment added by a user.
 */
public class UndoAddCommentCommand implements Command {
    private final Database db;
    private final InputData input;


    public UndoAddCommentCommand(final Database db, final InputData input) {
        this.db = db;
        this.input = input;
    }

    @Override
    public void execute(final List<ObjectNode> outputs) {
        String username = input.getUsername();
        int ticketId = input.getTicketId();
        String timestamp = input.getTimestamp();

        Ticket ticket = db.getTicket(ticketId);

        // Restriction 1 (Part A): If ticket does not exist, ignore.
        if (ticket == null) {
            return;
        }

        // Restriction 2: Anonymous ticket
        if (ticket.isAnonymous()) {
            ObjectNode error = JsonNodeFactory.instance.objectNode();
            error.put("command", "undoAddComment");
            error.put("username", username);
            error.put("timestamp", timestamp);
            error.put("error", "Comments are not allowed on anonymous tickets.");
            outputs.add(error);
            return;
        }

        // Restriction 1 (Part B): If user has no comments, ignore.
        // Search for the last comment by this user
        List<Ticket.Comment> comments = ticket.getComments();
        Ticket.Comment match = null;

        // Iterate backwards to find the last added comment
        for (int i = comments.size() - 1; i >= 0; i--) {
            if (comments.get(i).getAuthor().equals(username)) {
                match = comments.get(i);
                break;
            }
        }

        if (match == null) {
            // If no comment found, ignore command
            return;
        }

        // Delete the comment
        //System.out.println("Removing comment: " + match.getContent());
        ticket.removeComment(match);
    }
}
