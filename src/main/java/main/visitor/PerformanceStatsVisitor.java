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
public class PerformanceStatsVisitor implements Visitor {
    // Counters for types (needed for Junior - Diversity)
    private int bugCount = 0;
    private int featureCount = 0;
    private int uiCount = 0;

    // General counters
    private int highPriorityCount = 0; // HIGH or CRITICAL
    private int closedTicketsCount = 0;
    private long totalResolutionDays = 0;

    // --- VISIT METHODS ---

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

        // 1. Priority Check (HIGH/CRITICAL)
        if (t.getPriority() == Priority.HIGH || t.getPriority() == Priority.CRITICAL) {
            highPriorityCount++;
        }

        // 2. Resolution Days Calculation (AssignedAt -> SolvedAt)
        // Formula: assignedAt - solvedAt + 1 (inclusive)
        if (t.getAssignedAt() != null && t.getSolvedAt() != null) {
            LocalDate start = LocalDate.parse(t.getAssignedAt());
            LocalDate end = LocalDate.parse(t.getSolvedAt());
            // Math.abs for safety, +1 according to requirements
            long days = Math.abs(ChronoUnit.DAYS.between(start, end)) + 1;
            totalResolutionDays += days;
        }
    }

    // --- GETTERS ---

    public int getBugCount() {
        return bugCount;
    }

    public int getFeatureCount() {
        return featureCount;
    }

    public int getUiCount() {
        return uiCount;
    }

    public int getHighPriorityCount() {
        return highPriorityCount;
    }

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