package main.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import main.database.Database;
import main.model.Milestone;
import main.model.ticket.Ticket;
import main.model.user.User;
import main.utils.InputData;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command to view all tickets visible to a user.
 */
public final class ViewTicketsCommand implements Command {
    private final InputData data;

    /**
     * Constructor for ViewTicketsCommand.
     *
     * @param data The input data containing command parameters.
     */
    public ViewTicketsCommand(final InputData data) {
        this.data = data;
    }

    @Override
    public void execute(final List<ObjectNode> outputs) {
        Database db = Database.getInstance();
        ObjectMapper mapper = new ObjectMapper();

        LocalDate currentDay = LocalDate.parse(data.getTimestamp());

        // Update milestones rules first
        for (Milestone m : db.getMilestones()) {
            m.applyRules(db, currentDay);
        }

        // 1. User Check
        User user = db.findUserByUsername(data.getUsername());
        if (user == null) {
            return;
        }

        // 2. Filter tickets based on visibility rules
        List<Ticket> visibleTickets = db.getTickets().stream()
                .filter(t -> isVisible(user, t, db))
                .sorted(Comparator.comparingInt(Ticket::getId))
                .collect(Collectors.toList());

        // 3. Construct JSON Output
        ObjectNode resultNode = mapper.createObjectNode();
        resultNode.put("command", "viewTickets");
        resultNode.put("username", data.getUsername());
        resultNode.put("timestamp", data.getTimestamp());

        ArrayNode ticketsArray = mapper.createArrayNode();

        for (Ticket t : visibleTickets) {
            ObjectNode ticketNode = mapper.createObjectNode();
            ticketNode.put("id", t.getId());
            ticketNode.put("type", t.getType());
            ticketNode.put("title", t.getTitle());

            // Map "priority" to "businessPriority" for JSON output
            ticketNode.put("businessPriority", t.getPriority().name());

            ticketNode.put("status", t.getStatus());
            ticketNode.put("createdAt", t.getCreatedAt());

            // Handle optional fields (initialized to "" in Ticket)
            ticketNode.put("assignedAt", t.getAssignedAt());
            ticketNode.put("solvedAt", t.getSolvedAt());
            ticketNode.put("assignedTo", t.getAssignedTo());
            ticketNode.put("reportedBy", t.getReportedBy());

            // Handle comments
            ArrayNode commentsArray = mapper.createArrayNode();
            if (t.getComments() != null) {
                for (Ticket.Comment c : t.getComments()) {
                    ObjectNode commentNode = mapper.createObjectNode();
                    commentNode.put("author", c.getAuthor());
                    commentNode.put("content", c.getContent());
                    commentNode.put("createdAt", c.getCreatedAt());
                    commentsArray.add(commentNode);
                }
            }
            ticketNode.set("comments", commentsArray);

            ticketsArray.add(ticketNode);
        }

        resultNode.set("tickets", ticketsArray);
        outputs.add(resultNode);
    }

    /**
     * Determines if a user can see a specific ticket based on their role and ticket state.
     *
     * @param user The user attempting to view the ticket.
     * @param t    The ticket.
     * @param db   The database instance (needed to check milestones).
     * @return True if the ticket is visible to the user, false otherwise.
     */
    private boolean isVisible(final User user, final Ticket t, final Database db) {
        String role = String.valueOf(user.getRole()).toUpperCase();

        if ("MANAGER".equals(role)) {
            return true;
        }

        if ("REPORTER".equals(role)) {
            return t.getReportedBy().equals(user.getUsername());
        }

        if ("DEVELOPER".equals(role) || "EMPLOYEE".equals(role)) {
            // Condition 1: Must be OPEN
            if (!"OPEN".equals(t.getStatus())) {
                return false;
            }

            // Condition 2: Check Milestone access
            Milestone m = db.findMilestoneByTicketId(t.getId());
            if (m != null) {
                return m.getAssignedDevs().contains(user.getUsername());
            }

            // If not in a milestone, it is visible
            return true;
        }

        return false;
    }
}
