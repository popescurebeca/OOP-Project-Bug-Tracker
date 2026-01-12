package main.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import main.database.Database;
import main.model.ticket.Ticket;
import main.utils.InputData;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command to view tickets assigned to a specific user.
 */
public class ViewAssignedTicketsCommand implements Command {
    private final Database db;
    private final InputData input;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Constructor for ViewAssignedTicketsCommand.
     *
     * @param db    The database instance.
     * @param input The input data containing command parameters.
     */
    public ViewAssignedTicketsCommand(final Database db, final InputData input) {
        this.db = db;
        this.input = input;
    }

    /**
     * Executes the view assigned tickets command.
     *
     * @param outputs The list of outputs to append results to.
     */
    @Override
    public void execute(final List<ObjectNode> outputs) {
        String username = input.getUsername();

        // 1. Filter: only tickets assigned to this user
        List<Ticket> assignedTickets = db.getTickets().stream()
                .filter(t -> username.equals(t.getAssignee()))
                .collect(Collectors.toList());

        // 2. Sorting
        // Order: BusinessPriority (DESC), CreatedAt (ASC), ID (ASC)
        assignedTickets.sort(new Comparator<Ticket>() {
            @Override
            public int compare(final Ticket t1, final Ticket t2) {
                // 1. Priority DESC (CRITICAL > HIGH > MEDIUM > LOW)
                int p1 = t1.getPriority().ordinal();
                int p2 = t2.getPriority().ordinal();
                if (p1 != p2) {
                    return Integer.compare(p2, p1); // DESC
                }

                // 2. CreatedAt ASC
                int dateComp = t1.getCreatedAt().compareTo(t2.getCreatedAt());
                if (dateComp != 0) {
                    return dateComp;
                }

                // 3. ID ASC
                return Integer.compare(t1.getId(), t2.getId());
            }
        });

        // 3. Construct Output JSON
        ObjectNode result = mapper.createObjectNode();
        result.put("command", "viewAssignedTickets");
        result.put("username", username);
        result.put("timestamp", input.getTimestamp());

        ArrayNode ticketsArray = result.putArray("assignedTickets");

        for (Ticket t : assignedTickets) {
            ObjectNode tNode = ticketsArray.addObject();
            tNode.put("id", t.getId());
            tNode.put("type", t.getType());
            tNode.put("title", t.getTitle());
            tNode.put("businessPriority", t.getPriority().name());
            tNode.put("status", t.getStatus());
            tNode.put("createdAt", t.getCreatedAt());
            tNode.put("assignedAt", t.getAssignedAt());
            tNode.put("reportedBy", t.getReportedBy());

            ArrayNode commentsArray = tNode.putArray("comments");

            if (t.getComments() != null) {
                for (Ticket.Comment c : t.getComments()) {
                    // 1. Create JSON object for each comment
                    ObjectNode commentNode = mapper.createObjectNode();

                    // 2. Populate fields
                    commentNode.put("author", c.getAuthor());
                    commentNode.put("content", c.getContent());
                    commentNode.put("createdAt", c.getCreatedAt());

                    // 3. Add object to array
                    commentsArray.add(commentNode);
                }
            }
        }

        outputs.add(result);
    }
}
