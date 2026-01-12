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

/**
 * Command to view milestones with status and statistics.
 */
public final class ViewMilestonesCommand implements Command {
    private static final double ROUNDING_FACTOR = 100.0;

    private final Database db;
    private final InputData input;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Constructor for ViewMilestonesCommand.
     *
     * @param db    The database instance.
     * @param input The input data.
     */
    public ViewMilestonesCommand(final Database db, final InputData input) {
        this.db = db;
        this.input = input;
    }

    @Override
    public void execute(final List<ObjectNode> outputs) {
        String username = input.getUsername();
        LocalDate currentDay = LocalDate.parse(input.getTimestamp());
        String role = db.getUserRole(username);

        // 1. Filter
        List<Milestone> filtered = db.getMilestones().stream()
                .filter(m -> {
                    if ("MANAGER".equalsIgnoreCase(role)) {
                        return m.getCreatedBy().equals(username);
                    } else {
                        return m.getAssignedDevs().contains(username);
                    }
                })
                .collect(Collectors.toList());

        // 2. Sort
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
            if (m.getBlockingFor() != null) {
                m.getBlockingFor().forEach(bNode::add);
            }

            mNode.put("dueDate", m.getDueDate().toString());
            mNode.put("createdAt", m.getCreatedAt().toString());

            ArrayNode tIds = mNode.putArray("tickets");
            m.getTicketIds().forEach(tIds::add);

            ArrayNode devs = mNode.putArray("assignedDevs");
            m.getAssignedDevs().forEach(devs::add);

            mNode.put("createdBy", m.getCreatedBy());

            // --- Logic Extraction ---
            List<Ticket> milestoneTicketsObjects = new ArrayList<>();
            List<Integer> openTickets = new ArrayList<>();
            List<Integer> closedTickets = new ArrayList<>();

            String status = processTickets(m, milestoneTicketsObjects, openTickets, closedTickets);
            mNode.put("status", status);

            boolean isBlocked = checkIsBlocked(m);
            mNode.put("isBlocked", isBlocked);

            calculateDates(m, mNode, status, milestoneTicketsObjects, currentDay);

            // Lists & Percentage
            ArrayNode openArr = mNode.putArray("openTickets");
            openTickets.forEach(openArr::add);
            ArrayNode closedArr = mNode.putArray("closedTickets");
            closedTickets.forEach(closedArr::add);

            double pct = m.getTicketIds().isEmpty() ? 0.0
                    : (double) closedTickets.size() / m.getTicketIds().size();
            mNode.put("completionPercentage", Math.round(pct * ROUNDING_FACTOR) / ROUNDING_FACTOR);

            addRepartition(m, mNode);
        }
        outputs.add(root);
    }

    private String processTickets(final Milestone m, final List<Ticket> ticketObjects,
                                  final List<Integer> open, final List<Integer> closed) {
        boolean allClosed = true;
        for (Integer tid : m.getTicketIds()) {
            Ticket t = db.getTicket(tid);
            if (t != null) {
                ticketObjects.add(t);
                if ("CLOSED".equals(t.getStatus())) {
                    closed.add(tid);
                } else {
                    allClosed = false;
                    open.add(tid);
                }
            }
        }
        return (m.getTicketIds().isEmpty() || !allClosed) ? "ACTIVE" : "COMPLETED";
    }

    private boolean checkIsBlocked(final Milestone m) {
        for (Milestone other : db.getMilestones()) {
            if (other == m) {
                continue;
            }
            if (other.getBlockingFor() != null
                    && other.getBlockingFor().contains(m.getName())) {
                boolean otherIsActive = false;
                for (Integer tid : other.getTicketIds()) {
                    Ticket t = db.getTicket(tid);
                    if (t == null || !"CLOSED".equals(t.getStatus())) {
                        otherIsActive = true;
                        break;
                    }
                }
                if (otherIsActive) {
                    return true;
                }
            }
        }
        return false;
    }

    private void calculateDates(final Milestone m, final ObjectNode mNode, final String status,
                                final List<Ticket> tickets, final LocalDate currentDay) {
        LocalDate calculationDate = currentDay;

        if ("COMPLETED".equals(status) && !tickets.isEmpty()) {
            LocalDate maxClosedDate = null;
            for (Ticket t : tickets) {
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

        long diff = ChronoUnit.DAYS.between(calculationDate, m.getDueDate());
        long daysUntil = 0;
        long overdueBy = 0;

        if (diff >= 0) {
            daysUntil = diff + 1;
        } else {
            overdueBy = Math.abs(diff) + 1;
        }

        mNode.put("daysUntilDue", daysUntil);
        mNode.put("overdueBy", overdueBy);
    }

    private void addRepartition(final Milestone m, final ObjectNode mNode) {
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
            if (s1 != s2) {
                return s1 - s2;
            }
            return a.get("developer").asText().compareTo(b.get("developer").asText());
        });

        devRepList.forEach(repArr::add);
    }
}
