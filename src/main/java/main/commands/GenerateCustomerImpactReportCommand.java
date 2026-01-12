package main.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import main.database.Database;
import main.model.ticket.Ticket;
import main.utils.InputData;
import main.visitor.CustomerImpactVisitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Command to generate the Customer Impact Report.
 * Calculates impact scores for tickets based on various metrics.
 */
public class GenerateCustomerImpactReportCommand implements Command {
    private final Database db;
    private final InputData input;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Constructor for GenerateCustomerImpactReportCommand.
     *
     * @param db    The database instance.
     * @param input The input data containing command parameters.
     */
    public GenerateCustomerImpactReportCommand(final Database db, final InputData input) {
        this.db = db;
        this.input = input;
    }

    /**
     * Executes the customer impact report generation.
     *
     * @param outputs The list of outputs to append results to.
     */
    @Override
    public void execute(final List<ObjectNode> outputs) {
        String username = input.getUsername();
        String timestamp = input.getTimestamp();

        // 1. Filter tickets (OPEN or IN_PROGRESS)
        List<Ticket> activeTickets = db.getTickets().stream()
                .filter(t -> "OPEN".equals(t.getStatus())
                        || "IN_PROGRESS".equals(t.getStatus()))
                .collect(Collectors.toList());

        // 2. Instantiate the Visitor
        CustomerImpactVisitor visitor = new CustomerImpactVisitor();

        // 3. Visit each ticket
        // Here is the magic: t.accept(visitor) will call the correct visit method
        for (Ticket t : activeTickets) {
            t.accept(visitor);
        }

        // Calculate manual priority distribution (this doesn't depend on ticket type)
        Map<String, Integer> ticketsByPriority = new HashMap<>();
        ticketsByPriority.put("LOW", 0);
        ticketsByPriority.put("MEDIUM", 0);
        ticketsByPriority.put("HIGH", 0);
        ticketsByPriority.put("CRITICAL", 0);

        for (Ticket t : activeTickets) {
            String p = t.getPriority().name();
            ticketsByPriority.put(p, ticketsByPriority.getOrDefault(p, 0) + 1);
        }

        ObjectNode root = mapper.createObjectNode();
        root.put("command", "generateCustomerImpactReport");
        root.put("username", username);
        root.put("timestamp", timestamp);

        ObjectNode report = root.putObject("report");
        report.put("totalTickets", activeTickets.size());

        // Tickets By Type
        ObjectNode typeNode = report.putObject("ticketsByType");
        // Get counts from Visitor
        typeNode.put("BUG", visitor.getCount("BUG"));
        typeNode.put("FEATURE_REQUEST", visitor.getCount("FEATURE_REQUEST"));
        typeNode.put("UI_FEEDBACK", visitor.getCount("UI_FEEDBACK"));

        // Tickets By Priority
        ObjectNode prioNode = report.putObject("ticketsByPriority");
        prioNode.put("LOW", ticketsByPriority.get("LOW"));
        prioNode.put("MEDIUM", ticketsByPriority.get("MEDIUM"));
        prioNode.put("HIGH", ticketsByPriority.get("HIGH"));
        prioNode.put("CRITICAL", ticketsByPriority.get("CRITICAL"));

        // Customer Impact By Type (Average)
        ObjectNode impactNode = report.putObject("customerImpactByType");
        // Get averages calculated by Visitor
        impactNode.put("BUG", visitor.getAverageImpact("BUG"));
        impactNode.put("FEATURE_REQUEST", visitor.getAverageImpact("FEATURE_REQUEST"));
        impactNode.put("UI_FEEDBACK", visitor.getAverageImpact("UI_FEEDBACK"));

        outputs.add(root);
    }
}
