package main.model.user;

import main.model.user.enums.Expertise;
import main.model.user.enums.Seniority;
import main.model.user.enums.UserRoles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents a Developer user in the system.
 */
public final class Developer extends User {

    private LocalDate hireDate;
    private Expertise expertise; // FRONTEND, BACKEND, etc.
    private Seniority seniority; // JUNIOR, MID, SENIOR

    private final List<String> notifications = new ArrayList<>();

    /**
     * Constructor for Developer.
     *
     * @param username  The username of the developer.
     * @param email     The email of the developer.
     * @param hireDate  The date the developer was hired.
     * @param expertise The area of expertise.
     * @param seniority The seniority level.
     */
    public Developer(final String username, final String email, final LocalDate hireDate,
                     final Expertise expertise, final Seniority seniority) {
        super(username, email, String.valueOf(UserRoles.DEVELOPER));
        this.hireDate = hireDate;
        this.expertise = expertise;
        this.seniority = seniority;
    }

    // Getters and Setters

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(final LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    public Expertise getExpertise() {
        return expertise;
    }

    public void setExpertise(final Expertise expertise) {
        this.expertise = expertise;
    }

    public Seniority getSeniority() {
        return seniority;
    }

    public void setSeniority(final Seniority seniority) {
        this.seniority = seniority;
    }

    /**
     * Adds a notification message to the developer's list.
     *
     * @param message The notification message.
     */
    public void addNotification(final String message) {
        this.notifications.add(message);
    }

    /**
     * Retrieves the list of notifications.
     *
     * @return The list of notifications.
     */
    public List<String> getNotifications() {
        return notifications;
    }

    /**
     * Clears all notifications for the developer.
     */
    public void clearNotifications() {
        this.notifications.clear();
    }

    @Override
    public Optional<Developer> isDeveloper() {
        return Optional.of(this);
    }
}
