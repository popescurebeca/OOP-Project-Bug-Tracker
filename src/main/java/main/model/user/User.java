package main.model.user;

import main.model.user.enums.Expertise;
import main.model.user.enums.Seniority;

import java.util.Optional;

/**
 * Abstract base class for all users in the system.
 */
public abstract class User {
    private String username;
    private String email;
    private String role; // MANAGER, DEVELOPER, EMPLOYEE, REPORTER etc.

    protected double performanceScore = 0.0;

    /**
     * Constructor for User.
     *
     * @param username The username of the user.
     * @param email    The email of the user.
     * @param role     The role of the user.
     */
    public User(final String username, final String email, final String role) {
        this.role = role;
        this.username = username;
        this.email = email;
    }

    /**
     * Extended constructor (if used when loading from JSON).
     *
     * @param username      The username.
     * @param email         The email.
     * @param role          The role.
     * @param seniority     The seniority level (optional).
     * @param expertiseArea The expertise area (optional).
     */
    public User(final String username, final String email, final String role,
                final Seniority seniority, final Expertise expertiseArea) {
        this(username, email, role);
    }

    public final String getUsername() {
        return username;
    }

    public final void setUsername(final String username) {
        this.username = username;
    }

    public final String getRole() {
        return role;
    }

    public final void setRole(final String role) {
        this.role = role;
    }

    public final double getPerformanceScore() {
        return performanceScore;
    }

    public final void setPerformanceScore(final double performanceScore) {
        this.performanceScore = performanceScore;
    }

    public Optional<Developer> isDeveloper() {
        return Optional.empty();
    }
}
