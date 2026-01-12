package main.commands;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import main.database.Database;
import main.utils.InputData;
import main.commands.*;

import java.util.List;

/**
 * Factory class to create specific Command instances based on input data.
 */
public class CommandFactory {
    private static final Database db = Database.getInstance();

    /**
     * Creates a command based on the input string.
     *
     * @param data The input data containing the command name and parameters.
     * @return A concrete Command instance or an error command if unknown.
     */
    public static Command createCommand(InputData data) {
        String type = data.getCommand();

        return switch (type) {
            case "reportTicket" -> new ReportTicketCommand(data);
            case "viewTickets" -> new ViewTicketsCommand(data);
            case "createMilestone" -> new CreateMilestoneCommand(db, data);
            case "viewMilestones" -> new ViewMilestonesCommand(db, data);

            case "assignTicket" -> new AssignTicketCommand(db, data);
            case "undoAssignTicket" -> new UndoAssignTicketCommand(db, data);
            case "viewAssignedTickets" -> new ViewAssignedTicketsCommand(db, data);

            case "addComment" -> new AddCommentCommand(db, data);
            case "undoAddComment" -> new UndoAddCommentCommand(db, data);

            case "changeStatus" -> new ChangeStatusCommand(db, data);
            case "undoChangeStatus" -> new UndoChangeStatusCommand(db, data);
            case "viewTicketHistory" -> new ViewTicketHistoryCommand(db, data);

            case "search" -> new SearchCommand(db, data);
            case "viewNotifications" -> new ViewNotificationsCommand(db, data);

            case "generateCustomerImpactReport" -> new GenerateCustomerImpactReportCommand(db, data);
            case "generateTicketRiskReport" -> new GenerateTicketRiskReportCommand(db, data);
            case "generateResolutionEfficiencyReport" -> new GenerateResolutionEfficiencyReportCommand(db, data);
            case "generatePerformanceReport" -> new GeneratePerformanceReportCommand(db, data);

            case "appStabilityReport" -> new AppStabilityReportCommand(db, data);
            default -> null;
        };
    }
}