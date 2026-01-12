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

public class Milestone {
    private String name;
    private String createdBy;
    private LocalDate createdAt;
    private LocalDate dueDate;
    private List<String> blockingFor; // Lista de nume ale milestone-urilor pe care acesta le blochează
    private List<Integer> ticketIds;
    private List<String> assignedDevs;

    private LocalDate completionDate = null;

    // --- CÂMPURI NOI PENTRU NOTIFICĂRI ---
    @JsonIgnore
    private boolean notifiedDueTomorrow = false;
    @JsonIgnore
    private boolean notifiedUnblockedAfterDue = false;

    public Milestone(String name, String createdBy, LocalDate createdAt, LocalDate dueDate,
                     List<String> blockingFor, List<Integer> ticketIds, List<String> assignedDevs) {
        this.name = name;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.dueDate = dueDate;
        this.blockingFor = blockingFor;
        this.ticketIds = ticketIds;
        this.assignedDevs = assignedDevs;
    }

    public String getName() { return name; }
    public String getCreatedBy() { return createdBy; }
    public LocalDate getCreatedAt() { return createdAt; }
    public LocalDate getDueDate() { return dueDate; }
    public List<String> getBlockingFor() { return blockingFor; }
    public List<Integer> getTicketIds() { return ticketIds; }
    public List<String> getAssignedDevs() { return assignedDevs; }

