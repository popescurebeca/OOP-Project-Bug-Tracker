package main.model.user;

import main.model.user.enums.UserRoles;

/**
 * Represents a Reporter user in the system.
 */
public class Reporter extends User {

    /**
     * Constructor for Reporter.
     *
     * @param username The username of the reporter.
     * @param email    The email of the reporter.
     */
    public Reporter(final String username, final String email) {
        super(username, email, String.valueOf(UserRoles.REPORTER));
    }
}