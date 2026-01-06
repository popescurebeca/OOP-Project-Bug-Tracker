package main.database;

import main.model.Milestone;
import main.model.ticket.Ticket;
import main.model.user.*;
import main.utils.InputData;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class Database {
    // 1. Instanța unică
    private static Database instance = null;

    // 2. Listele de date
    private List<User> users;
    private List<Ticket> tickets;
    private List<Milestone> milestones;

    // Flag pentru faza proiectului
    private boolean testingPhaseActive;
    private LocalDate projectStartDate = null;

    // 3. Constructor Privat
    private Database() {
        users = new ArrayList<>();
        tickets = new ArrayList<>();
        milestones = new ArrayList<>();
        testingPhaseActive = true; // Default începe cu testare
    }

    // 4. Acces Global
    public static Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    // 5. Încărcarea Userilor (Adaptată la InputData-ul tău cu tipuri corecte)
    public void loadUsers(List<InputData> inputs) {
        users.clear();
        for (InputData data : inputs) {
            // Verificare rol
            String roleStr = data.getRole();
            if (roleStr == null) roleStr = data.getUserType(); // Fallback
            if (roleStr == null) continue;

            switch (roleStr.toUpperCase()) {
                case "REPORTER":
                    users.add(new Reporter(data.getUsername(), data.getEmail()));
                    break;

                case "MANAGER":
                    if (data.getHireDate() != null) {
                        users.add(new Manager(
                                data.getUsername(),
                                data.getEmail(),
                                data.getHireDate(), // E deja LocalDate
                                data.getSubordinates()
                        ));
                    }
                    break;

                case "DEVELOPER":
                    if (data.getHireDate() != null) {
                        users.add(new Developer(
                                data.getUsername(),
                                data.getEmail(),
                                data.getHireDate(),      // E deja LocalDate
                                data.getExpertiseArea(), // E deja Enum Expertise
                                data.getSeniority()      // E deja Enum Seniority
                        ));
                    }
                    break;
            }
        }
    }

    // --- Helper Methods ---

    public User findUserByUsername(String username) {
        for (User u : users) {
            if (u.getUsername().equals(username)) {
                return u;
            }
        }
        return null;
    }

    public void addTicket(Ticket ticket) {
        tickets.add(ticket);
    }

    public List<Ticket> getTickets() {
        return tickets;
    }

    public void addMilestone(Milestone milestone) {
        milestones.add(milestone);
    }

    public List<Milestone> getMilestones() {
        return milestones;
    }

    public boolean isTestingPhaseActive() {
        return testingPhaseActive;
    }

    public void setTestingPhaseActive(boolean testingPhaseActive) {
        this.testingPhaseActive = testingPhaseActive;
    }

    public void setProjectStartDate(LocalDate date) {
        // Setăm data de start doar dacă nu e setată deja (prima comandă dictează startul)
        if (this.projectStartDate == null) {
            this.projectStartDate = date;
        }
    }

    public LocalDate getProjectStartDate() {
        return projectStartDate;
    }

    // Metoda de verificare a fazei
    public boolean isTestingPhaseActive(LocalDate currentCommandDate) {
        if (!testingPhaseActive) return false; // Dacă a fost oprită manual de manager
        if (projectStartDate == null) return true; // Prima comandă

        // Calculăm diferența de zile
        long daysBetween = ChronoUnit.DAYS.between(projectStartDate, currentCommandDate);

        // Perioada de testare e de 12 zile (zilele 0..11 sau 1..12 depinde de interpretare,
        // dar output-ul tau sugereaza ca pe data de 18 (ziua 17) e gata.
        // De obicei regula e <= 12 zile.
        return daysBetween <= 12;
    }
}