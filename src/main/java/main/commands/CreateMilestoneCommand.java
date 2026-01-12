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

public class CreateMilestoneCommand implements Command {
    private final Database db;
    private final InputData input;

    public CreateMilestoneCommand(Database db, InputData input) {
        this.db = db;
        this.input = input;
    }

    @Override
    public void execute(List<ObjectNode> outputs) {
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
            error.put("error", "The user does not have permission to execute this command: required role MANAGER; user role " + role + ".");
            outputs.add(error);
            return;
        }

        // Extragere date
        String timestamp = input.getTimestamp();
        // Fallback pentru nume (uneori e în 'name', alteori în 'milestoneName' în funcție de mapare)
        String name = input.getMilestoneName();
        if (name == null) name = input.getName();

        String dueDateStr = input.getDueDate();

        List<Integer> tickets = input.getTickets();
        if (tickets == null) tickets = new ArrayList<>();

        List<String> blockingFor = input.getBlockingFor();
        if (blockingFor == null) blockingFor = new ArrayList<>();

        List<String> assignedDevs = input.getAssignedDevs();
        if (assignedDevs == null) assignedDevs = new ArrayList<>();

        // --- EDGE CASE 2: Conflict Tichete ---
        for (Integer ticketId : tickets) {
            for (Milestone m : db.getMilestones()) {
                if (m.getTicketIds().contains(ticketId)) {
                    ObjectNode error = JsonNodeFactory.instance.objectNode();
                    error.put("command", "createMilestone");
                    error.put("username", username);
                    error.put("timestamp", timestamp);
                    // Mesaj exact care preia numele dinamic al milestone-ului existent
                    error.put("error", "Tickets " + ticketId + " already assigned to milestone " + m.getName() + ".");

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

        String notifMsg = "New milestone " + newMilestone.getName() + " has been created with due date " + newMilestone.getDueDate() + ".";

        for (String devName : newMilestone.getAssignedDevs()) {
            User u = db.findUserByUsername(devName);
            if (u instanceof main.model.user.Developer) {
                ((main.model.user.Developer) u).addNotification(notifMsg);
            }
        }
    }
}