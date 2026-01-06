package main.model.user;

import main.model.user.enums.UserRoles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Manager extends User{
    private LocalDate hireDate;
    private List<String> subordinates; // Lista de username-uri

    public Manager(String username, String email, LocalDate hireDate, List<String> subordinates) {
        super(username, email, String.valueOf(UserRoles.MANAGER));
        this.hireDate = hireDate;
        // Dacă lista e null în JSON, inițializăm una goală
        this.subordinates = subordinates != null ? subordinates : new ArrayList<>();
    }
}
