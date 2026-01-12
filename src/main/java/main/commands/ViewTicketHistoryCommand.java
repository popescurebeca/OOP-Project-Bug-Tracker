package main.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import main.database.Database;
import main.model.Milestone;
import main.model.ticket.Ticket;
import main.model.user.User;
import main.utils.InputData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Command to view the history of tickets based on user role and visibility.
 */
public final class ViewTicketHistoryCommand implements Command {
    private final Database db;
    private final InputData input;

    /**
     * Constructor for ViewTicketHistoryCommand.
     *
     * @param db    The database instance.
     * @param input The input data containing command parameters.
     */
    public ViewTicketHistoryCommand(final Database db, final InputData input) {
        this.db = db;
        this.input = input;
    }

    @Override
    public void execute(final List<ObjectNode> outputs) {
        ObjectMapper mapper = new ObjectMapper();
        String username = input.getUsername();
        String timestamp = input.getTimestamp();

        User user = db.findUserByUsername(username);
        if (user == null) {
            return;
        }

        String role = String.valueOf(user.getRole()).toUpperCase();
        List<Ticket> viewableTickets = new ArrayList<>();

        // 1. FILTRARE TICHETE PE BAZA ROLULUI
        if ("MANAGER".equals(role)) {
            // Managerul vede tichetele din milestone-urile create de el
            for (Milestone m : db.getMilestones()) {
                if (m.getCreatedBy().equals(username)) {
                    for (Integer tid : m.getTicketIds()) {
                        Ticket t = db.getTicket(tid);
                        if (t != null && !viewableTickets.contains(t)) {
                            viewableTickets.add(t);
                        }
                    }
                }
            }
        } else if ("DEVELOPER".equals(role) || "EMPLOYEE".equals(role)) {
            // Developerul vede tichetele la care a lucrat (a fost asignat vreodată)
            for (Ticket t : db.getTickets()) {
                boolean wasAssigned = false;
                for (Ticket.HistoryEntry h : t.getHistory()) {
                    if ("ASSIGNED".equals(h.getAction()) && h.getBy().equals(username)) {
                        wasAssigned = true;
                        break;
                    }
                }
                if (wasAssigned) {
                    viewableTickets.add(t);
                }
            }
        }

        // 2. SORTARE (CreatedAt ASC, apoi ID ASC)
        viewableTickets.sort(Comparator.comparing(Ticket::getCreatedAt)
                .thenComparingInt(Ticket::getId));

        // 3. CONSTRUIRE OUTPUT
        ObjectNode result = mapper.createObjectNode();
        result.put("command", "viewTicketHistory");
        result.put("username", username);
        result.put("timestamp", timestamp);

        ArrayNode historyArray = result.putArray("ticketHistory");

        for (Ticket t : viewableTickets) {
            ObjectNode tNode = mapper.createObjectNode();
            tNode.put("id", t.getId());
            tNode.put("title", t.getTitle());
            tNode.put("status", t.getStatus()); // Statusul curent

            // --- FILTRARE ISTORIC PENTRU DEVELOPER ---
            List<Ticket.HistoryEntry> filteredHistory = new ArrayList<>();
            List<Ticket.HistoryEntry> fullHistory = t.getHistory();

            if ("MANAGER".equals(role)) {
                filteredHistory = fullHistory;
            } else {
                // Logică pentru Developer
                boolean isCurrentlyAssigned = username.equals(t.getAssignee());

                if (isCurrentlyAssigned) {
                    filteredHistory = fullHistory;
                } else {
                    int cutoffIndex = -1;
                    for (int i = fullHistory.size() - 1; i >= 0; i--) {
                        Ticket.HistoryEntry entry = fullHistory.get(i);
                        if ("DE-ASSIGNED".equals(entry.getAction())
                                && entry.getBy().equals(username)) {
                            cutoffIndex = i;
                            break;
                        }
                    }

                    if (cutoffIndex != -1) {
                        for (int i = 0; i <= cutoffIndex; i++) {
                            filteredHistory.add(fullHistory.get(i));
                        }
                        // Include subsequent STATUS_CHANGED if immediate consequence
                        if (cutoffIndex + 1 < fullHistory.size()) {
                            Ticket.HistoryEntry next = fullHistory.get(cutoffIndex + 1);
                            if ("STATUS_CHANGED".equals(next.getAction())
                                    && next.getBy().equals(username)
                                    && next.getTimestamp().equals(
                                    fullHistory.get(cutoffIndex).getTimestamp())) {
                                filteredHistory.add(next);
                            }
                        }
                    } else {
                        filteredHistory = fullHistory;
                    }
                }
            }

            ArrayNode actionsArray = tNode.putArray("actions");
            for (Ticket.HistoryEntry entry : filteredHistory) {
                ObjectNode aNode = mapper.createObjectNode();
                aNode.put("action", entry.getAction());

                if (entry.getFrom() != null) {
                    aNode.put("from", entry.getFrom());
                }
                if (entry.getTo() != null) {
                    aNode.put("to", entry.getTo());
                }
                if (entry.getMilestone() != null) {
                    aNode.put("milestone", entry.getMilestone());
                }

                aNode.put("by", entry.getBy());
                aNode.put("timestamp", entry.getTimestamp());
                actionsArray.add(aNode);
            }

            ArrayNode commentsArray = tNode.putArray("comments");
            if (t.getComments() != null) {
                for (Ticket.Comment c : t.getComments()) {
                    ObjectNode cNode = mapper.createObjectNode();
                    cNode.put("author", c.getAuthor());
                    // Output cere "timestamp", clasa are "createdAt"
                    cNode.put("createdAt", c.getCreatedAt());
                    // Output cere "message", clasa are "content"
                    cNode.put("content", c.getContent());
                    commentsArray.add(cNode);
                }
            }

            historyArray.add(tNode);
        }

        outputs.add(result);
    }
}
