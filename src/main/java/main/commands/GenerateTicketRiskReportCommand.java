package main.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import main.database.Database;
import main.model.ticket.Ticket;
import main.utils.InputData;
import main.visitor.TicketRiskVisitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Command to generate the Ticket Risk Report.
 */
public class GenerateTicketRiskReportCommand implements Command {
    private final Database db;
    private final InputData input;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Constructor for GenerateTicketRiskReportCommand.
     *
     * @param db    The database instance.
     * @param input The input data containing command parameters.
     */
    public GenerateTicketRiskReportCommand(final Database db, final InputData input) {
        this.db = db;
        this.input = input;
    }

    /**
     * Executes the ticket risk report generation.
     *
     * @param outputs The list of outputs to append results to.
     */
    @Override
    public void execute(final List<ObjectNode> outputs) {
        String username = input.getUsername();
        String timestamp = input.getTimestamp();

        // 1. Filter tickets: ONLY OPEN and IN_PROGRESS
        List<Ticket> activeTickets = db.getTickets().stream()
                .filter(t -> "OPEN".equals(t.getStatus())
                        || "IN_PROGRESS".equals(t.getStatus()))
                .collect(Collectors.toList());

        // 2. Instantiate Risk Visitor
        TicketRiskVisitor visitor = new TicketRiskVisitor();

        // 3. Visit filtered tickets
        for (Ticket t : activeTickets) {
            t.accept(visitor);
        }

        // 4. Calculate additional statistics (Priority Distribution) - independent of visitor
        Map<String, Integer> ticketsByPriority = new HashMap<>();
        ticketsByPriority.put("LOW", 0);
        ticketsByPriority.put("MEDIUM", 0);
        ticketsByPriority.put("HIGH", 0);
        ticketsByPriority.put("CRITICAL", 0);

        for (Ticket t : activeTickets) {
            String p = t.getPriority().name();
            ticketsByPriority.put(p, ticketsByPriority.getOrDefault(p, 0) + 1);
        }

        // 5. Build JSON
        ObjectNode root = mapper.createObjectNode();
        root.put("command", "generateTicketRiskReport");
        root.put("username", username);
        root.put("timestamp", timestamp);

        ObjectNode report = root.putObject("report");
        report.put("totalTickets", activeTickets.size());

        // Tickets By Type
        ObjectNode typeNode = report.putObject("ticketsByType");
        typeNode.put("BUG", visitor.getCount("BUG"));
        typeNode.put("FEATURE_REQUEST", visitor.getCount("FEATURE_REQUEST"));
        typeNode.put("UI_FEEDBACK", visitor.getCount("UI_FEEDBACK"));

        // Tickets By Priority
        ObjectNode prioNode = report.putObject("ticketsByPriority");
        prioNode.put("LOW", ticketsByPriority.get("LOW"));
        prioNode.put("MEDIUM", ticketsByPriority.get("MEDIUM"));
        prioNode.put("HIGH", ticketsByPriority.get("HIGH"));
        prioNode.put("CRITICAL", ticketsByPriority.get("CRITICAL"));

        // Risk By Type (Qualificatives)
        ObjectNode riskNode = report.putObject("riskByType");
        riskNode.put("BUG", visitor.getRiskQualifier("BUG"));
        riskNode.put("FEATURE_REQUEST", visitor.getRiskQualifier("FEATURE_REQUEST"));
        riskNode.put("UI_FEEDBACK", visitor.getRiskQualifier("UI_FEEDBACK"));

        outputs.add(root);
    }
}