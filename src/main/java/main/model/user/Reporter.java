package main.model.user;

import main.model.user.enums.UserRoles;

public class Reporter extends User {
    public Reporter(String username, String email) {
        super(username, email, String.valueOf(UserRoles.REPORTER));
    }
}