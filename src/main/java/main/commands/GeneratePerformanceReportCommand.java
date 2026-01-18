package main.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import main.database.Database;
import main.model.Milestone;
import main.model.Priority;
import main.model.ticket.Ticket;
import main.model.user.Developer;
import main.model.user.Manager;
import main.model.user.User;
import main.model.user.enums.Seniority;
import main.utils.InputData;
import main.visitor.PerformanceStatsVisitor;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Command to generate a performance report for developers.
 */
public final class GeneratePerformanceReportCommand implements Command {

    // --- Constants for Calculations ---
    private static final double JUNIOR_CLOSED_COEFF = 0.5;
    private static final int JUNIOR_BONUS = 5;

    private static final double MID_CLOSED_COEFF = 0.5;
    private static final double MID_HIGH_PRIO_COEFF = 0.7;
    private static final double MID_TIME_COEFF = 0.3;
    private static final int MID_BONUS = 15;

    private static final double SENIOR_CLOSED_COEFF = 0.5;
    private static final double SENIOR_HIGH_PRIO_COEFF = 1.0;
    private static final double SENIOR_TIME_COEFF = 0.5;
    private static final int SENIOR_BONUS = 30;

    private static final double ROUNDING_FACTOR = 100.0;
    private static final double TICKET_TYPES_COUNT = 3.0;
    private static final int BUMP_INTERVAL_DAYS = 3;

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
        Manager manager = null;
        if ("MANAGER".equals(String.valueOf(u.getRole()))) {
            manager = (Manager) u;
        } else {
            // Invalid user role for this command
            return;
        }

        // 2. Collect subordinates (Developers)
        List<Developer> team = manager.getSubordinates().stream()
                .map(db::findUserByUsername).filter(Objects::nonNull)
                // Convertim fiecare User într-un Optional<Developer>
                .map(User::isDeveloper)
                // Păstrăm doar cei care sunt prezenți (adică sunt Developers) și îi extragem
                .flatMap(java.util.Optional::stream)
                .sorted(Comparator.comparing(Developer::getUsername))
                .toList();

        // 3. Prepare output
        ObjectNode root = mapper.createObjectNode();
        root.put("command", "generatePerformanceReport");
        root.put("username", managerUsername);
        root.put("timestamp", input.getTimestamp());
        ArrayNode reportArray = root.putArray("report");

        // 4. Process each developer
        for (Developer dev : team) {
            // Filter relevant tickets
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

            for (Ticket t : devTickets) {
                if (t.getInitialPriority() == Priority.LOW) {
                    continue;
                }

                // Determine active duration: CreatedAt -> SolvedAt
                // Use first closed date for consistency with time calculation
                String solvedStr = getFirstClosedDate(t);
                if (solvedStr == null) {
                    solvedStr = t.getSolvedAt();
                }

                if (solvedStr != null && t.getCreatedAt() != null) {
                    LocalDate created = LocalDate.parse(t.getCreatedAt());
                    LocalDate solved = LocalDate.parse(solvedStr);
                    long daysActive = ChronoUnit.DAYS.between(created, solved);

                    // Check if ticket belonged to a milestone
                    Milestone m = db.findMilestoneByTicketId(t.getId());
                    if (m != null) {
                        // Apply bump: every 3 days
                        int bumps = (int) (daysActive / BUMP_INTERVAL_DAYS);
                        if (bumps > 0) {
                            Priority effectivePriority = t.getInitialPriority();
                            for (int i = 0; i < bumps; i++) {
                                effectivePriority = effectivePriority.next();
                            }
                            t.setForcePriority(effectivePriority);
                        }
                    }
                }
            }

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

    private String getFirstClosedDate(final Ticket t) {
        if (t.getHistory() != null) {
            for (Ticket.HistoryEntry entry : t.getHistory()) {
                if ("STATUS_CHANGED".equals(entry.getAction())
                        && "CLOSED".equalsIgnoreCase(entry.getTo())) {
                    return entry.getTimestamp();
                }
            }
        }
        return null;
    }


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
                rawScore = Math.max(0, JUNIOR_CLOSED_COEFF * closed - diversity);
                bonus = JUNIOR_BONUS;
                break;
            case MID:
                rawScore = Math.max(0, MID_CLOSED_COEFF * closed
                        + MID_HIGH_PRIO_COEFF * highPrio
                        - MID_TIME_COEFF * avgTime);
                bonus = MID_BONUS;
                break;
            case SENIOR:
                rawScore = Math.max(0, SENIOR_CLOSED_COEFF * closed
                        + SENIOR_HIGH_PRIO_COEFF * highPrio
                        - SENIOR_TIME_COEFF * avgTime);
                bonus = SENIOR_BONUS;
                break;
            default:
                break;
        }

        double total = rawScore + bonus;
        return round(total);
    }

    // MATHEMATICAL METHODS

    private double round(final double value) {
        return Math.round(value * ROUNDING_FACTOR) / ROUNDING_FACTOR;
    }

    private double averageResolvedTicketType(final int bug, final int feature, final int ui) {
        return (bug + feature + ui) / TICKET_TYPES_COUNT;
    }

    private double standardDeviation(final int bug, final int feature, final int ui) {
        double mean = averageResolvedTicketType(bug, feature, ui);
        double variance = (Math.pow(bug - mean, 2)
                + Math.pow(feature - mean, 2)
                + Math.pow(ui - mean, 2)) / TICKET_TYPES_COUNT;
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