    // Status: ACTIVE dacă există tichete care nu sunt CLOSED
    public String getStatus(List<Ticket> allTickets) {
        if (ticketIds.isEmpty()) return "COMPLETED";

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

    public double getCompletionPercentage(List<Ticket> allTickets) {
        if (ticketIds.isEmpty()) return 0.00;
        long closedCount = ticketIds.stream()
                .map(id -> findTicketById(allTickets, id))
                .filter(t -> t != null && "CLOSED".equals(t.getStatus()))
                .count();
        return (double) closedCount / ticketIds.size();
    }

    public List<Integer> getOpenTickets(List<Ticket> allTickets) {
        List<Integer> open = new ArrayList<>();
        for (Integer id : ticketIds) {
            Ticket t = findTicketById(allTickets, id);
            if (t != null && !"CLOSED".equals(t.getStatus())) {
                open.add(id);
            }
        }
        return open;
    }

    public List<Integer> getClosedTickets(List<Ticket> allTickets) {
        List<Integer> closed = new ArrayList<>();
        for (Integer id : ticketIds) {
            Ticket t = findTicketById(allTickets, id);
            if (t != null && "CLOSED".equals(t.getStatus())) {
                closed.add(id);
            }
        }
        return closed;
    }

    public long getDaysUntilDue(LocalDate currentDay, List<Ticket> allTickets) {
        updateCompletionDateIfFinished(allTickets, currentDay);
        LocalDate refDate = (completionDate != null) ? completionDate : currentDay;

        // Dacă e trecut de due date, returnează 0
        if (refDate.isAfter(dueDate)) return 0;

        // Formula: dueDate - currentDay + 1
        return ChronoUnit.DAYS.between(refDate, dueDate) + 1;
    }

    public long getOverdueBy(LocalDate currentDay, List<Ticket> allTickets) {
        updateCompletionDateIfFinished(allTickets, currentDay);
        LocalDate refDate = (completionDate != null) ? completionDate : currentDay;

        if (!refDate.isAfter(dueDate)) return 0;
        return ChronoUnit.DAYS.between(dueDate, refDate);
    }

    private void updateCompletionDateIfFinished(List<Ticket> allTickets, LocalDate currentDay) {
        if ("COMPLETED".equals(getStatus(allTickets)) && completionDate == null) {
            this.completionDate = currentDay;
        }
    }

    private Ticket findTicketById(List<Ticket> allTickets, int id) {
        return allTickets.stream().filter(t -> t.getId() == id).findFirst().orElse(null);
    }

    // --- MODIFICARE: APPLY RULES CU NOTIFICĂRI ---
    public void applyRules(Database db, LocalDate currentDay) {

        // 1. Verificăm dacă suntem BLOCAȚI
        if (isBlocked(db)) {
            return;
        }

        // --- REGULA NOTIFICARE: Unblocked AFTER Due Date ---
        // MODIFICARE: Verificăm mai întâi dacă milestone-ul este DEPENDENT (dacă poate fi blocat de altele).
        // Dacă nu apare în 'blockingFor' al nimănui, înseamnă că e independent și nu trimitem notificare de "unblocked".

        boolean isDependent = false;
        for (Milestone m : db.getMilestones()) {
            if (m != this && m.getBlockingFor() != null && m.getBlockingFor().contains(this.name)) {
                isDependent = true;
                break;
            }
        }

        // Aplicăm notificarea doar dacă este dependent
        if (isDependent && currentDay.isAfter(this.dueDate) && !notifiedUnblockedAfterDue) {
            String msg = "Milestone " + this.name + " was unblocked after due date. All active tickets are now CRITICAL.";
            sendNotificationToDevs(db, msg);
            notifiedUnblockedAfterDue = true;

            setAllTicketsCritical(db);
        }

        // --- CALCUL ZILE RĂMASE ---
        long daysUntilDue = ChronoUnit.DAYS.between(currentDay, this.dueDate) + 1;
        boolean isCriticalTime = (daysUntilDue <= 2);

        if (isCriticalTime) {
            // --- REGULA NOTIFICARE: Due Tomorrow ---
            if (daysUntilDue == 2 && !notifiedDueTomorrow) {
                String msg = "Milestone " + this.name + " is due tomorrow. All unresolved tickets are now CRITICAL.";
                sendNotificationToDevs(db, msg);
                notifiedDueTomorrow = true;
            }

            setAllTicketsCritical(db);
        } else {
            // 3. Regula de 3 zile (Se aplică doar dacă NU suntem în zona critică)
            apply3DayBump(db, currentDay);
        }
    }


    // --- Helper pentru a seta toate tichetele active pe CRITICAL ---
    private void setAllTicketsCritical(Database db) {
        for (Integer id : this.ticketIds) {
            Ticket t = db.getTicket(id);
            if (t != null && !"CLOSED".equals(t.getStatus())) {
                t.setForcePriority(main.model.Priority.CRITICAL);
            }
        }
    }

    // --- Helper pentru trimiterea notificărilor ---
    private void sendNotificationToDevs(Database db, String msg) {
        if (this.assignedDevs == null) return;

        for (String username : this.assignedDevs) {
            User u = db.findUserByUsername(username);
            // Verificăm dacă userul este Developer (și nu Manager/Reporter) pentru a avea metoda addNotification
            if (u instanceof Developer) {
                ((Developer) u).addNotification(msg);
            }
        }
    }

    // Helper intern: Calculează creșterea la fiecare 3 zile
    private void apply3DayBump(Database db, LocalDate currentDay) {
        // Câte zile au trecut de la creare?
        long daysActive = ChronoUnit.DAYS.between(this.createdAt, currentDay);

        // Câte trepte urcăm? (ex: 7 zile active / 3 = 2 trepte)
        int bumps = (int) (daysActive / 3);

        if (bumps > 0) {
            for (Integer id : this.ticketIds) {
                Ticket t = db.getTicket(id);
                if (t != null && !"CLOSED".equals(t.getStatus())) {

                    // Calculăm noua prioritate pornind de la cea INIȚIALĂ
                    main.model.Priority base = t.getInitialPriority();
                    main.model.Priority target = base;

                    for (int i = 0; i < bumps; i++) {
                        target = target.next(); // Urcă o treaptă
                    }

                    // Actualizăm tichetul
                    t.setForcePriority(target);
                }
            }
        }
    }

    // Helper intern: Verifică dacă acest milestone este blocat de altul
    private boolean isBlocked(Database db) {
        for (Milestone other : db.getMilestones()) {
            if (other == this) continue;

            // Verificăm dacă 'other' blochează milestone-ul curent (this)
            if (other.getBlockingFor() != null && other.getBlockingFor().contains(this.name)) {

                // Verificăm dacă 'other' este încă ACTIV (are tichete deschise)
                boolean otherIsActive = false;
                for (Integer tid : other.getTicketIds()) {
                    Ticket t = db.getTicket(tid);
                    if (t != null && !"CLOSED".equals(t.getStatus())) {
                        otherIsActive = true;
                        break;
                    }
                }

                if (otherIsActive) return true; // Suntem blocați
            }
        }
        return false;
    }
}