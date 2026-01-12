package main.commands;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import main.database.Database;
import main.model.Milestone;
import main.model.ticket.Ticket;
import main.model.user.Developer;
import main.model.user.User;
import main.model.Priority;
import main.model.user.enums.Seniority;
import main.utils.InputData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Command to assign a ticket to a developer.
 */
public class AssignTicketCommand implements Command {
    private final Database db;
    private final InputData input;

    /**
     * Constructor for AssignTicketCommand.
     *
     * @param db    The database instance.
     * @param input The input data containing command parameters.
     */
    public AssignTicketCommand(final Database db, final InputData input) {
        this.db = db;
        this.input = input;
    }

    /**
     * Executes the ticket assignment command.
     *
     * @param outputs The list of outputs to append results to.
     */
    @Override
    public void execute(final List<ObjectNode> outputs) {
        String username = input.getUsername();
        int ticketId = input.getTicketId();
        String timestamp = input.getTimestamp();

        Ticket ticket = db.getTicket(ticketId);
        User user = db.findUserByUsername(username);
        Milestone milestone = db.findMilestoneByTicketId(ticketId);

        if (ticket == null) {
            return;
        }

        // Basic Checks
        if ("CLOSED".equals(ticket.getStatus())) {
            ObjectNode error = JsonNodeFactory.instance.objectNode();
            error.put("command", "assignTicket");
            error.put("username", username);
            error.put("timestamp", timestamp);
            error.put("error", "Cannot assign a CLOSED ticket.");
            outputs.add(error);
            return;
        }

        if (!(user instanceof Developer)) {
            return;
        }
        Developer dev = (Developer) user;

        // --- 1. EXPERTISE VALIDATION ---
        String ticketArea = ticket.getExpertiseArea();
        String userExpertise = dev.getExpertise().name();

        // Determine who is allowed to take this ticket
        List<String> allowedExpertise = new ArrayList<>();
        allowedExpertise.add("FULLSTACK"); // Always allowed

        // Reverse mapping from "Accessible Zones" table
        // Who has access to 'ticketArea'?
        if (ticketArea.equals("FRONTEND")) {
            allowedExpertise.add("FRONTEND");
            allowedExpertise.add("DESIGN"); // Design devs can access Frontend
        } else if (ticketArea.equals("BACKEND")) {
            allowedExpertise.add("BACKEND");
            // DB devs cannot access Backend (Table says DB -> DB)
        } else if (ticketArea.equals("DB")) {
            allowedExpertise.add("DB");
            allowedExpertise.add("BACKEND"); // Backend devs can access DB
        } else if (ticketArea.equals("DESIGN")) {
            allowedExpertise.add("DESIGN");
            allowedExpertise.add("FRONTEND"); // Frontend devs can access Design
        } else if (ticketArea.equals("DEVOPS")) {
            allowedExpertise.add("DEVOPS");
        }

        if (!allowedExpertise.contains(userExpertise)) {
            Collections.sort(allowedExpertise);
            ObjectNode error = JsonNodeFactory.instance.objectNode();
            error.put("command", "assignTicket");
            error.put("username", username);
            error.put("timestamp", timestamp);
            error.put("error", "Developer " + username + " cannot assign ticket " + ticketId
                    + " due to expertise area. Required: " + String.join(", ", allowedExpertise)
                    + "; Current: " + userExpertise + ".");
            outputs.add(error);
            return;
        }

        // --- 2. SENIORITY VALIDATION ---
        Priority p = ticket.getPriority();
        String type = ticket.getType(); // Need ticket type for table logic
        Seniority s = dev.getSeniority();

        // Calculate Minimum Seniority Required
        Seniority minRequired = Seniority.JUNIOR;

        // Rule: CRITICAL -> Senior only
        if (p == Priority.CRITICAL) {
            minRequired = Seniority.SENIOR;
        } else if (p == Priority.HIGH) {
            // Rule: HIGH -> Mid or Senior
            if (minRequired.ordinal() < Seniority.MID.ordinal()) {
                minRequired = Seniority.MID;
            }
        }

        // Rule: Feature Request -> Mid or Senior
        if ("FEATURE_REQUEST".equalsIgnoreCase(type)) {
            if (minRequired.ordinal() < Seniority.MID.ordinal()) {
                minRequired = Seniority.MID;
            }
        }

        // Check if user qualifies
        if (s.ordinal() < minRequired.ordinal()) {
            List<String> requiredSeniority = new ArrayList<>();
            // Build list of valid seniorities based on minRequired
            if (minRequired == Seniority.MID) {
                requiredSeniority.add("MID");
                requiredSeniority.add("SENIOR");
            } else if (minRequired == Seniority.SENIOR) {
                requiredSeniority.add("SENIOR");
            }
            // (If Junior is min, everyone qualifies, so no error)

            // Special case for output consistency: sometimes list order matters.
            // "Required: MID, SENIOR" is standard alphabetical order M before S.

            ObjectNode error = JsonNodeFactory.instance.objectNode();
            error.put("command", "assignTicket");
            error.put("username", username);
            error.put("timestamp", timestamp);
            error.put("error", "Developer " + username + " cannot assign ticket " + ticketId
                    + " due to seniority level. Required: "
                    + String.join(", ", requiredSeniority) + "; Current: " + s + ".");
            outputs.add(error);
            return;
        }

        // --- 3. MILESTONE VALIDATION ---
        if (milestone != null) {
            if (!milestone.getAssignedDevs().contains(username)) {
                ObjectNode error = JsonNodeFactory.instance.objectNode();
                error.put("command", "assignTicket");
                error.put("username", username);
                error.put("timestamp", timestamp);
                error.put("error", "Developer " + username
                        + " is not assigned to milestone " + milestone.getName() + ".");
                outputs.add(error);
                return;
            }
            if (isMilestoneBlocked(milestone)) {
                ObjectNode error = JsonNodeFactory.instance.objectNode();
                error.put("command", "assignTicket");
                error.put("username", username);
                error.put("timestamp", timestamp);
                error.put("error", "Cannot assign ticket " + ticketId
                        + " from blocked milestone " + milestone.getName() + ".");
                outputs.add(error);
                return;
            }
        }

        // --- 4. STATUS VALIDATION ---
        if (!"OPEN".equals(ticket.getStatus())) {
            ObjectNode error = JsonNodeFactory.instance.objectNode();
            error.put("command", "assignTicket");
            error.put("username", username);
            error.put("timestamp", timestamp);
            error.put("error", "Only OPEN tickets can be assigned.");
            outputs.add(error);
            return;
        }

        // Success
        ticket.setAssignee(username);
        ticket.setStatus("IN_PROGRESS");
        ticket.setAssignedAt(timestamp);

        ticket.addHistory(new Ticket.HistoryEntry("ASSIGNED", username, timestamp));
        ticket.addHistory(new Ticket.HistoryEntry("STATUS_CHANGED", username, timestamp)
                .setFromTo("OPEN", "IN_PROGRESS"));
    }

    /**
     * Checks if a milestone is blocked by other active milestones.
     *
     * @param m The milestone to check.
     * @return True if blocked, false otherwise.
     */
    private boolean isMilestoneBlocked(final Milestone m) {
        for (Milestone other : db.getMilestones()) {
            if (other == m) {
                continue;
            }
            if (other.getBlockingFor() != null
                    && other.getBlockingFor().contains(m.getName())) {
                boolean otherIsActive = other.getTicketIds().stream()
                        .map(db::getTicket)
                        .anyMatch(t -> t != null && !"CLOSED".equals(t.getStatus()));
                if (otherIsActive) {
                    return true;
                }
            }
        }
        return false;
    }
}
