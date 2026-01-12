package main.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import main.database.Database;
import main.model.Milestone;
import main.model.Priority;
import main.model.ticket.Ticket;
import main.model.user.Developer;
import main.model.user.User;
import main.model.user.Manager;
import main.model.user.enums.Seniority;
import main.utils.InputData;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Command to search for tickets or developers based on filters.
 */
public class SearchCommand implements Command {
    private final Database db;
    private final InputData input;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Constructor for SearchCommand.
     *
     * @param db    The database instance.
     * @param input The input data containing command parameters.
     */
    public SearchCommand(final Database db, final InputData input) {
        this.db = db;
        this.input = input;
    }

    /**
     * Executes the search command.
     *
     * @param outputs The list of outputs to append results to.
     */
    @Override
    public void execute(final List<ObjectNode> outputs) {
        String username = input.getUsername();
        Map<String, Object> filters = input.getFilters();
        if (filters == null) {
            filters = new HashMap<>();
        }

        User user = db.findUserByUsername(username);
        if (user == null) {
            return;
        }

        String searchType = (String) filters.getOrDefault("searchType", "TICKET");

        // Build basic output structure
        ObjectNode result = mapper.createObjectNode();
        result.put("command", "search");
        result.put("username", username);
        result.put("timestamp", input.getTimestamp());
        result.put("searchType", searchType);
        ArrayNode resultsArray = result.putArray("results");

        if ("TICKET".equals(searchType)) {
            searchTickets(user, filters, resultsArray);
        } else if ("DEVELOPER".equals(searchType)) {
            // Only managers can search for developers
            if ("MANAGER".equalsIgnoreCase(user.getRole())) {
                searchDevelopers((Manager) user, filters, resultsArray);
            }
        }

        outputs.add(result);
    }

    // ==========================================
    // TICKET SEARCH LOGIC
    // ==========================================
    private void searchTickets(final User user, final Map<String, Object> filters,
                               final ArrayNode resultsArray) {
        List<Ticket> matches = new ArrayList<>();
        List<Ticket> allTickets = db.getTickets();

        for (Ticket t : allTickets) {
            // 1. BASIC FILTERING (VISIBILITY)
            if (!canUserSeeTicket(user, t)) {
                continue;
            }

            // 2. APPLY INPUT FILTERS
            if (!matchesFilters(t, user, filters)) {
                continue;
            }

            matches.add(t);
        }

        // 3. SORTING: createdAt ASC, then ID ASC
        matches.sort((t1, t2) -> {
            int dateComp = t1.getCreatedAt().compareTo(t2.getCreatedAt());
            if (dateComp != 0) {
                return dateComp;
            }
            return Integer.compare(t1.getId(), t2.getId());
        });

        // 4. JSON GENERATION
        for (Ticket t : matches) {
            ObjectNode tNode = mapper.createObjectNode();
            tNode.put("id", t.getId());
            tNode.put("type", t.getType());
            tNode.put("title", t.getTitle());
            tNode.put("businessPriority", t.getPriority().name());
            tNode.put("status", t.getStatus());
            tNode.put("createdAt", t.getCreatedAt());
            tNode.put("solvedAt", t.getSolvedAt() != null ? t.getSolvedAt() : "");
            tNode.put("reportedBy", t.getReportedBy());

            // Add matchingWords if keywords filter was used
            if (filters.containsKey("keywords")) {
                List<String> keywords = (List<String>) filters.get("keywords");
                List<String> found = getMatchingWords(t, keywords);

                ArrayNode matchNode = tNode.putArray("matchingWords");
                found.forEach(matchNode::add);
            }
            resultsArray.add(tNode);
        }
    }

    // Checks basic visibility (without explicit filters)
    private boolean canUserSeeTicket(final User user, final Ticket t) {
        String role = user.getRole().toUpperCase();

        if ("MANAGER".equals(role)) {
            return true; // Manager sees everything
        }

        if ("DEVELOPER".equals(role) || "EMPLOYEE".equals(role)) {
            // Developer sees only OPEN tickets from their milestones
            if (!"OPEN".equals(t.getStatus())) {
                return false;
            }

            Milestone m = db.findMilestoneByTicketId(t.getId());
            return m != null && m.getAssignedDevs().contains(user.getUsername());
        }

        return false;
    }

    // Checks all dynamic filters
    private boolean matchesFilters(final Ticket t, final User user,
                                   final Map<String, Object> filters) {
        // Filter: Business Priority
        if (filters.containsKey("businessPriority")) {
            String val = (String) filters.get("businessPriority");
            if (!t.getPriority().name().equalsIgnoreCase(val)) {
                return false;
            }
        }

        // Filter: Type
        if (filters.containsKey("type")) {
            String val = (String) filters.get("type");
            if (!t.getType().equalsIgnoreCase(val)) {
                return false;
            }
        }

        // Filters: Date
        LocalDate ticketDate = LocalDate.parse(t.getCreatedAt());
        if (filters.containsKey("createdAfter")) {
            LocalDate after = LocalDate.parse((String) filters.get("createdAfter"));
            if (!ticketDate.isAfter(after)) {
                return false;
            }
        }
        if (filters.containsKey("createdBefore")) {
            LocalDate before = LocalDate.parse((String) filters.get("createdBefore"));
            if (!ticketDate.isBefore(before)) {
                return false;
            }
        }

        // Filter: Keywords
        if (filters.containsKey("keywords")) {
            List<String> keywords = (List<String>) filters.get("keywords");
            if (getMatchingWords(t, keywords).isEmpty()) {
                return false;
            }
        }

        // Filter: AvailableForAssignment (Complex)
        if (filters.containsKey("availableForAssignment")
                && (Boolean) filters.get("availableForAssignment")) {
            if (!isAvailableForAssignment(t, user)) {
                return false;
            }
        }

        return true;
    }

