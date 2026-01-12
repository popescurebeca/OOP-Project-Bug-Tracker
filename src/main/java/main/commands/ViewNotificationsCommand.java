package main.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import main.database.Database;
import main.model.Milestone;
import main.model.user.Developer;
import main.model.user.User;
import main.utils.InputData;

import java.time.LocalDate;
import java.util.List;

/**
 * Command to view notifications for a developer.
 */
public class ViewNotificationsCommand implements Command {
    private final Database db;
    private final InputData input;

    /**
     * Constructor for ViewNotificationsCommand.
     *
     * @param db    The database instance.
     * @param input The input data containing command parameters.
     */
    public ViewNotificationsCommand(final Database db, final InputData input) {
        this.db = db;
        this.input = input;
    }

    /**
     * Executes the view notifications command.
     *
     * @param outputs The list of outputs to append results to.
     */
    @Override
    public void execute(final List<ObjectNode> outputs) {
        String username = input.getUsername();
        String timestamp = input.getTimestamp();
        LocalDate currentDay = LocalDate.parse(timestamp);

        // 1. Apply global rules (which may generate new notifications based on date)
        for (Milestone m : db.getMilestones()) {
            m.applyRules(db, currentDay);
        }

        User user = db.findUserByUsername(username);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode();

        result.put("command", "viewNotifications");
        result.put("username", username);
        result.put("timestamp", timestamp);

        ArrayNode notifsArray = result.putArray("notifications");

        if (user instanceof Developer) {
            Developer dev = (Developer) user;
            List<String> notifications = dev.getNotifications();

            // 2. Add notifications to output
            for (String msg : notifications) {
                notifsArray.add(msg);
            }

            // 3. Clear notifications after viewing
            dev.clearNotifications();
        }
        // If user is not a developer, list remains empty (or handle error if spec says so)

        outputs.add(result);
    }
}
