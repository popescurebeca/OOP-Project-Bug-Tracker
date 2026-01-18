package main.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import main.database.Database;
import main.model.ticket.Ticket;
import main.model.user.Developer;
import main.model.user.User;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a milestone in the project management system.
 */
public final class Milestone {
    private static final int BUMP_INTERVAL_DAYS = 3;

    private final String name;
    private final String createdBy;
    private final LocalDate createdAt;
    private final LocalDate dueDate;
    // List of names of milestones this one blocks
    private final List<String> blockingFor;
    private final List<Integer> ticketIds;
    private final List<String> assignedDevs;

    private LocalDate completionDate = null;

    // --- CÂMPURI NOI PENTRU NOTIFICĂRI ---
    @JsonIgnore
    private boolean notifiedDueTomorrow = false;
    @JsonIgnore
    private boolean notifiedUnblockedAfterDue = false;


    public Milestone(final String name, final String createdBy, final LocalDate createdAt,
                     final LocalDate dueDate, final List<String> blockingFor,
                     final List<Integer> ticketIds, final List<String> assignedDevs) {
        this.name = name;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.dueDate = dueDate;
        this.blockingFor = blockingFor;
        this.ticketIds = ticketIds;
        this.assignedDevs = assignedDevs;
    }


    public String getName() {
        return name;
    }


    public String getCreatedBy() {
        return createdBy;
    }


    public LocalDate getCreatedAt() {
        return createdAt;
    }


    public LocalDate getDueDate() {
        return dueDate;
    }


    public List<String> getBlockingFor() {
        return blockingFor;
    }


    public List<Integer> getTicketIds() {
        return ticketIds;
    }


    public List<String> getAssignedDevs() {
        return assignedDevs;
    }


    public String getStatus(final List<Ticket> allTickets) {
        if (ticketIds.isEmpty()) {
            return "COMPLETED";
        }

        boolean allClosed = true;
        for (Integer id : ticketIds) {
            Ticket t = findTicketById(allTickets, id);
            if (t != null && !"CLOSED".equals(t.getStatus())) {
                allClosed = false;
                break;
            }
        }
        return allClosed ? "COMPLETED" : "ACTIVE";
    }

    /**
     * Calculates the completion percentage.
     *
     * @param allTickets List of all tickets.
     * @return The percentage (0.0 to 1.0).
     */
    public double getCompletionPercentage(final List<Ticket> allTickets) {
        if (ticketIds.isEmpty()) {
            return 0.00;
        }
        long closedCount = ticketIds.stream()
                .map(id -> findTicketById(allTickets, id))
                .filter(t -> t != null && "CLOSED".equals(t.getStatus()))
                .count();
        return (double) closedCount / ticketIds.size();
    }

    /**
     * Gets IDs of open tickets.
     *
     * @param allTickets List of all tickets.
     * @return List of open ticket IDs.
     */
    public List<Integer> getOpenTickets(final List<Ticket> allTickets) {
        List<Integer> open = new ArrayList<>();
        for (Integer id : ticketIds) {
            Ticket t = findTicketById(allTickets, id);
            if (t != null && !"CLOSED".equals(t.getStatus())) {
                open.add(id);
            }
        }
        return open;
    }

    /**
     * Gets IDs of closed tickets.
     *
     * @param allTickets List of all tickets.
     * @return List of closed ticket IDs.
     */
    public List<Integer> getClosedTickets(final List<Ticket> allTickets) {
        List<Integer> closed = new ArrayList<>();
        for (Integer id : ticketIds) {
            Ticket t = findTicketById(allTickets, id);
            if (t != null && "CLOSED".equals(t.getStatus())) {
                closed.add(id);
            }
        }
        return closed;
    }

    /**
     * Calculates days remaining until due date.
     *
     * @param currentDay The current simulation date.
     * @param allTickets List of all tickets.
     * @return Days until due, or 0 if overdue.
     */
    public long getDaysUntilDue(final LocalDate currentDay, final List<Ticket> allTickets) {
        updateCompletionDateIfFinished(allTickets, currentDay);
        LocalDate refDate = (completionDate != null) ? completionDate : currentDay;

        // Dacă e trecut de due date, returnează 0
        if (refDate.isAfter(dueDate)) {
            return 0;
        }

        return ChronoUnit.DAYS.between(refDate, dueDate) + 1;
    }

