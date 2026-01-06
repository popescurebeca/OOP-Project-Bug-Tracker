package main.model.user;

import main.model.user.enums.Expertise;
import main.model.user.enums.Seniority;
import main.model.user.enums.UserRoles;

import java.time.LocalDate;

public class Developer extends User {

    private LocalDate hireDate;
    private Expertise expertise; // FRONTEND, BACKEND, etc.
    private Seniority seniority; // JUNIOR, MID, SENIOR

    public Developer(String username, String email, LocalDate hireDate, Expertise expertise, Seniority seniority) {
        super(username, email, String.valueOf(UserRoles.DEVELOPER));
        this.hireDate = hireDate;
        this.expertise = expertise;
        this.seniority = seniority;
        //this.assignedTicketsIds = new ArrayList<>();
    }

    // Getters and Setters
    public LocalDate getHireDate() {return hireDate;}
    public void setHireDate(LocalDate hireDate) {this.hireDate = hireDate;}
    public Expertise getExpertise() {return expertise;}
    public void setExpertise(Expertise expertise) {this.expertise = expertise;}
    public Seniority getSeniority() {return seniority;}
    public void setSeniority(Seniority seniority) {this.seniority = seniority;}
}