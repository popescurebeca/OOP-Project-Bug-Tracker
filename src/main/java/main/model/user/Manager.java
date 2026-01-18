package main.model.user;

import main.model.user.enums.UserRoles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Manager user in the system.
 */
public final class Manager extends User {
    private LocalDate hireDate;
    private List<String> subordinates; // List of usernames

    /**
     * Constructor for Manager.
     *
     * @param username     The username of the manager.
     * @param email        The email of the manager.
     * @param hireDate     The date the manager was hired.
     * @param subordinates The list of subordinate usernames.
     */
    public Manager(final String username, final String email, final LocalDate hireDate,
                   final List<String> subordinates) {
        super(username, email, String.valueOf(UserRoles.MANAGER));
        this.hireDate = hireDate;
        // If list is null in JSON, initialize an empty one
        this.subordinates = subordinates != null ? subordinates : new ArrayList<>();
    }

    public List<String> getSubordinates() {
        return subordinates;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(final LocalDate hireDate) {
        this.hireDate = hireDate;
    }
}
