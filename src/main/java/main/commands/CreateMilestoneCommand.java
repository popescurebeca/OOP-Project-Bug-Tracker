package main.commands;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import main.database.Database;
import main.model.Milestone;
import main.model.ticket.Ticket;
import main.model.user.Developer;
import main.model.user.User;
import main.utils.InputData;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Command to create a new milestone in the system.
 */
public class CreateMilestoneCommand implements Command {
    private final Database db;
    private final InputData input;


    public CreateMilestoneCommand(final Database db, final InputData input) {
        this.db = db;
        this.input = input;
    }

    @Override
    public void execute(final List<ObjectNode> outputs) {
        String username = input.getUsername();

        String role = db.getUserRole(username).toUpperCase();

        if (!"MANAGER".equals(role)) {
            ObjectNode error = JsonNodeFactory.instance.objectNode();
            error.put("command", "createMilestone");
            error.put("username", username);
            error.put("timestamp", input.getTimestamp());

            String message = "The user does not have permission "
                    + "to execute this command: required role MANAGER;"
                    + " user role " + role + ".";
            error.put("error", message);
            outputs.add(error);
            return;
        }

        String timestamp = input.getTimestamp();
        String name = input.getMilestoneName();
        if (name == null) {
            name = input.getName();
        }

        String dueDateStr = input.getDueDate();

        List<Integer> tickets = input.getTickets();
        if (tickets == null) {
            tickets = new ArrayList<>();
        }

        List<String> blockingFor = input.getBlockingFor();
        if (blockingFor == null) {
            blockingFor = new ArrayList<>();
        }

        List<String> assignedDevs = input.getAssignedDevs();
        if (assignedDevs == null) {
            assignedDevs = new ArrayList<>();
        }

        for (Integer ticketId : tickets) {
            for (Milestone m : db.getMilestones()) {
                if (m.getTicketIds().contains(ticketId)) {
                    ObjectNode error = JsonNodeFactory.instance.objectNode();
                    error.put("command", "createMilestone");
                    error.put("username", username);
                    error.put("timestamp", timestamp);
                    String message = "Tickets " + ticketId + " already assigned to milestone "
                            + m.getName() + ".";
                    error.put("error", message);

                    outputs.add(error);
                    return;
                }
            }
        }

        Milestone newMilestone = new Milestone(
                name,
                username,
                LocalDate.parse(timestamp),
                LocalDate.parse(dueDateStr),
                blockingFor,
                tickets,
                assignedDevs
        );

        db.addMilestone(newMilestone);

        for (Integer tid : newMilestone.getTicketIds()) {
            Ticket t = db.getTicket(tid);
            if (t != null) {
                t.addHistory(new Ticket.HistoryEntry("ADDED_TO_MILESTONE", username, timestamp)
                        .setMilestone(newMilestone.getName()));
            }
        }

        String notifMsg = "New milestone " + newMilestone.getName()
                + " has been created with due date "
                + newMilestone.getDueDate() + ".";

        for (String devName : newMilestone.getAssignedDevs()) {
            User user = db.findUserByUsername(devName);
            if ("DEVELOPER".equals(user.getRole().toString())) {
                ((Developer) user).addNotification(notifMsg);
            }
        }
    }
}
