package main.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import main.database.Database;
import main.model.Milestone;
import main.model.ticket.Ticket;
import main.utils.InputData;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ViewMilestonesCommand implements Command {
    private final Database db;
    private final InputData input;
    private final ObjectMapper mapper = new ObjectMapper();

    public ViewMilestonesCommand(Database db, InputData input) {
        this.db = db;
        this.input = input;
    }

    @Override
    public void execute(List<ObjectNode> outputs) {
        String username = input.getUsername();
        LocalDate currentDay = LocalDate.parse(input.getTimestamp());

        // Putem șterge primul 'for' complet, era redundant.
        // Facem totul în loop-ul final după filtrare și sortare.

        String role = db.getUserRole(username);

        // 1. Filtrare
        List<Milestone> filtered = db.getMilestones().stream()
                .filter(m -> {
                    if ("MANAGER".equalsIgnoreCase(role)) {
                        return m.getCreatedBy().equals(username);
                    } else {
                        return m.getAssignedDevs().contains(username);
                    }
                })
                .collect(Collectors.toList());

        // 2. Sortare
        filtered.sort(Comparator.comparing(Milestone::getDueDate)
                .thenComparing(Milestone::getName));

        ObjectNode root = mapper.createObjectNode();
        root.put("command", "viewMilestones");
        root.put("username", username);
        root.put("timestamp", input.getTimestamp());
        ArrayNode milestonesArray = root.putArray("milestones");

        for (Milestone m : filtered) {
            ObjectNode mNode = milestonesArray.addObject();
            mNode.put("name", m.getName());

            ArrayNode bNode = mNode.putArray("blockingFor");
            if (m.getBlockingFor() != null) m.getBlockingFor().forEach(bNode::add);

            mNode.put("dueDate", m.getDueDate().toString());
            mNode.put("createdAt", m.getCreatedAt().toString());

            ArrayNode tIds = mNode.putArray("tickets");
            m.getTicketIds().forEach(tIds::add);

            ArrayNode devs = mNode.putArray("assignedDevs");
            m.getAssignedDevs().forEach(devs::add);

            mNode.put("createdBy", m.getCreatedBy());

            // --- Status & Colectare Tichete ---
            boolean allClosed = true;
            List<Integer> openTickets = new ArrayList<>();
            List<Integer> closedTickets = new ArrayList<>();
            List<Ticket> milestoneTicketsObjects = new ArrayList<>(); // Păstrăm obiectele pentru calcul dată

            for(Integer tid : m.getTicketIds()) {
                Ticket t = db.getTicket(tid);
                if (t != null) {
                    milestoneTicketsObjects.add(t);
                    if ("CLOSED".equals(t.getStatus())) {
                        closedTickets.add(tid);
                    } else {
                        allClosed = false;
                        openTickets.add(tid);
                    }
                }
            }
            String status = (m.getTicketIds().isEmpty() || !allClosed) ? "ACTIVE" : "COMPLETED";
            mNode.put("status", status);

            // --- IsBlocked Logic ---
            boolean isBlocked = false;
            for (Milestone other : db.getMilestones()) {
                if (other == m) continue;
                if (other.getBlockingFor() != null && other.getBlockingFor().contains(m.getName())) {
                    boolean otherIsActive = false;
                    for (Integer tid : other.getTicketIds()) {
                        Ticket t = db.getTicket(tid);
                        if (t == null || !"CLOSED".equals(t.getStatus())) {
                            otherIsActive = true;
                            break;
                        }
                    }
                    if (otherIsActive) {
                        isBlocked = true;
                        break;
                    }
                }
            }
            mNode.put("isBlocked", isBlocked);

            // --- Date Calculation (Calcul Data Corectă) ---

            // 1. Stabilim data de referință (Default: data curentă a comenzii)
            LocalDate calculationDate = currentDay;

            // 2. Dacă e complet, "înghețăm" data la momentul ultimei închideri
            if ("COMPLETED".equals(status) && !milestoneTicketsObjects.isEmpty()) {
                LocalDate maxClosedDate = null;
                for (Ticket t : milestoneTicketsObjects) {
                    for (Ticket.HistoryEntry h : t.getHistory()) {
                        if ("STATUS_CHANGED".equals(h.getAction()) && "CLOSED".equals(h.getTo())) {
                            LocalDate actionDate = LocalDate.parse(h.getTimestamp());
                            if (maxClosedDate == null || actionDate.isAfter(maxClosedDate)) {
                                maxClosedDate = actionDate;
                            }
                        }
                    }
                }
                if (maxClosedDate != null) {
                    calculationDate = maxClosedDate;
                }
            }

            // 3. Calculăm diferența folosind Data Corectă (calculationDate)
            long daysUntil = 0;
            long overdueBy = 0;
            long diff = ChronoUnit.DAYS.between(calculationDate, m.getDueDate());

            // between returnează negativ dacă calculationDate > dueDate (întârziat)
            // between returnează pozitiv dacă calculationDate < dueDate (în timp)

            if (diff >= 0) {
                daysUntil = diff + 1; // +1 pentru inclusiv
            } else {
                overdueBy = Math.abs(diff) + 1; // +1 pentru inclusiv
            }

            mNode.put("daysUntilDue", daysUntil);
            mNode.put("overdueBy", overdueBy);

            // Liste & Procentaj
            ArrayNode openArr = mNode.putArray("openTickets");
            openTickets.forEach(openArr::add);
            ArrayNode closedArr = mNode.putArray("closedTickets");
            closedTickets.forEach(closedArr::add);

            double pct = m.getTicketIds().isEmpty() ? 0.0 : (double) closedTickets.size() / m.getTicketIds().size();
            mNode.put("completionPercentage", Math.round(pct * 100.0) / 100.0);

            // Repartition
            ArrayNode repArr = mNode.putArray("repartition");
            List<ObjectNode> devRepList = new ArrayList<>();
            for (String dev : m.getAssignedDevs()) {
                ObjectNode devNode = mapper.createObjectNode();
                devNode.put("developer", dev);
                ArrayNode assignedT = devNode.putArray("assignedTickets");
                for (Integer tid : m.getTicketIds()) {
                    Ticket t = db.getTicket(tid);
                    if (t != null && dev.equals(t.getAssignee())) {
                        assignedT.add(tid);
                    }
                }
                devRepList.add(devNode);
            }
            devRepList.sort((a, b) -> {
                int s1 = a.get("assignedTickets").size();
                int s2 = b.get("assignedTickets").size();
                if (s1 != s2) return s1 - s2;
                return a.get("developer").asText().compareTo(b.get("developer").asText());
            });
            devRepList.forEach(repArr::add);
        }
        outputs.add(root);
    }
}