package main.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import main.database.Database;
import main.model.Priority;
import main.model.ticket.Bug;
import main.model.ticket.FeatureRequest;
import main.model.ticket.Ticket;
import main.model.ticket.UIFeedback;
import main.model.user.User;
import main.utils.InputData;

import java.time.LocalDate;
import java.util.List;

/**
 * Command to report a new ticket in the system.
 */
public class ReportTicketCommand implements Command {
    private final InputData data;

    /**
     * Constructor for ReportTicketCommand.
     *
     * @param data The input data containing command parameters.
     */
    public ReportTicketCommand(final InputData data) {
        this.data = data;
    }

    /**
     * Executes the report ticket command.
     *
     * @param outputs The list of outputs to append results to.
     */
    @Override
    public void execute(final List<ObjectNode> outputs) {
        Database db = Database.getInstance();
        ObjectMapper mapper = new ObjectMapper();

        LocalDate commandDate = LocalDate.parse(data.getTimestamp());

        db.setProjectStartDate(commandDate);

        // 1. Validation: Testing Phase (12 days)
        if (!db.isTestingPhaseActive(commandDate)) {
            ObjectNode error = mapper.createObjectNode();
            error.put("command", "reportTicket");
            error.put("username", data.getUsername());
            error.put("timestamp", data.getTimestamp());
            error.put("error", "Tickets can only be reported during testing phases.");

            outputs.add(error);
            return; // Stop execution, do not create ticket
        }

        // 2. Validation: User exists
        User user = db.findUserByUsername(data.getUsername());
        if (user == null) {
            ObjectNode error = mapper.createObjectNode();
            error.put("command", "reportTicket");
            error.put("username", data.getUsername());
            error.put("timestamp", data.getTimestamp());
            error.put("error", "The user " + data.getUsername() + " does not exist.");
            outputs.add(error);
            return;
        }

        // 3. Validation: Reporter Role
        if (!String.valueOf(user.getRole()).equalsIgnoreCase("REPORTER")) {
            ObjectNode error = mapper.createObjectNode();
            error.put("command", "reportTicket");
            error.put("status", "error");
            error.put("description", "The user does not have permission to execute this command: "
                    + "required role REPORTER; user role " + user.getRole() + ".");
            outputs.add(error);
            return;
        }

        // Logic for Anonymous Tickets
        // If reportedBy is empty, it is anonymous
        String reportedBy = data.getReportedBy();
        if (reportedBy == null || reportedBy.isEmpty()) {
            // RULE: Only BUGs can be anonymous
            String type = data.getType();

            if (type != null && !"BUG".equalsIgnoreCase(type)) {
                ObjectNode error = mapper.createObjectNode();
                error.put("command", "reportTicket");
                error.put("username", data.getUsername());
                error.put("timestamp", data.getTimestamp());
                error.put("error", "Anonymous reports are only allowed for tickets of type BUG.");

                outputs.add(error);
                return; // Stop execution
            }

            // If anonymous BUG, priority automatically becomes LOW
            data.setPriority(Priority.LOW);
        }

        String ticketType = data.getType();

        if (ticketType == null) {
            return;
        }

        // Generate incremental ID
        int newId = db.getTickets().size();

        Ticket newTicket = switch (ticketType.toUpperCase()) {
            case "BUG" -> {
                Bug bug = new Bug(newId, data.getType(), data.getTitle(), data.getDescription(),
                        data.getPriority(), "OPEN", data.getReportedBy(), data.getTimestamp());

                bug.setSeverity(data.getSeverity());
                bug.setFrequency(data.getFrequency());

                // Return 'bug' object
                yield bug;
            }
            case "FEATURE_REQUEST" -> {
                FeatureRequest fr = new FeatureRequest(newId, data.getType(), data.getTitle(),
                        data.getDescription(), data.getPriority(), "OPEN",
                        data.getReportedBy(), data.getTimestamp());

                fr.setBusinessValue(data.getBusinessValue());
                fr.setCustomerDemand(data.getCustomerDemand());

                // Return 'fr' object
                yield fr;
            }
            case "UI_FEEDBACK" -> {
                UIFeedback ui = new UIFeedback(newId, data.getType(), data.getTitle(),
                        data.getDescription(), data.getPriority(), "OPEN",
                        data.getReportedBy(), data.getTimestamp());

                ui.setBusinessValue(data.getBusinessValue());
                ui.setUsabilityScore(data.getUsabilityScore());

                // Return 'ui' object
                yield ui;
            }
            default -> {
                // Switch expression MUST return something
                yield null;
            }
        };

        if (newTicket != null) {
            db.addTicket(newTicket);
            if (data.getExpertiseArea() != null) {
                newTicket.setExpertiseArea(data.getExpertiseArea().toString());
            } else {
                newTicket.setExpertiseArea("");
            }
            // NO output for success ("Nu există output")
        }
    }
}