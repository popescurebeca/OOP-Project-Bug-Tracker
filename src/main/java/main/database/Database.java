package main.database;

import main.model.Milestone;
import main.model.ticket.Ticket;
import main.model.user.Developer;
import main.model.user.Manager;
import main.model.user.Reporter;
import main.model.user.User;
import main.utils.InputData;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton class representing the database of the application.
 * Stores users, tickets, and milestones.
 */
public final class Database {
    // 1. Single Instance
    private static Database instance = null;

    // 2. Data Lists
    private List<User> users;
    private List<Ticket> tickets;
    private List<Milestone> milestones;

    // Project phase flags
    private boolean testingPhaseActive;
    private boolean stopTestingPhase = false;
    private LocalDate projectStartDate = null;

    private static final int TESTING_PHASE_DAYS = 12;

    /**
     * Private Constructor.
     * Initializes empty lists and sets default testing phase to active.
     */
    private Database() {
        users = new ArrayList<>();
        tickets = new ArrayList<>();
        milestones = new ArrayList<>();
        testingPhaseActive = true; // Default starts with testing
    }

    /**
     * Resets the database state (clears all lists).
     */
    public void reset() {
        milestones.clear();
        tickets.clear();
        users.clear();
    }

    /**
     * Gets the singleton instance of the Database.
     *
     * @return The Database instance.
     */
    public static Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    /**
     * Loads users into the database from the provided input data.
     * Uses Factory logic inside or manual instantiation based on roles.
     *
     * @param inputs The list of input data objects containing user info.
     */
    public void loadUsers(final List<InputData> inputs) {
        users.clear();
        for (InputData data : inputs) {
            // Check role
            String roleStr = data.getRole();
            if (roleStr == null) {
                roleStr = data.getUserType(); // Fallback
            }
            if (roleStr == null) {
                continue;
            }

            switch (roleStr.toUpperCase()) {
                case "REPORTER":
                    users.add(new Reporter(data.getUsername(), data.getEmail()));
                    break;

                case "MANAGER":
                    if (data.getHireDate() != null) {
                        users.add(new Manager(
                                data.getUsername(),
                                data.getEmail(),
                                data.getHireDate(), // Already LocalDate
                                data.getSubordinates()
                        ));
                    }
                    break;

                case "DEVELOPER":
                    if (data.getHireDate() != null) {
                        // System.out.println("Loading Developer: " + data.getUsername());
                        // System.out.println("Expertise: " + data.getExpertiseArea());
                        users.add(new Developer(
                                data.getUsername(),
                                data.getEmail(),
                                data.getHireDate(),      // Already LocalDate
                                data.getExpertiseArea(), // Already Enum Expertise
                                data.getSeniority()      // Already Enum Seniority
                        ));
                    }
                    break;
                default:
                    break;
            }
        }
    }

    // --- Helper Methods ---

    /**
     * Finds a user by their username.
     *
     * @param username The username to search for.
     * @return The User object or null if not found.
     */
    public User findUserByUsername(final String username) {
        for (User u : users) {
            if (u.getUsername().equals(username)) {
                return u;
            }
        }
        return null;
    }

    /**
     * Adds a ticket to the database.
     *
     * @param ticket The ticket to add.
     */
    public void addTicket(final Ticket ticket) {
        tickets.add(ticket);
    }

    /**
     * Retrieves the list of all tickets.
     *
     * @return The list of tickets.
     */
    public List<Ticket> getTickets() {
        return tickets;
    }

    /**
     * Adds a milestone to the database.
     *
     * @param milestone The milestone to add.
     */
    public void addMilestone(final Milestone milestone) {
        milestones.add(milestone);
    }

    /**
     * Retrieves the list of all milestones.
     *
     * @return The list of milestones.
     */
    public List<Milestone> getMilestones() {
        return milestones;
    }

    /**
     * Checks if the testing phase flag is currently true.
     *
     * @return True if testing phase is active, false otherwise.
     */
    public boolean isTestingPhaseActive() {
        return testingPhaseActive;
    }

    /**
     * Sets the testing phase activity flag.
     *
     * @param testingPhaseActive The new state of the testing phase.
     */
    public void setTestingPhaseActive(final boolean testingPhaseActive) {
        this.testingPhaseActive = testingPhaseActive;
    }

    /**
     * Sets the project start date.
     *
     * @param date The project start date.
     */
    public void setProjectStartDate(final LocalDate date) {
        // Set start date only if not already set (first command dictates start)
        if (this.projectStartDate == null) {
            this.projectStartDate = date;
        }
    }

    /**
     * Gets the project start date.
     *
     * @return The project start date.
     */
    public LocalDate getProjectStartDate() {
        return projectStartDate;
    }

    /**
     * Checks if the testing phase is active based on the current command date.
     * The testing phase lasts for 12 days from the start date.
     *
     * @param currentCommandDate The date of the current command.
     * @return True if the testing phase is active, false otherwise.
     */
    public boolean isTestingPhaseActive(final LocalDate currentCommandDate) {
        if (!testingPhaseActive) {
            return false; // If manually stopped by manager
        }
        if (projectStartDate == null) {
            return true; // First command
        }

        // Calculate day difference
        long daysBetween = ChronoUnit.DAYS.between(projectStartDate, currentCommandDate);

        // Testing phase is 12 days (days 0..11 or 1..12 depends on interpretation).
        // Usually rule is <= 12 days.
        return daysBetween <= TESTING_PHASE_DAYS;
    }

    /**
     * Retrieves a ticket by its ID.
     *
     * @param id The ID of the ticket.
     * @return The Ticket object, or null if not found.
     */
    public Ticket getTicket(final int id) {
        for (Ticket t : tickets) {
            if (t.getId() == id) {
                return t;
            }
        }
        return null;
    }

    /**
     * Retrieves the role of a user by username.
     *
     * @param username The username.
     * @return The role as a string, or "Developer" if not found.
     */
    public String getUserRole(final String username) {
        for (User u : users) {
            if (u.getUsername().equals(username)) {
                // Assuming User has getRole() returning Object/String
                return String.valueOf(u.getRole());
            }
        }
        // Fallback if user does not exist (or return "Developer" by default)
        return "Developer";
    }

    /**
     * Finds a milestone that contains a specific ticket ID.
     *
     * @param ticketId The ticket ID to search for.
     * @return The Milestone object, or null if not found.
     */
    public Milestone findMilestoneByTicketId(final int ticketId) {
        for (Milestone m : milestones) {
            if (m.getTicketIds().contains(ticketId)) {
                return m;
            }
        }
        return null;
    }

    // --- NEW METHODS FOR STOPPING EXECUTION ---

    /**
     * Stops the execution/testing phase permanently.
     */
    public void stopExec() {
        this.stopTestingPhase = true;
    }

    /**
     * Checks if the execution stop flag is set.
     *
     * @return True if execution should stop.
     */
    public boolean isStopTestingPhase() {
        return stopTestingPhase;
    }
}
