package main.visitor;

import main.model.Priority;
import main.model.ticket.Bug;
import main.model.ticket.FeatureRequest;
import main.model.ticket.Ticket;
import main.model.ticket.UIFeedback;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Visitor implementation to collect performance statistics from tickets.
 */
public final class PerformanceStatsVisitor implements Visitor {

    private int bugCount = 0;
    private int featureCount = 0;
    private int uiCount = 0;

    // General counters
    private int highPriorityCount = 0; // HIGH or CRITICAL
    private int closedTicketsCount = 0;
    private long totalResolutionDays = 0;

    // VISIT METHODS Implementation

    @Override
    public void visit(final Bug bug) {
        bugCount++;
        processCommon(bug);
    }

    @Override
    public void visit(final FeatureRequest fr) {
        featureCount++;
        processCommon(fr);
    }

    @Override
    public void visit(final UIFeedback ui) {
        uiCount++;
        processCommon(ui);
    }

    // Common logic for all tickets
    private void processCommon(final Ticket t) {
        closedTicketsCount++;

        // 1. High/Critical check
        if (t.getPriority() == Priority.HIGH || t.getPriority() == Priority.CRITICAL) {
            highPriorityCount++;
        }

        // 2. Resolution days calculation
        if (t.getAssignedAt() == null || t.getAssignedAt().isEmpty()) {
            return;
        }

        LocalDate start = LocalDate.parse(t.getAssignedAt());
        String solvedDateStr = t.getSolvedAt(); // Default to current solvedAt

        if (t.getHistory() != null) {
            for (Ticket.HistoryEntry entry : t.getHistory()) {
                System.out.println("History Entry Action: " + entry.getAction() +
                        ", From: " + entry.getFrom() + ", To: " + entry.getTo() +
                        ", Timestamp: " + entry.getTimestamp());
                if (entry.getTo() != null && "CLOSED".equalsIgnoreCase(entry.getTo())) {
                    LocalDate entryDate = LocalDate.parse(entry.getTimestamp());

                    if (!entryDate.isBefore(start)) {
                        solvedDateStr = entry.getTimestamp();
                    }
                }
            }
        }

        if (solvedDateStr != null && !solvedDateStr.isEmpty()) {
            LocalDate end = LocalDate.parse(solvedDateStr);
            // Formula: assignedAt - solvedAt + 1 (inclusive)
            long days = Math.abs(ChronoUnit.DAYS.between(start, end)) + 1;
            totalResolutionDays += days;
        }
    }

    // --- GETTERS ---

    /**
     * Gets the count of bug tickets visited.
     *
     * @return The number of bug tickets.
     */
    public int getBugCount() {
        return bugCount;
    }

    /**
     * Gets the count of feature request tickets visited.
     *
     * @return The number of feature request tickets.
     */
    public int getFeatureCount() {
        return featureCount;
    }

    /**
     * Gets the count of UI feedback tickets visited.
     *
     * @return The number of UI feedback tickets.
     */
    public int getUiCount() {
        return uiCount;
    }

    /**
     * Gets the count of tickets with HIGH or CRITICAL priority.
     *
     * @return The high priority ticket count.
     */
    public int getHighPriorityCount() {
        return highPriorityCount;
    }

    /**
     * Gets the total number of closed tickets processed.
     *
     * @return The closed tickets count.
     */
    public int getClosedTicketsCount() {
        return closedTicketsCount;
    }

    /**
     * Calculates the average resolution time in days.
     *
     * @return The average resolution time.
     */
    public double getAverageResolutionTime() {
        if (closedTicketsCount == 0) {
            return 0.0;
        }
        return (double) totalResolutionDays / closedTicketsCount;
    }
}