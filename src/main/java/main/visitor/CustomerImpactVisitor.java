package main.visitor;

import main.model.ticket.Bug;
import main.model.ticket.FeatureRequest;
import main.model.ticket.UIFeedback;

import java.util.HashMap;
import java.util.Map;

/**
 * Visitor implementation to calculate Customer Impact metrics.
 */
public final class CustomerImpactVisitor implements Visitor {
    // --- Constants for Calculations ---
    private static final double PERCENTAGE_MULTIPLIER = 100.0;
    private static final double BUG_MAX_SCORE = 48.0;

    // --- Constants for Values ---
    private static final int VAL_1 = 1;
    private static final int VAL_2 = 2;
    private static final int VAL_3 = 3;
    private static final int VAL_4 = 4;
    private static final int VAL_6 = 6;
    private static final int VAL_10 = 10;

    // Structures to store results
    private final Map<String, Double> totalImpactByType = new HashMap<>();
    private final Map<String, Integer> countByType = new HashMap<>();

    /**
     * Constructor for CustomerImpactVisitor.
     * Initializes counters and totals.
     */
    public CustomerImpactVisitor() {
        totalImpactByType.put("BUG", 0.0);
        totalImpactByType.put("FEATURE_REQUEST", 0.0);
        totalImpactByType.put("UI_FEEDBACK", 0.0);

        countByType.put("BUG", 0);
        countByType.put("FEATURE_REQUEST", 0);
        countByType.put("UI_FEEDBACK", 0);
    }

    // --- VISIT BUG ---
    @Override
    public void visit(final Bug bug) {
        double impact = calculateBugImpact(bug);
        accumulate("BUG", impact);
    }

    // --- VISIT FEATURE REQUEST ---
    @Override
    public void visit(final FeatureRequest fr) {
        double impact = calculateFeatureImpact(fr);
        accumulate("FEATURE_REQUEST", impact);
    }

    // --- VISIT UI FEEDBACK ---
    @Override
    public void visit(final UIFeedback ui) {
        double impact = calculateUiImpact(ui);
        accumulate("UI_FEEDBACK", impact);
    }

    // --- CALCULATION LOGIC (Moved from Command) ---

    private void accumulate(final String type, final double impact) {
        totalImpactByType.put(type, totalImpactByType.get(type) + impact);
        countByType.put(type, countByType.get(type) + 1);
    }

    private double calculateBugImpact(final Bug t) {
        int frequency = getFrequencyValue(t.getFrequency());
        int priority = getPriorityValue(t.getPriority().name());
        int severity = getSeverityValue(t.getSeverity());
        // Formula: (frequency * priority * severity * 100) / 48
        return (frequency * priority * severity * PERCENTAGE_MULTIPLIER) / BUG_MAX_SCORE;
    }

    private double calculateFeatureImpact(final FeatureRequest t) {
        int businessValue = getBusinessValue(t.getBusinessValue());
        int demand = getCustomerDemandValue(t.getCustomerDemand());
        // Formula: businessValue * customerDemand
        return (double) (businessValue * demand);
    }

    private double calculateUiImpact(final UIFeedback t) {
        int businessValue = getBusinessValue(t.getBusinessValue());
        int usability = t.getUsabilityScore(); // This is already int
        // Formula: businessValue * usabilityScore
        return (double) (businessValue * usability);
    }

    // --- HELPER METHODS FOR VALUES ---

    // 1. Priority (LOW=1, MEDIUM=2, HIGH=3, CRITICAL=4)
    private int getPriorityValue(final String p) {
        if (p == null) {
            return 1; // Default
        }
        return switch (p.toUpperCase()) {
            case "LOW" -> VAL_1;
            case "MEDIUM" -> VAL_2;
            case "HIGH" -> VAL_3;
            case "CRITICAL" -> VAL_4;
            default -> 1;
        };
    }

    // 2. Severity (MINOR=1, MODERATE=2, SEVERE=3)
    private int getSeverityValue(final String s) {
        if (s == null) {
            return 1;
        }
        return switch (s.toUpperCase()) {
            case "MINOR" -> VAL_1;
            case "MODERATE" -> VAL_2;
            case "SEVERE" -> VAL_3;
            default -> 1;
        };
    }

    // 3. Frequency (RARE=1, OCCASIONAL=2, FREQUENT=3, ALWAYS=4)
    private int getFrequencyValue(final String f) {
        if (f == null) {
            return 1;
        }
        return switch (f.toUpperCase()) {
            case "RARE" -> VAL_1;
            case "OCCASIONAL" -> VAL_2;
            case "FREQUENT" -> VAL_3;
            case "ALWAYS" -> VAL_4;
            default -> 1;
        };
    }

    // 4. Business Value (S=1, M=3, L=6, XL=10)
    private int getBusinessValue(final String bv) {
        if (bv == null) {
            return 1;
        }
        return switch (bv.toUpperCase()) {
            case "S" -> VAL_1;
            case "M" -> VAL_3;
            case "L" -> VAL_6;
            case "XL" -> VAL_10;
            default -> 1;
        };
    }

    // 5. Customer Demand (LOW=1, MEDIUM=3, HIGH=6, VERY_HIGH=10)
    private int getCustomerDemandValue(final String cd) {
        if (cd == null) {
            return 1;
        }
        return switch (cd.toUpperCase()) {
            case "LOW" -> VAL_1;
            case "MEDIUM" -> VAL_3;
            case "HIGH" -> VAL_6;
            case "VERY_HIGH" -> VAL_10;
            default -> 1;
        };
    }

    // --- GETTERS FOR COMMAND ---

    /**
     * Calculates the average impact for a given ticket type.
     *
     * @param type The type of the ticket.
     * @return The average impact, rounded to 2 decimal places.
     */
    public double getAverageImpact(final String type) {
        int count = countByType.getOrDefault(type, 0);
        if (count == 0) {
            return 0.0;
        }

        double avg = totalImpactByType.get(type) / count;

        // Use Math.round for correct precision
        return Math.round(avg * PERCENTAGE_MULTIPLIER) / PERCENTAGE_MULTIPLIER;
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
