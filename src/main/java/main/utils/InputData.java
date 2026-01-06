package main.utils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import main.model.user.enums.Expertise;
import main.model.user.enums.Seniority;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InputData {
    // --- Câmpuri Comune (Rădăcină JSON) ---
    private String command;
    private String username;
    private String timestamp;

    // --- Câmpuri User (users.json) ---
    private String email;
    private String role;
    private String userType;
    private LocalDate hireDate;
    private List<String> subordinates;
    private Seniority seniority;
    private Expertise expertiseArea;

    // --- Câmpuri Ticket & Milestone (Din "params") ---
    private String type;         // BUG, FEATURE_REQUEST, etc.
    private int ticketId;
    private String title;
    private String description;
    private String status;
    private String priority;     // Mapăm "businessPriority" aici
    private String severity;
    private String reportedBy;
    private String milestoneName;
    private String dueDate;

    // --- Câmpurile care lipseau și provocau erori ---
    private String expectedBehavior;
    private String actualBehavior;
    private String frequency;
    private String uiElementId;
    private String businessValue;
    private String customerDemand;
    private int usabilityScore;
    private int errorCode; // Adăugat pentru completitudine
    private String environment; // Adăugat (apare în JSON-ul de BUG)

    /**
     * --- METODA MAGICĂ UNPACK ---
     * Jackson o apelează automat când găsește "params".
     */
    @JsonProperty("params")
    private void unpackNested(Map<String, Object> params) {
        if (params == null) return;

        // 1. Extragem datele comune
        this.type = (String) params.get("type");
        this.title = (String) params.get("title");
        this.description = (String) params.get("description");
        this.status = (String) params.get("status");

        // 2. Mapare Priority
        if (params.containsKey("businessPriority")) {
            this.priority = (String) params.get("businessPriority");
        } else {
            this.priority = (String) params.get("priority");
        }

        // 3. Extragem câmpurile specifice
        this.severity = (String) params.get("severity");
        this.reportedBy = (String) params.get("reportedBy");

        this.expectedBehavior = (String) params.get("expectedBehavior");
        this.actualBehavior = (String) params.get("actualBehavior");
        this.frequency = (String) params.get("frequency");
        this.milestoneName = (String) params.get("milestoneName");
        this.dueDate = (String) params.get("dueDate");
        this.uiElementId = (String) params.get("uiElementId");
        this.businessValue = (String) params.get("businessValue");
        this.customerDemand = (String) params.get("customerDemand");
        this.environment = (String) params.get("environment");

        // 4. Tratare numere (Integer) - cast sigur
        if (params.get("usabilityScore") != null) {
            this.usabilityScore = (Integer) params.get("usabilityScore");
        }

        if (params.get("errorCode") != null) {
            this.errorCode = (Integer) params.get("errorCode");
        }

        // Dacă ticketId vine în params (uneori vine la AssignTicket)
        if (params.get("ticketId") != null) {
            this.ticketId = (Integer) params.get("ticketId");
        }
    }

    // --- GETTERS & SETTERS (Obligatorii ca să poți folosi datele în comenzi) ---

    // 1. Comune
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    // 2. Useri
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
    public List<String> getSubordinates() { return subordinates; }
    public void setSubordinates(List<String> subordinates) { this.subordinates = subordinates; }
    public Seniority getSeniority() { return seniority; }
    public void setSeniority(Seniority seniority) { this.seniority = seniority; }
    public Expertise getExpertiseArea() { return expertiseArea; }
    public void setExpertiseArea(Expertise expertiseArea) { this.expertiseArea = expertiseArea; }

    // 3. Tichete & Params
    public String getType() { return type; }
    public int getTicketId() { return ticketId; }
    public void setTicketId(int ticketId) { this.ticketId = ticketId; } // Setter necesar pt comenzi
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getSeverity() { return severity; }
    public String getReportedBy() { return reportedBy; }

    public String getMilestoneName() { return milestoneName; }
    public String getDueDate() { return dueDate; }

    // Gettere pentru câmpurile noi (ca să le poți folosi în ReportTicketCommand)
    public String getExpectedBehavior() { return expectedBehavior; }
    public String getActualBehavior() { return actualBehavior; }
    public String getFrequency() { return frequency; }
    public String getUiElementId() { return uiElementId; }
    public String getBusinessValue() { return businessValue; }
    public String getCustomerDemand() { return customerDemand; }
    public int getUsabilityScore() { return usabilityScore; }
    public int getErrorCode() { return errorCode; }
    public String getEnvironment() { return environment; }


}