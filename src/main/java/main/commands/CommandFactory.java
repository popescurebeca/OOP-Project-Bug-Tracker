package main.commands;

import main.database.Database;
import main.utils.InputData;

/**
 * Factory class to create specific Command instances based on input data.
 */
public final class CommandFactory {
    private static final Database DB = Database.getInstance();

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private CommandFactory() {
    }

    /**
     * Creates a command based on the input string.
     *
     * @param data The input data containing the command name and parameters.
     * @return A concrete Command instance or null if unknown.
     */
    public static Command createCommand(final InputData data) {
        String type = data.getCommand();

        return switch (type) {
            case "reportTicket" -> new ReportTicketCommand(data);
            case "viewTickets" -> new ViewTicketsCommand(data);
            case "createMilestone" -> new CreateMilestoneCommand(DB, data);
            case "viewMilestones" -> new ViewMilestonesCommand(DB, data);

            case "assignTicket" -> new AssignTicketCommand(DB, data);
            case "undoAssignTicket" -> new UndoAssignTicketCommand(DB, data);
            case "viewAssignedTickets" -> new ViewAssignedTicketsCommand(DB, data);

            case "addComment" -> new AddCommentCommand(DB, data);
            case "undoAddComment" -> new UndoAddCommentCommand(DB, data);

            case "changeStatus" -> new ChangeStatusCommand(DB, data);
            case "undoChangeStatus" -> new UndoChangeStatusCommand(DB, data);
            case "viewTicketHistory" -> new ViewTicketHistoryCommand(DB, data);

            case "search" -> new SearchCommand(DB, data);
            case "viewNotifications" -> new ViewNotificationsCommand(DB, data);

            case "generateCustomerImpactReport" -> new GenerateCustomerImpactReportCommand(DB, data);
            case "generateTicketRiskReport" -> new GenerateTicketRiskReportCommand(DB, data);
            case "generateResolutionEfficiencyReport" ->
                    new GenerateResolutionEfficiencyReportCommand(DB, data);
            case "generatePerformanceReport" -> new GeneratePerformanceReportCommand(DB, data);

            case "appStabilityReport" -> new AppStabilityReportCommand(DB, data);
            default -> null;
        };
    }
}
