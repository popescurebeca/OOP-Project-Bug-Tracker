package main.utils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import main.model.Priority;
import main.model.user.enums.Expertise;
import main.model.user.enums.Seniority;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object (DTO) for handling input data from JSON files.
 * It maps fields for commands, users, tickets, and milestones.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class InputData {

    // Common Field
    private String command;
    private String username;
    private String timestamp;

    // User Fields
    private String email;
    private String role;
    private String userType;
    private LocalDate hireDate;
    private List<String> subordinates;
    private Seniority seniority;

    @JsonProperty("expertiseArea")
    private Expertise expertiseArea;

    // --- Ticket & Milestone Fields
    private String name;
    private String type;

    @JsonProperty("ticketID")
    private int ticketId;

    private String title;
    private String description;
    private String status;
    private Priority priority;     // Maps "businessPriority"
    private String severity;
    private String reportedBy;
    private String milestoneName;
    private String dueDate;

    // Specific Fields for Ticket Details
    private String expectedBehavior;
    private String actualBehavior;
    private String frequency;
    private String uiElementId;
    private String businessValue;
    private String customerDemand;
    private int usabilityScore;
    private int errorCode;
    private String environment;

    // Milestone Specifics
    private List<Integer> tickets;
    private List<String> blockingFor;
    private List<String> assignedDevs;

    // Comment Specifics
    private String comment;

    //  Filters for Search Command
    private Map<String, Object> filters;

    /**
     * Unpacks parameters from the "params" nested object in JSON.
     * This allows flat mapping of fields even if they are nested under "params".
     *
     * @param params The map of parameters.
     */
    @JsonProperty("params")
    private void unpackNested(final Map<String, Object> params) {
        if (params == null) {
            return;
        }
        if (params.containsKey("title")) {
            this.title = (String) params.get("title");
        }
        if (params.containsKey("description")) {
            this.description = (String) params.get("description");
        }
        if (params.containsKey("type")) {
            this.type = (String) params.get("type");
        }
        if (params.containsKey("businessPriority")) {
            String pObj = (String) params.get("businessPriority");
            if (pObj != null) {
                this.priority = Priority.valueOf(pObj);
            }
        }
        if (params.containsKey("frequency")) {
            this.frequency = (String) params.get("frequency");
        }
        if (params.containsKey("severity")) {
            this.severity = (String) params.get("severity");
        }
        if (params.containsKey("reportedBy")) {
            this.reportedBy = (String) params.get("reportedBy");
        }
        if (params.containsKey("expertiseArea")) {
            String expStr = (String) params.get("expertiseArea");
            if (expStr != null) {
                try {
                    this.expertiseArea = Expertise.valueOf(expStr);
                } catch (IllegalArgumentException e) {
                    // Handle unknown enum values gracefully
                    this.expertiseArea = null;
                }
            }
        }
        if (params.containsKey("expectedBehavior")) {
            this.expectedBehavior = (String) params.get("expectedBehavior");
        }
        if (params.containsKey("actualBehavior")) {
            this.actualBehavior = (String) params.get("actualBehavior");
        }
        if (params.containsKey("businessValue")) {
            this.businessValue = (String) params.get("businessValue");
        }
        if (params.containsKey("customerDemand")) {
            this.customerDemand = (String) params.get("customerDemand");
        }
        if (params.containsKey("usabilityScore")) {
            this.usabilityScore = (Integer) params.get("usabilityScore");
        }
        if (params.containsKey("environment")) {
            this.environment = (String) params.get("environment");
        }
        if (params.containsKey("errorCode")) {
            this.errorCode = (Integer) params.get("errorCode");
        }
        if (params.containsKey("uiElementId")) {
            this.uiElementId = (String) params.get("uiElementId");
        }
    }

    // GETTERS AND SETTERS


    public String getCommand() {
        return command;
    }


    public void setCommand(final String command) {
        this.command = command;
    }


    public String getUsername() {
        return username;
    }


    public void setUsername(final String username) {
        this.username = username;
    }


    public String getTimestamp() {
        return timestamp;
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


    public String getUserType() {
        return userType;
    }


    public void setUserType(final String userType) {
        this.userType = userType;
    }


    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(final LocalDate hireDate) {
        this.hireDate = hireDate;
    }


    public List<String> getSubordinates() {
        return subordinates;
    }


    public void setSubordinates(final List<String> subordinates) {
        this.subordinates = subordinates;
    }


    public Seniority getSeniority() {
        return seniority;
    }


    public void setSeniority(final Seniority seniority) {
        this.seniority = seniority;
    }


    public Expertise getExpertiseArea() {
        return expertiseArea;
    }


    public void setExpertiseArea(final Expertise expertiseArea) {
        this.expertiseArea = expertiseArea;
    }


    public String getName() {
        return name;
    }


    public void setName(final String name) {
        this.name = name;
    }


    public String getType() {
        return type;
    }


    public void setType(final String type) {
        this.type = type;
    }


    public int getTicketId() {
        return ticketId;
    }


    public void setTicketId(final int ticketId) {
        this.ticketId = ticketId;
    }


    public String getTitle() {
        return title;
    }


    public void setTitle(final String title) {
        this.title = title;
    }


    public String getDescription() {
        return description;
    }


    public void setDescription(final String description) {
        this.description = description;
    }


    public String getStatus() {
        return status;
    }


    public void setStatus(final String status) {
        this.status = status;
    }

    /**
     * Gets the priority.
     *
     * @return The priority.
     */
    @JsonProperty("businessPriority")
    public Priority getPriority() {
        return priority;
    }

    /**
     * Sets the priority.
     *
     * @param priority The priority.
     */
    @JsonProperty("businessPriority")
    public void setPriority(final Priority priority) {
        this.priority = priority;
    }


    public String getSeverity() {
        return severity;
    }


    public void setSeverity(final String severity) {
        this.severity = severity;
    }


    public String getReportedBy() {
        return reportedBy;
    }


    public void setReportedBy(final String reportedBy) {
        this.reportedBy = reportedBy;
    }


    public String getMilestoneName() {
        return milestoneName;
    }


    public void setMilestoneName(final String milestoneName) {
        this.milestoneName = milestoneName;
    }


    public String getDueDate() {
        return dueDate;
    }


    public void setDueDate(final String dueDate) {
        this.dueDate = dueDate;
    }


    public String getExpectedBehavior() {
        return expectedBehavior;
    }


    public void setExpectedBehavior(final String expectedBehavior) {
        this.expectedBehavior = expectedBehavior;
    }


    public String getActualBehavior() {
        return actualBehavior;
    }


    public void setActualBehavior(final String actualBehavior) {
        this.actualBehavior = actualBehavior;
    }


    public String getFrequency() {
        return frequency;
    }


    public void setFrequency(final String frequency) {
        this.frequency = frequency;
    }


    public String getUiElementId() {
        return uiElementId;
    }


    public void setUiElementId(final String uiElementId) {
        this.uiElementId = uiElementId;
    }


    public String getBusinessValue() {
        return businessValue;
    }


    public void setBusinessValue(final String businessValue) {
        this.businessValue = businessValue;
    }


    public String getCustomerDemand() {
        return customerDemand;
    }


    public void setCustomerDemand(final String customerDemand) {
        this.customerDemand = customerDemand;
    }


    public int getUsabilityScore() {
        return usabilityScore;
    }


    public void setUsabilityScore(final int usabilityScore) {
        this.usabilityScore = usabilityScore;
    }


    public int getErrorCode() {
        return errorCode;
    }


    public void setErrorCode(final int errorCode) {
        this.errorCode = errorCode;
    }


    public String getEnvironment() {
        return environment;
    }


    public void setEnvironment(final String environment) {
        this.environment = environment;
    }


    public List<Integer> getTickets() {
        return tickets;
    }


    public void setTickets(final List<Integer> tickets) {
        this.tickets = tickets;
    }


    public List<String> getBlockingFor() {
        return blockingFor;
    }


    public void setBlockingFor(final List<String> blockingFor) {
        this.blockingFor = blockingFor;
    }


    public List<String> getAssignedDevs() {
        return assignedDevs;
    }


    public void setAssignedDevs(final List<String> assignedDevs) {
        this.assignedDevs = assignedDevs;
    }


    public String getComment() {
        return comment;
    }


    public void setComment(final String comment) {
        this.comment = comment;
    }


    public Map<String, Object> getFilters() {
        return filters;
    }


    public void setFilters(final Map<String, Object> filters) {
        this.filters = filters;
    }
}
