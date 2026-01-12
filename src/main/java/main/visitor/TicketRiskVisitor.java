package main.visitor;

import main.model.ticket.Bug;
import main.model.ticket.FeatureRequest;
import main.model.ticket.UIFeedback;

import java.util.HashMap;
import java.util.Map;

/**
 * Visitor implementation to calculate Ticket Risk metrics.
 */
public class TicketRiskVisitor implements Visitor {
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
        double finalRisk = (rawRisk * 100.0) / 12.0;

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
        double finalRisk = (rawRisk * 100.0) / 20.0;

        accumulate("FEATURE_REQUEST", finalRisk);
    }

    // --- UI FEEDBACK ---
    // Formula: (11 - usabilityScore) * businessValue
    // Final: (Raw * 100) / 100 (i.e., Raw)
    @Override
    public void visit(final UIFeedback ui) {
        int businessValue = getBusinessValue(ui.getBusinessValue());
        int usability = ui.getUsabilityScore();

        double rawRisk = (11 - usability) * businessValue;
        double finalRisk = (rawRisk * 100.0) / 100.0;

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
        // Round for safety, although intervals are wide
        // avgRisk = Math.round(avgRisk * 100.0) / 100.0;

        if (avgRisk < 25) {
            return "NEGLIGIBLE";
        }
        if (avgRisk < 50) {
            return "MODERATE";
        }
        if (avgRisk < 75) {
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

    // --- HELPER METHODS FOR VALUES (If Enums with .getValue() are not implemented) ---

    private int getSeverityValue(final String s) {
        if (s == null) {
            return 1;
        }
        return switch (s.toUpperCase()) {
            case "MINOR" -> 1;
            case "MODERATE" -> 2;
            case "SEVERE" -> 3;
            default -> 1;
        };
    }

    private int getFrequencyValue(final String f) {
        if (f == null) {
            return 1;
        }
        return switch (f.toUpperCase()) {
            case "RARE" -> 1;
            case "OCCASIONAL" -> 2;
            case "FREQUENT" -> 3;
            case "ALWAYS" -> 4;
            default -> 1;
        };
    }

    private int getBusinessValue(final String bv) {
        if (bv == null) {
            return 1;
        }
        return switch (bv.toUpperCase()) {
            case "S" -> 1;
            case "M" -> 3;
            case "L" -> 6;
            case "XL" -> 10;
            default -> 1;
        };
    }

    private int getCustomerDemandValue(final String cd) {
        if (cd == null) {
            return 1;
        }
        return switch (cd.toUpperCase()) {
            case "LOW" -> 1;
            case "MEDIUM" -> 3;
            case "HIGH" -> 6;
            case "VERY_HIGH" -> 10;
            default -> 1;
        };
    }
}