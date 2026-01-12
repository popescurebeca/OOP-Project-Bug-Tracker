package main.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import main.database.Database;
import main.model.ticket.Ticket;
import main.model.user.Developer;
import main.model.user.Manager;
import main.model.user.User;
import main.model.user.enums.Seniority;
import main.utils.InputData;
import main.visitor.PerformanceStatsVisitor;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command to generate a performance report for developers.
 */
public class GeneratePerformanceReportCommand implements Command {
    private final Database db;
    private final InputData input;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Constructor for GeneratePerformanceReportCommand.
     *
     * @param db    The database instance.
     * @param input The input data containing command parameters.
     */
    public GeneratePerformanceReportCommand(final Database db, final InputData input) {
        this.db = db;
        this.input = input;
    }

    /**
     * Executes the performance report generation.
     *
     * @param outputs The list of outputs to append results to.
     */
    @Override
    public void execute(final List<ObjectNode> outputs) {
        String managerUsername = input.getUsername();
        LocalDate commandDate = LocalDate.parse(input.getTimestamp());

        // 1. Determine "Previous Month"
        YearMonth previousMonth = YearMonth.from(commandDate).minusMonths(1);

        User u = db.findUserByUsername(managerUsername);
        if (!(u instanceof Manager)) {
            return;
        }
        Manager manager = (Manager) u;

        // 2. Collect subordinates (Developers)
        List<Developer> team = manager.getSubordinates().stream()
                .map(db::findUserByUsername)
                .filter(user -> user instanceof Developer)
                .map(user -> (Developer) user)
                .sorted(Comparator.comparing(Developer::getUsername)) // Lexicographical sort
                .collect(Collectors.toList());

        // 3. Prepare output
        ObjectNode root = mapper.createObjectNode();
        root.put("command", "generatePerformanceReport");
        root.put("username", managerUsername);
        root.put("timestamp", input.getTimestamp());
        ArrayNode reportArray = root.putArray("report");

        // 4. Process each developer
        for (Developer dev : team) {
            // Filter relevant tickets:
            // - CLOSED
            // - Assignee = dev
            // - SolvedAt in previous month
            List<Ticket> devTickets = db.getTickets().stream()
                    .filter(t -> "CLOSED".equals(t.getStatus()))
                    .filter(t -> dev.getUsername().equals(t.getAssignee()))
                    .filter(t -> {
                        if (t.getSolvedAt() == null) {
                            return false;
                        }
                        LocalDate solvedDate = LocalDate.parse(t.getSolvedAt());
                        return YearMonth.from(solvedDate).equals(previousMonth);
                    })
                    .collect(Collectors.toList());

            // Use Visitor to gather raw data
            PerformanceStatsVisitor statsVisitor = new PerformanceStatsVisitor();
            for (Ticket t : devTickets) {
                t.accept(statsVisitor);
            }

            // Calculate score
            double score = calculateScore(dev, statsVisitor);

            // UPDATE SCORE IN DATABASE (for later Search command)
            dev.setPerformanceScore(score);

            // Build JSON per developer
            ObjectNode devNode = reportArray.addObject();
            devNode.put("username", dev.getUsername());
            devNode.put("closedTickets", statsVisitor.getClosedTicketsCount());

            // Format average time to 2 decimal places (but keep double in JSON for precision)
            double avgTime = statsVisitor.getAverageResolutionTime();
            // Reference seems to have 2 decimal precision on display
            devNode.put("averageResolutionTime", round(avgTime));

            devNode.put("performanceScore", score);
            devNode.put("seniority", dev.getSeniority().name());
        }

        outputs.add(root);
    }

    /**
     * Calculates the performance score for a developer.
     *
     * @param dev   The developer.
     * @param stats The visitor stats containing raw metrics.
     * @return The calculated score.
     */
    private double calculateScore(final Developer dev, final PerformanceStatsVisitor stats) {
        if (stats.getClosedTicketsCount() == 0) {
            return 0.0;
        }

        Seniority seniority = dev.getSeniority();
        double rawScore = 0.0;
        int bonus = 0;

        int closed = stats.getClosedTicketsCount();
        int highPrio = stats.getHighPriorityCount();
        double avgTime = stats.getAverageResolutionTime();

        switch (seniority) {
            case JUNIOR:
                double diversity = ticketDiversityFactor(stats.getBugCount(),
                        stats.getFeatureCount(), stats.getUiCount());
                rawScore = Math.max(0, 0.5 * closed - diversity);
                bonus = 5;
                break;
            case MID:
                rawScore = Math.max(0, 0.5 * closed + 0.7 * highPrio - 0.3 * avgTime);
                bonus = 15;
                break;
            case SENIOR:
                rawScore = Math.max(0, 0.5 * closed + 1.0 * highPrio - 0.5 * avgTime);
                bonus = 30;
                break;
            default:
                break;
        }

        double total = rawScore + bonus;
        return round(total);
    }

    // --- MATHEMATICAL METHODS FROM REQUIREMENTS ---

    private double round(final double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private double averageResolvedTicketType(final int bug, final int feature, final int ui) {
        return (bug + feature + ui) / 3.0;
    }

    private double standardDeviation(final int bug, final int feature, final int ui) {
        double mean = averageResolvedTicketType(bug, feature, ui);
        double variance = (Math.pow(bug - mean, 2)
                + Math.pow(feature - mean, 2)
                + Math.pow(ui - mean, 2)) / 3.0;
        return Math.sqrt(variance);
    }

    private double ticketDiversityFactor(final int bug, final int feature, final int ui) {
        double mean = averageResolvedTicketType(bug, feature, ui);
        if (mean == 0.0) {
            return 0.0;
        }
        double std = standardDeviation(bug, feature, ui);
        return std / mean;
    }
}