package main.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import main.commands.Command;
import main.database.Database;
import main.model.ticket.*;
import main.model.user.User;
import main.model.user.enums.UserRoles;
import main.utils.InputData;

import java.time.LocalDate;
import java.util.List;

public class ReportTicketCommand implements Command {
    private final InputData data;

    public ReportTicketCommand(InputData data) {
        this.data = data;
    }

    @Override
    public void execute(List<ObjectNode> outputs) {
        Database db = Database.getInstance();
        ObjectMapper mapper = new ObjectMapper();

        LocalDate commandDate = LocalDate.parse(data.getTimestamp());

        db.setProjectStartDate(commandDate);

        // 1. Validare: Perioada de testare
        // 2. VERIFICARE: Perioada de Testare (12 zile)
        if (!db.isTestingPhaseActive(commandDate)) {
            ObjectNode error = mapper.createObjectNode();
            error.put("command", "reportTicket");
            // Output-ul tău cere și username/timestamp în eroare
            error.put("username", data.getUsername());
            error.put("timestamp", data.getTimestamp());
            error.put("error", "Tickets can only be reported during testing phases.");

            outputs.add(error);
            return; // Oprim execuția, nu creăm tichetul
        }

        // 2. Validare: User existent
        User user = db.findUserByUsername(data.getUsername());
        if (user == null) {
            ObjectNode error = mapper.createObjectNode();
            error.put("command", "reportTicket");
            error.put("username", data.getUsername());
            error.put("timestamp", data.getTimestamp());
            error.put("error", "The user " + data.getUsername() + " does not exist.");
            outputs.add(error);
            return;
        }

        // 3. Validare: Rol de Reporter
        // Atenție: Asigură-te că user.getRole() returnează un String sau Enum compatibil
        // Aici presupun că getRole() returnează un Enum sau String care se poate compara
        if (!String.valueOf(user.getRole()).equalsIgnoreCase("REPORTER")) {
            ObjectNode error = mapper.createObjectNode();
            error.put("command", "reportTicket");
            error.put("status", "error");
            error.put("description", "The user does not have permission to execute this command: required role REPORTER; user role " + user.getRole() + ".");
            outputs.add(error);
            return;
        }

        // Logica pentru Tichete Anonime
        // Dacă reportedBy lipsește din JSON (e null), autorul e cel care dă comanda
        // Dacă e string gol "", e anonim
        String author = data.getUsername(); // Default
        System.out.println("Author initial: " + author);
        String reportedBy = data.getReportedBy(); // Presupunem că ai adăugat acest câmp în InputData
        if (reportedBy.isEmpty()) {
            System.out.println("Eroare: utilizator anonim");
            // REGULA: Doar BUG-urile pot fi anonime
            String type = data.getType(); // BUG, FEATURE_REQUEST, etc.

            if (type != null && !type.equalsIgnoreCase("BUG")) {
                // --- GENERARE EROARE SPECIFICĂ ---
                ObjectNode error = mapper.createObjectNode();
                error.put("command", "reportTicket");
                // Atenție: output-ul cerut de tine nu avea câmpul "status", dar îl pun pentru siguranță.
                // Dacă checker-ul e strict pe formatul dat de tine, șterge linia cu "status".
                // error.put("status", "error");

                error.put("username", data.getUsername());
                error.put("timestamp", data.getTimestamp());
                error.put("error", "Anonymous reports are only allowed for tickets of type BUG.");

                outputs.add(error);
                return; // Stop execuție
            }

            // Dacă e BUG anonim, prioritatea devine automat LOW (conform enunțului)
            data.setPriority("LOW");
        }
        // Verificăm dacă avem un câmp specific de autor în InputData (dacă ai adăugat reportedBy)
        // Dacă nu ai reportedBy în InputData, logica de anonim e mai complicată, dar presupunem standardul:

        // Verificăm tipul tichetului (BUG, FEATURE etc)
        // În InputData, tipul vine în userType sau ai creat un câmp `type`?
        // Presupunem userType conform codului tău anterior.
        String ticketType = data.getType();

        if (ticketType == null) {
            System.out.println("Eroare: Tipul tichetului nu a fost citit corect din JSON pentru comanda reportTicket.");
            return;
        }

        // Generare ID incremental
        int newId = db.getTickets().size();

        Ticket newTicket = switch (ticketType.toUpperCase()) {
            case "BUG" ->
                // Instantiere Bug
                // Constructori trebuie sa existe in modele!
                    new Bug(newId, data.getType(), data.getTitle(), data.getDescription(),
                            data.getPriority(), "OPEN", data.getReportedBy(), data.getTimestamp());
            case "FEATURE_REQUEST" -> new FeatureRequest(newId, data.getType(), data.getTitle(), data.getDescription(),
                    data.getPriority(), "OPEN", data.getReportedBy(), data.getTimestamp());
            case "UI_FEEDBACK" -> new UIFeedback(newId, data.getType(), data.getTitle(), data.getDescription(),
                    data.getPriority(), "OPEN", data.getReportedBy(), data.getTimestamp());
            default -> null;
        };

        if (newTicket != null) {
            db.addTicket(newTicket);
            // NU se adaugă output de succes ("Nu există output" conform enunțului)
        }
    }
}