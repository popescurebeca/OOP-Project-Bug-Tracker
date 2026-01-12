package main.model.user;

import main.model.user.enums.Expertise;
import main.model.user.enums.Seniority;

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
        // this.seniority = seniority;
        // this.expertiseArea = expertiseArea;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(final String role) {
        this.role = role;
    }

    public double getPerformanceScore() {
        return performanceScore;
    }

    public void setPerformanceScore(final double performanceScore) {
        this.performanceScore = performanceScore;
    }
}