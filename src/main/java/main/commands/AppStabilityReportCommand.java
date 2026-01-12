package main.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import main.database.Database;
import main.model.ticket.Ticket;
import main.utils.InputData;
import main.visitor.CustomerImpactVisitor;
import main.visitor.TicketRiskVisitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Command to generate an application stability report.
 * Combines Ticket Risk and Customer Impact metrics.
 */
public class AppStabilityReportCommand implements Command {
    private final Database db;
    private final InputData input;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final double STABILITY_THRESHOLD = 50.0;

    /**
     * Constructor for AppStabilityReportCommand.
     *
     * @param db    The database instance.
     * @param input The input data containing command parameters.
     */
    public AppStabilityReportCommand(final Database db, final InputData input) {
        this.db = db;
        this.input = input;
    }

    /**
     * Executes the application stability report generation.
     *
     * @param outputs The list of outputs to append results to.
     */
    @Override
    public void execute(final List<ObjectNode> outputs) {
        String username = input.getUsername();
        String timestamp = input.getTimestamp();

        // 1. Filter active tickets (OPEN and IN_PROGRESS)
        List<Ticket> activeTickets = db.getTickets().stream()
                .filter(t -> "OPEN".equals(t.getStatus())
                        || "IN_PROGRESS".equals(t.getStatus()))
                .collect(Collectors.toList());

        // 2. Instantiate Visitors
        TicketRiskVisitor riskVisitor = new TicketRiskVisitor();
        CustomerImpactVisitor impactVisitor = new CustomerImpactVisitor();

        // 3. Visit tickets to calculate metrics
        for (Ticket t : activeTickets) {
            t.accept(riskVisitor);
            t.accept(impactVisitor);
        }

        // 4. Calculate priority distribution (manually)
        Map<String, Integer> openTicketsByPriority = new HashMap<>();
        openTicketsByPriority.put("LOW", 0);
        openTicketsByPriority.put("MEDIUM", 0);
        openTicketsByPriority.put("HIGH", 0);
        openTicketsByPriority.put("CRITICAL", 0);

        for (Ticket t : activeTickets) {
            String p = t.getPriority().name();
            openTicketsByPriority.put(p, openTicketsByPriority.getOrDefault(p, 0) + 1);
        }

        // 5. Collect results from visitors
        Map<String, String> riskByType = new HashMap<>();
        riskByType.put("BUG", riskVisitor.getRiskQualifier("BUG"));
        riskByType.put("FEATURE_REQUEST", riskVisitor.getRiskQualifier("FEATURE_REQUEST"));
        riskByType.put("UI_FEEDBACK", riskVisitor.getRiskQualifier("UI_FEEDBACK"));

        Map<String, Double> impactByType = new HashMap<>();
        impactByType.put("BUG", impactVisitor.getAverageImpact("BUG"));
        impactByType.put("FEATURE_REQUEST", impactVisitor.getAverageImpact("FEATURE_REQUEST"));
        impactByType.put("UI_FEEDBACK", impactVisitor.getAverageImpact("UI_FEEDBACK"));

        // 6. Determine APP STABILITY
        String appStability = "PARTIALLY STABLE"; // Default

        if (activeTickets.isEmpty()) {
            appStability = "STABLE";
        } else {
            // Check UNSTABLE: At least one SIGNIFICANT or MAJOR risk
            boolean isUnstable = riskByType.values().stream()
                    .anyMatch(r -> "SIGNIFICANT".equals(r) || "MAJOR".equals(r));

            if (isUnstable) {
                appStability = "UNSTABLE";
            } else {
                // Check STABLE: All risks NEGLIGIBLE AND All impacts < 50
                boolean allRisksNegligible = riskByType.values().stream()
                        .allMatch(r -> "NEGLIGIBLE".equals(r));

                boolean allImpactsLow = impactByType.values().stream()
                        .allMatch(i -> i < STABILITY_THRESHOLD);

                if (allRisksNegligible && allImpactsLow) {
                    appStability = "STABLE";
                }
            }
        }

        // 7. Build output JSON
        ObjectNode root = mapper.createObjectNode();
        root.put("command", "appStabilityReport");
        root.put("username", username);
        root.put("timestamp", timestamp);

        ObjectNode report = root.putObject("report");
        report.put("totalOpenTickets", activeTickets.size());

        ObjectNode typeNode = report.putObject("openTicketsByType");
        typeNode.put("BUG", riskVisitor.getCount("BUG")); // Reuse count from visitor
        typeNode.put("FEATURE_REQUEST", riskVisitor.getCount("FEATURE_REQUEST"));
        typeNode.put("UI_FEEDBACK", riskVisitor.getCount("UI_FEEDBACK"));

        ObjectNode prioNode = report.putObject("openTicketsByPriority");
        prioNode.put("LOW", openTicketsByPriority.get("LOW"));
        prioNode.put("MEDIUM", openTicketsByPriority.get("MEDIUM"));
        prioNode.put("HIGH", openTicketsByPriority.get("HIGH"));
        prioNode.put("CRITICAL", openTicketsByPriority.get("CRITICAL"));

        ObjectNode riskNode = report.putObject("riskByType");
        riskNode.put("BUG", riskByType.get("BUG"));
        riskNode.put("FEATURE_REQUEST", riskByType.get("FEATURE_REQUEST"));
        riskNode.put("UI_FEEDBACK", riskByType.get("UI_FEEDBACK"));

        ObjectNode impactNode = report.putObject("impactByType");
        impactNode.put("BUG", impactByType.get("BUG"));
        impactNode.put("FEATURE_REQUEST", impactByType.get("FEATURE_REQUEST"));
        impactNode.put("UI_FEEDBACK", impactByType.get("UI_FEEDBACK"));

        report.put("appStability", appStability);

        outputs.add(root);

        // 8. Execution stop logic
        if ("STABLE".equals(appStability)) {
            db.stopExec();
        }
    }
}
