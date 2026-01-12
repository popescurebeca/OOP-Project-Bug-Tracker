package main.visitor;

import main.model.ticket.Bug;
import main.model.ticket.FeatureRequest;
import main.model.ticket.Ticket;
import main.model.ticket.UIFeedback;
import main.model.ticket.enums.BusinessValue;
import main.model.ticket.enums.CustomerDemand;
import main.model.ticket.enums.Frequency;
import main.model.ticket.enums.Severity;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Visitor implementation to calculate Resolution Efficiency metrics.
 */
public final class ResolutionEfficiencyVisitor implements Visitor {
    // --- Constants for Calculations ---
    private static final double BUG_SCORE_MULTIPLIER = 10.0;
    private static final double PERCENTAGE_MULTIPLIER = 100.0;
    private static final double ROUNDING_FACTOR = 100.0;

    // --- Constants for Max Scores ---
    private static final double BUG_MAX_SCORE = 70.0;
    private static final double FEATURE_MAX_SCORE = 20.0;
    private static final double UI_MAX_SCORE = 20.0;

    private final Map<String, Double> totalEfficiency = new HashMap<>();
    private final Map<String, Integer> countByType = new HashMap<>();

    /**
     * Constructor for ResolutionEfficiencyVisitor.
     */
    public ResolutionEfficiencyVisitor() {
        totalEfficiency.put("BUG", 0.0);
        totalEfficiency.put("FEATURE_REQUEST", 0.0);
        totalEfficiency.put("UI_FEEDBACK", 0.0);

        countByType.put("BUG", 0);
        countByType.put("FEATURE_REQUEST", 0);
        countByType.put("UI_FEEDBACK", 0);
    }

    // Helper for calculating days (daysToResolve)
    private long getDaysToResolve(final Ticket t) {
        if (t.getAssignedAt() == null || t.getSolvedAt() == null) {
            return 1; // Default safe
        }
        LocalDate start = LocalDate.parse(t.getAssignedAt());
        LocalDate end = LocalDate.parse(t.getSolvedAt());

        // Formula: day difference + 1 (to include current day/avoid division by 0)
        // Ex: Assigned today, Resolved today -> Diff=0 -> Return 1
        // Ex: Assigned today, Resolved tomorrow -> Diff=1 -> Return 2
        return ChronoUnit.DAYS.between(start, end) + 1;
    }

    // --- BUG ---
    // Formula: (frequency + severityFactor) * 10 / daysToResolve
    // Final: (Score * 100) / 70
    @Override
    public void visit(final Bug bug) {
        long days = getDaysToResolve(bug);
        int freq = Frequency.valueOf(bug.getFrequency().toUpperCase()).getValue();
        int sev = Severity.valueOf(bug.getSeverity().toUpperCase()).getValue();

        double score = (double) (freq + sev) * BUG_SCORE_MULTIPLIER / days;
        double finalEff = (score * PERCENTAGE_MULTIPLIER) / BUG_MAX_SCORE;

        accumulate("BUG", finalEff);
    }

    // --- FEATURE REQUEST ---
    // Formula: (businessValue + customerDemand) / daysToResolve
    // Final: (Score * 100) / 20
    @Override
    public void visit(final FeatureRequest fr) {
        long days = getDaysToResolve(fr);
        int bv = BusinessValue.valueOf(fr.getBusinessValue().toUpperCase()).getValue();
        int demand = CustomerDemand.valueOf(fr.getCustomerDemand().toUpperCase()).getValue();

        double score = (double) (bv + demand) / days;
        double finalEff = (score * PERCENTAGE_MULTIPLIER) / FEATURE_MAX_SCORE;

        accumulate("FEATURE_REQUEST", finalEff);
    }

    // --- UI FEEDBACK ---
    // Formula: (usabilityScore + businessValue) / daysToResolve
    // Final: (Score * 100) / 20
    @Override
    public void visit(final UIFeedback ui) {
        long days = getDaysToResolve(ui);
        int bv = BusinessValue.valueOf(ui.getBusinessValue().toUpperCase()).getValue();
        int usability = ui.getUsabilityScore();

        double score = (double) (usability + bv) / days;
        double finalEff = (score * PERCENTAGE_MULTIPLIER) / UI_MAX_SCORE;

        accumulate("UI_FEEDBACK", finalEff);
    }

    private void accumulate(final String type, final double value) {
        totalEfficiency.put(type, totalEfficiency.get(type) + value);
        countByType.put(type, countByType.get(type) + 1);
    }

    /**
     * Calculates the average efficiency for a given ticket type.
     *
     * @param type The type of the ticket.
     * @return The average efficiency score.
     */
    public double getAverageEfficiency(final String type) {
        int count = countByType.getOrDefault(type, 0);
        if (count == 0) {
            return 0.0;
        }

        double avg = totalEfficiency.get(type) / count;
        // Round to 2 decimal places (example: 35.714 -> 35.71)
        return Math.round(avg * ROUNDING_FACTOR) / ROUNDING_FACTOR;
    }

    /**
     * Gets the count of tickets for a given type.
     *
     * @param type The type of the ticket.
     * @return The count of tickets.
     */
    public int getCount(final String type) {
        return countByType.getOrDefault(type, 0);
    }
}