    /**
     * Calculates days overdue.
     *
     * @param currentDay The current simulation date.
     * @param allTickets List of all tickets.
     * @return Days overdue, or 0 if not overdue.
     */
    public long getOverdueBy(final LocalDate currentDay, final List<Ticket> allTickets) {
        updateCompletionDateIfFinished(allTickets, currentDay);
        LocalDate refDate = (completionDate != null) ? completionDate : currentDay;

        if (!refDate.isAfter(dueDate)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(dueDate, refDate);
    }

    private void updateCompletionDateIfFinished(final List<Ticket> allTickets,
                                                final LocalDate currentDay) {
        if ("COMPLETED".equals(getStatus(allTickets)) && completionDate == null) {
            this.completionDate = currentDay;
        }
    }

    private Ticket findTicketById(final List<Ticket> allTickets, final int id) {
        return allTickets.stream().filter(t -> t.getId() == id).findFirst().orElse(null);
    }


    /**
     * Applies rules related to milestones (notifications, priority updates).
     *
     * @param db         The database instance.
     * @param currentDay The current simulation date.
     */
    public void applyRules(final Database db, final LocalDate currentDay) {

        // If blocked, do nothing
        if (isBlocked(db)) {
            return;
        }


        boolean isDependent = false;
        for (Milestone m : db.getMilestones()) {
            if (m != this && m.getBlockingFor() != null
                    && m.getBlockingFor().contains(this.name)) {
                isDependent = true;
                break;
            }
        }

        // Apply notification for unblocked after due date
        if (isDependent && currentDay.isAfter(this.dueDate) && !notifiedUnblockedAfterDue) {
            String msg = "Milestone " + this.name
                    + " was unblocked after due date. All active tickets are now CRITICAL.";
            sendNotificationToDevs(db, msg);
            notifiedUnblockedAfterDue = true;

            setAllTicketsCritical(db);
        }

        // Calculate days until due
        long daysUntilDue = ChronoUnit.DAYS.between(currentDay, this.dueDate) + 1;
        boolean isCriticalTime = (daysUntilDue <= 2);

        if (isCriticalTime) {
            // Due tomorrow or today
            if (daysUntilDue == 2 && !notifiedDueTomorrow) {
                String msg = "Milestone " + this.name
                        + " is due tomorrow. All unresolved tickets are now CRITICAL.";
                sendNotificationToDevs(db, msg);
                notifiedDueTomorrow = true;
            }

            setAllTicketsCritical(db);
        } else {
            // 3 days or more until due - apply 3-day bump
            apply3DayBump(db, currentDay);
        }
    }


    private void setAllTicketsCritical(final Database db) {
        for (Integer id : this.ticketIds) {
            Ticket t = db.getTicket(id);
            if (t != null && !"CLOSED".equals(t.getStatus())) {
                t.setForcePriority(main.model.Priority.CRITICAL);
            }
        }
    }

    private void sendNotificationToDevs(final Database db, final String msg) {
        if (this.assignedDevs == null) {
            return;
        }

        for (String username : this.assignedDevs) {
            User u = db.findUserByUsername(username);
            Developer dev = null;
            if ("DEVELOPER".equalsIgnoreCase(db.getUserRole(username))) {
                dev = (Developer) u;
                dev.addNotification(msg);
            }
        }
    }

    private void apply3DayBump(final Database db, final LocalDate currentDay) {
        // How many days active?
        long daysActive = ChronoUnit.DAYS.between(this.createdAt, currentDay);

        int bumps = (int) (daysActive / BUMP_INTERVAL_DAYS);

        if (bumps > 0) {
            for (Integer id : this.ticketIds) {
                Ticket t = db.getTicket(id);
                if (t != null && !"CLOSED".equals(t.getStatus())) {
                    main.model.Priority base = t.getInitialPriority();
                    main.model.Priority target = base;

                    for (int i = 0; i < bumps; i++) {
                        target = target.next();
                    }

                    t.setForcePriority(target);
                }
            }
        }
    }

    // Verify if this milestone is blocked by any active milestone
    private boolean isBlocked(final Database db) {
        for (Milestone other : db.getMilestones()) {
            if (other == this) {
                continue;
            }

            if (other.getBlockingFor() != null
                    && other.getBlockingFor().contains(this.name)) {

                boolean otherIsActive = false;
                for (Integer tid : other.getTicketIds()) {
                    Ticket t = db.getTicket(tid);
                    if (t != null && !"CLOSED".equals(t.getStatus())) {
                        otherIsActive = true;
                        break;
                    }
                }

                if (otherIsActive) {
                    return true; // Blocked
                }
            }
        }
        return false;
    }
}
