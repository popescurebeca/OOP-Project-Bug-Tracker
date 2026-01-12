package main.visitor;

import main.model.ticket.Bug;
import main.model.ticket.FeatureRequest;
import main.model.ticket.UIFeedback;

import java.util.HashMap;
import java.util.Map;

/**
 * Visitor implementation to calculate Ticket Risk metrics.
 */
public final class TicketRiskVisitor implements Visitor {
    // --- Constants for Calculations ---
    private static final double PERCENTAGE_MULTIPLIER = 100.0;
    private static final double BUG_MAX_SCORE = 12.0;
    private static final double FEATURE_MAX_SCORE = 20.0;
    private static final double UI_MAX_SCORE = 100.0;
    private static final int UI_BASE_FACTOR = 11;

    // --- Constants for Risk Thresholds ---
    private static final double LOW_RISK_THRESHOLD = 25.0;
    private static final double MODERATE_RISK_THRESHOLD = 50.0;
    private static final double SIGNIFICANT_RISK_THRESHOLD = 75.0;

    // --- Constants for Values ---
    private static final int VAL_1 = 1;
    private static final int VAL_2 = 2;
    private static final int VAL_3 = 3;
    private static final int VAL_4 = 4;
    private static final int VAL_6 = 6;
    private static final int VAL_10 = 10;

    // Store sum of risks and count of tickets for average
    private final Map<String, Double> totalRiskByType = new HashMap<>();
    private final Map<String, Integer> countByType = new HashMap<>();

    /**
     * Constructor for TicketRiskVisitor.
     */
    public TicketRiskVisitor() {
        totalRiskByType.put("BUG", 0.0);
        totalRiskByType.put("FEATURE_REQUEST", 0.0);
        totalRiskByType.put("UI_FEEDBACK", 0.0);

        countByType.put("BUG", 0);
        countByType.put("FEATURE_REQUEST", 0);
        countByType.put("UI_FEEDBACK", 0);
    }

    // --- BUG ---
    // Formula: frequency * severityFactor
    // Final: (Raw * 100) / 12
    @Override
    public void visit(final Bug bug) {
        // Use helper methods or enums if available
        int frequency = getFrequencyValue(bug.getFrequency());
        int severity = getSeverityValue(bug.getSeverity());

        double rawRisk = frequency * severity;
        double finalRisk = (rawRisk * PERCENTAGE_MULTIPLIER) / BUG_MAX_SCORE;

        accumulate("BUG", finalRisk);
    }

    // --- FEATURE REQUEST ---
    // Formula: businessValue + customerDemand
    // Final: (Raw * 100) / 20
    @Override
    public void visit(final FeatureRequest fr) {
        int businessValue = getBusinessValue(fr.getBusinessValue());
        int demand = getCustomerDemandValue(fr.getCustomerDemand());

        double rawRisk = businessValue + demand;
        double finalRisk = (rawRisk * PERCENTAGE_MULTIPLIER) / FEATURE_MAX_SCORE;

        accumulate("FEATURE_REQUEST", finalRisk);
    }

    // --- UI FEEDBACK ---
    // Formula: (11 - usabilityScore) * businessValue
    // Final: (Raw * 100) / 100 (i.e., Raw)
    @Override
    public void visit(final UIFeedback ui) {
        int businessValue = getBusinessValue(ui.getBusinessValue());
        int usability = ui.getUsabilityScore();

        double rawRisk = (UI_BASE_FACTOR - usability) * businessValue;
        double finalRisk = (rawRisk * PERCENTAGE_MULTIPLIER) / UI_MAX_SCORE;

        accumulate("UI_FEEDBACK", finalRisk);
    }

    private void accumulate(final String type, final double risk) {
        totalRiskByType.put(type, totalRiskByType.get(type) + risk);
        countByType.put(type, countByType.get(type) + 1);
    }

    /**
     * Calculates the average risk and returns a qualitative descriptor.
     *
     * @param type The type of the ticket.
     * @return The risk qualifier string (e.g., "NEGLIGIBLE", "MAJOR").
     */
    public String getRiskQualifier(final String type) {
        int count = countByType.getOrDefault(type, 0);
        if (count == 0) {
            return "NEGLIGIBLE"; // 0 risk is Negligible
        }

        double avgRisk = totalRiskByType.get(type) / count;

        if (avgRisk < LOW_RISK_THRESHOLD) {
            return "NEGLIGIBLE";
        }
        if (avgRisk < MODERATE_RISK_THRESHOLD) {
            return "MODERATE";
        }
        if (avgRisk < SIGNIFICANT_RISK_THRESHOLD) {
            return "SIGNIFICANT";
        }
        return "MAJOR";
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

    // --- HELPER METHODS FOR VALUES ---

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
}
