package main.commands;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import main.database.Database;
import main.model.Milestone;
import main.model.ticket.Ticket;
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

    /**
     * Constructor for CreateMilestoneCommand.
     *
     * @param db    The database instance.
     * @param input The input data containing command parameters.
     */
    public CreateMilestoneCommand(final Database db, final InputData input) {
        this.db = db;
        this.input = input;
    }

    @Override
    public void execute(final List<ObjectNode> outputs) {
        String username = input.getUsername();

        // Determinăm rolul (asigurăm Uppercase pentru mesajul de eroare)
        String role = db.getUserRole(username).toUpperCase();

        // --- EDGE CASE 1: Verificare Permisiuni ---
        if (!"MANAGER".equals(role)) {
            ObjectNode error = JsonNodeFactory.instance.objectNode();
            error.put("command", "createMilestone");
            error.put("username", username);
            error.put("timestamp", input.getTimestamp());
            // Mesaj exact conform REF
            String message = "The user does not have permission "
                    + "to execute this command: required role MANAGER;"
                    + " user role " + role + ".";
            error.put("error", message);
            outputs.add(error);
            return;
        }

        // Extragere date
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

        // --- EDGE CASE 2: Conflict Tichete ---
        for (Integer ticketId : tickets) {
            for (Milestone m : db.getMilestones()) {
                if (m.getTicketIds().contains(ticketId)) {
                    ObjectNode error = JsonNodeFactory.instance.objectNode();
                    error.put("command", "createMilestone");
                    error.put("username", username);
                    error.put("timestamp", timestamp);
                    // Mesaj exact care preia numele dinamic al milestone-ului existent
                    String message = "Tickets " + ticketId + " already assigned to milestone "
                            + m.getName() + ".";
                    error.put("error", message);

                    outputs.add(error);
                    return; // Stop la prima eroare
                }
            }
        }

        // Creare și Salvare (doar dacă nu sunt erori)
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
            User u = db.findUserByUsername(devName);
            if (u instanceof main.model.user.Developer) {
                ((main.model.user.Developer) u).addNotification(notifMsg);
            }
        }
    }
}