    // Logic for matchingWords
    private List<String> getMatchingWords(final Ticket t, final List<String> keywords) {
        List<String> found = new ArrayList<>();
        // Search in title and description (concatenated)
        String content = (t.getTitle() + " " + t.getDescription()).toLowerCase();

        for (String k : keywords) {
            if (content.contains(k.toLowerCase())) {
                found.add(k);
            }
        }
        Collections.sort(found); // Lexicographical
        return found;
    }

    // Logic for AvailableForAssignment (Repeats logic from AssignTicket)
    private boolean isAvailableForAssignment(final Ticket t, final User u) {
        if (!(u instanceof Developer)) {
            return false;
        }
        Developer dev = (Developer) u;

        // 1. Status
        if (!"OPEN".equals(t.getStatus())) {
            return false;
        }

        // 2. Milestone Access & Blocked
        Milestone m = db.findMilestoneByTicketId(t.getId());
        if (m == null || !m.getAssignedDevs().contains(u.getUsername())) {
            return false;
        }
        if (isMilestoneBlocked(m)) {
            return false;
        }

        // 3. Expertise
        String ticketArea = t.getExpertiseArea();
        String userExpertise = (dev.getExpertise() != null) ? dev.getExpertise().name() : "";

        boolean expMatch = "FULLSTACK".equals(userExpertise) || userExpertise.equals(ticketArea);
        if ("DB".equals(ticketArea) && "BACKEND".equals(userExpertise)) {
            expMatch = true;
        }
        if ("BACKEND".equals(ticketArea) && "DB".equals(userExpertise)) {
            expMatch = true;
        }

        if (!expMatch) {
            return false;
        }

        // 4. Seniority
        Priority p = t.getPriority();
        Seniority s = dev.getSeniority();
        if ((p == Priority.CRITICAL || p == Priority.HIGH) && s == Seniority.JUNIOR) {
            return false;
        }

        return true;
    }

    private boolean isMilestoneBlocked(final Milestone m) {
        for (Milestone other : db.getMilestones()) {
            if (other == m) {
                continue;
            }
            if (other.getBlockingFor() != null && other.getBlockingFor().contains(m.getName())) {
                boolean otherIsActive = other.getTicketIds().stream()
                        .map(db::getTicket)
                        .anyMatch(ti -> ti != null && !"CLOSED".equals(ti.getStatus()));
                if (otherIsActive) {
                    return true;
                }
            }
        }
        return false;
    }

    // ==========================================
    // DEVELOPER SEARCH LOGIC (Manager Only)
    // ==========================================
    private void searchDevelopers(final Manager manager, final Map<String, Object> filters,
                                  final ArrayNode resultsArray) {
        List<Developer> matches = new ArrayList<>();

        // Search only in subordinates
        List<String> subs = manager.getSubordinates();
        if (subs == null) {
            subs = new ArrayList<>();
        }

        for (String subName : subs) {
            User u = db.findUserByUsername(subName);
            if (u instanceof Developer) {
                Developer dev = (Developer) u;

                // Apply filters
                if (matchesDevFilters(dev, filters)) {
                    matches.add(dev);
                }
            }
        }

        // Sort: Username lexicographical
        matches.sort(Comparator.comparing(Developer::getUsername));

        // Generate JSON
        for (Developer d : matches) {
            ObjectNode dNode = mapper.createObjectNode();
            dNode.put("username", d.getUsername());
            dNode.put("expertiseArea",
                    (d.getExpertise() != null) ? d.getExpertise().name() : null);
            dNode.put("seniority",
                    (d.getSeniority() != null) ? d.getSeniority().name() : null);
            dNode.put("performanceScore", d.getPerformanceScore());
            dNode.put("hireDate", d.getHireDate().toString());

            resultsArray.add(dNode);
        }
    }

    private boolean matchesDevFilters(final Developer d, final Map<String, Object> filters) {
        // Expertise
        if (filters.containsKey("expertiseArea")) {
            String val = (String) filters.get("expertiseArea");
            if (d.getExpertise() == null
                    || !d.getExpertise().name().equalsIgnoreCase(val)) {
                return false;
            }
        }

        // Seniority
        if (filters.containsKey("seniority")) {
            String val = (String) filters.get("seniority");
            if (d.getSeniority() == null
                    || !d.getSeniority().name().equalsIgnoreCase(val)) {
                return false;
            }
        }

        // Performance Score
        if (filters.containsKey("performanceScoreAbove")) {
            // Jackson reads numbers as Integer or Double
            Number val = (Number) filters.get("performanceScoreAbove");
            if (d.getPerformanceScore() <= val.doubleValue()) {
                return false;
            }
        }

        if (filters.containsKey("performanceScoreBelow")) {
            Number val = (Number) filters.get("performanceScoreBelow");
            if (d.getPerformanceScore() >= val.doubleValue()) {
                return false;
            }
        }

        return true;
    }
}