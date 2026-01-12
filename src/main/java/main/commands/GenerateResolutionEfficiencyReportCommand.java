package main.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import main.database.Database;
import main.model.ticket.Ticket;
import main.utils.InputData;
import main.visitor.ResolutionEfficiencyVisitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Command to generate the Resolution Efficiency Report.
 */
public class GenerateResolutionEfficiencyReportCommand implements Command {
    private final Database db;
    private final InputData input;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Constructor for GenerateResolutionEfficiencyReportCommand.
     *
     * @param db    The database instance.
     * @param input The input data containing command parameters.
     */
    public GenerateResolutionEfficiencyReportCommand(final Database db, final InputData input) {
        this.db = db;
        this.input = input;
    }

    /**
     * Executes the resolution efficiency report generation.
     *
     * @param outputs The list of outputs to append results to.
     */
    @Override
    public void execute(final List<ObjectNode> outputs) {
        // 1. Filter: Only RESOLVED or CLOSED tickets
        List<Ticket> completedTickets = db.getTickets().stream()
                .filter(t -> "RESOLVED".equals(t.getStatus()) || "CLOSED".equals(t.getStatus()))
                .collect(Collectors.toList());

        // 2. Visitor
        ResolutionEfficiencyVisitor visitor = new ResolutionEfficiencyVisitor();
        for (Ticket t : completedTickets) {
            t.accept(visitor);
        }

        // 3. Priority Calculation (manual, not via visitor)
        Map<String, Integer> ticketsByPriority = new HashMap<>();
        ticketsByPriority.put("LOW", 0);
        ticketsByPriority.put("MEDIUM", 0);
        ticketsByPriority.put("HIGH", 0);
        ticketsByPriority.put("CRITICAL", 0);

        for (Ticket t : completedTickets) {
            if (t.getPriority() != null) {
                String p = t.getPriority().name();
                ticketsByPriority.put(p, ticketsByPriority.getOrDefault(p, 0) + 1);
            }
        }

        // 4. JSON Output
        ObjectNode root = mapper.createObjectNode();
        root.put("command", "generateResolutionEfficiencyReport");
        root.put("username", input.getUsername());
        root.put("timestamp", input.getTimestamp());

        ObjectNode report = root.putObject("report");
        report.put("totalTickets", completedTickets.size());

        ObjectNode typeNode = report.putObject("ticketsByType");
        typeNode.put("BUG", visitor.getCount("BUG"));
        typeNode.put("FEATURE_REQUEST", visitor.getCount("FEATURE_REQUEST"));
        typeNode.put("UI_FEEDBACK", visitor.getCount("UI_FEEDBACK"));

        ObjectNode prioNode = report.putObject("ticketsByPriority");
        prioNode.put("LOW", ticketsByPriority.get("LOW"));
        prioNode.put("MEDIUM", ticketsByPriority.get("MEDIUM"));
        prioNode.put("HIGH", ticketsByPriority.get("HIGH"));
        prioNode.put("CRITICAL", ticketsByPriority.get("CRITICAL"));

        ObjectNode effNode = report.putObject("efficiencyByType");
        effNode.put("BUG", visitor.getAverageEfficiency("BUG"));
        effNode.put("FEATURE_REQUEST", visitor.getAverageEfficiency("FEATURE_REQUEST"));
        effNode.put("UI_FEEDBACK", visitor.getAverageEfficiency("UI_FEEDBACK"));

        outputs.add(root);
    }
}
