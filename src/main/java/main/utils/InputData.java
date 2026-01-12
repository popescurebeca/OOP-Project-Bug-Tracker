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

    // --- Common Fields (JSON Root) ---
    private String command;
    private String username;
    private String timestamp;

    // --- User Fields (users.json) ---
    private String email;
    private String role;
    private String userType;
    private LocalDate hireDate;
    private List<String> subordinates;
    private Seniority seniority;

    @JsonProperty("expertiseArea")
    private Expertise expertiseArea;

    // --- Ticket & Milestone Fields (From "params" or root) ---
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

    // --- Specific Fields for Ticket Details ---
    private String expectedBehavior;
    private String actualBehavior;
    private String frequency;
    private String uiElementId;
    private String businessValue;
    private String customerDemand;
    private int usabilityScore;
    private int errorCode;
    private String environment;

    // --- Milestone Specifics ---
    private List<Integer> tickets;
    private List<String> blockingFor;
    private List<String> assignedDevs;

    // --- Comment Specifics ---
    private String comment;

    // --- Filters for Search ---
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

    // --- GETTERS AND SETTERS ---

    /**
     * Gets the command name.
     *
     * @return The command name.
     */
    public String getCommand() {
        return command;
    }

    /**
     * Sets the command name.
     *
     * @param command The command name.
     */
    public void setCommand(final String command) {
        this.command = command;
    }

    /**
     * Gets the username.
     *
     * @return The username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username.
     *
     * @param username The username.
     */
    public void setUsername(final String username) {
        this.username = username;
    }

    /**
     * Gets the timestamp.
     *
     * @return The timestamp.
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp.
     *
     * @param timestamp The timestamp.
     */
    public void setTimestamp(final String timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the email.
     *
     * @return The email.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email.
     *
     * @param email The email.
     */
    public void setEmail(final String email) {
        this.email = email;
    }

    /**
     * Gets the role.
     *
     * @return The role.
     */
    public String getRole() {
        return role;
    }

    /**
     * Sets the role.
     *
     * @param role The role.
     */
    public void setRole(final String role) {
        this.role = role;
    }

    /**
     * Gets the user type.
     *
     * @return The user type.
     */
    public String getUserType() {
        return userType;
    }

    /**
     * Sets the user type.
     *
     * @param userType The user type.
     */
    public void setUserType(final String userType) {
        this.userType = userType;
    }

    /**
     * Gets the hire date.
     *
     * @return The hire date.
     */
    public LocalDate getHireDate() {
        return hireDate;
    }

    /**
     * Sets the hire date.
     *
     * @param hireDate The hire date.
     */
    public void setHireDate(final LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    /**
     * Gets the list of subordinates.
     *
     * @return The list of subordinates.
     */
    public List<String> getSubordinates() {
        return subordinates;
    }

    /**
     * Sets the list of subordinates.
     *
     * @param subordinates The list of subordinates.
     */
    public void setSubordinates(final List<String> subordinates) {
        this.subordinates = subordinates;
    }

    /**
     * Gets the seniority level.
     *
     * @return The seniority level.
     */
    public Seniority getSeniority() {
        return seniority;
    }

    /**
     * Sets the seniority level.
     *
     * @param seniority The seniority level.
     */
    public void setSeniority(final Seniority seniority) {
        this.seniority = seniority;
    }

    /**
     * Gets the expertise area.
     *
     * @return The expertise area.
     */
    public Expertise getExpertiseArea() {
        return expertiseArea;
    }

    /**
     * Sets the expertise area.
     *
     * @param expertiseArea The expertise area.
     */
    public void setExpertiseArea(final Expertise expertiseArea) {
        this.expertiseArea = expertiseArea;
    }

    /**
     * Gets the name.
     *
     * @return The name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param name The name.
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Gets the type.
     *
     * @return The type.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type.
     *
     * @param type The type.
     */
    public void setType(final String type) {
        this.type = type;
    }

    /**
     * Gets the ticket ID.
     *
     * @return The ticket ID.
     */
    public int getTicketId() {
        return ticketId;
    }

    /**
     * Sets the ticket ID.
     *
     * @param ticketId The ticket ID.
     */
    public void setTicketId(final int ticketId) {
        this.ticketId = ticketId;
    }

    /**
     * Gets the title.
     *
     * @return The title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title.
     *
     * @param title The title.
     */
    public void setTitle(final String title) {
        this.title = title;
    }

    /**
     * Gets the description.
     *
     * @return The description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     *
     * @param description The description.
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * Gets the status.
     *
     * @return The status.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status.
     *
     * @param status The status.
     */
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

    /**
     * Gets the severity.
     *
     * @return The severity.
     */
    public String getSeverity() {
        return severity;
    }

    /**
     * Sets the severity.
     *
     * @param severity The severity.
     */
    public void setSeverity(final String severity) {
        this.severity = severity;
    }

    /**
     * Gets the reported by user.
     *
     * @return The reported by user.
     */
    public String getReportedBy() {
        return reportedBy;
    }

    /**
     * Sets the reported by user.
     *
     * @param reportedBy The reported by user.
     */
    public void setReportedBy(final String reportedBy) {
        this.reportedBy = reportedBy;
    }

    /**
     * Gets the milestone name.
     *
     * @return The milestone name.
     */
    public String getMilestoneName() {
        return milestoneName;
    }

    /**
     * Sets the milestone name.
     *
     * @param milestoneName The milestone name.
     */
    public void setMilestoneName(final String milestoneName) {
        this.milestoneName = milestoneName;
    }

    /**
     * Gets the due date.
     *
     * @return The due date.
     */
    public String getDueDate() {
        return dueDate;
    }

    /**
     * Sets the due date.
     *
     * @param dueDate The due date.
     */
    public void setDueDate(final String dueDate) {
        this.dueDate = dueDate;
    }

    /**
     * Gets the expected behavior.
     *
     * @return The expected behavior.
     */
    public String getExpectedBehavior() {
        return expectedBehavior;
    }

    /**
     * Sets the expected behavior.
     *
     * @param expectedBehavior The expected behavior.
     */
    public void setExpectedBehavior(final String expectedBehavior) {
        this.expectedBehavior = expectedBehavior;
    }

    /**
     * Gets the actual behavior.
     *
     * @return The actual behavior.
     */
    public String getActualBehavior() {
        return actualBehavior;
    }

    /**
     * Sets the actual behavior.
     *
     * @param actualBehavior The actual behavior.
     */
    public void setActualBehavior(final String actualBehavior) {
        this.actualBehavior = actualBehavior;
    }

    /**
     * Gets the frequency.
     *
     * @return The frequency.
     */
    public String getFrequency() {
        return frequency;
    }

    /**
     * Sets the frequency.
     *
     * @param frequency The frequency.
     */
    public void setFrequency(final String frequency) {
        this.frequency = frequency;
    }

    /**
     * Gets the UI element ID.
     *
     * @return The UI element ID.
     */
    public String getUiElementId() {
        return uiElementId;
    }

    /**
     * Sets the UI element ID.
     *
     * @param uiElementId The UI element ID.
     */
    public void setUiElementId(final String uiElementId) {
        this.uiElementId = uiElementId;
    }

    /**
     * Gets the business value.
     *
     * @return The business value.
     */
    public String getBusinessValue() {
        return businessValue;
    }

    /**
     * Sets the business value.
     *
     * @param businessValue The business value.
     */
    public void setBusinessValue(final String businessValue) {
        this.businessValue = businessValue;
    }

    /**
     * Gets the customer demand.
     *
     * @return The customer demand.
     */
    public String getCustomerDemand() {
        return customerDemand;
    }

    /**
     * Sets the customer demand.
     *
     * @param customerDemand The customer demand.
     */
    public void setCustomerDemand(final String customerDemand) {
        this.customerDemand = customerDemand;
    }

    /**
     * Gets the usability score.
     *
     * @return The usability score.
     */
    public int getUsabilityScore() {
        return usabilityScore;
    }

    /**
     * Sets the usability score.
     *
     * @param usabilityScore The usability score.
     */
    public void setUsabilityScore(final int usabilityScore) {
        this.usabilityScore = usabilityScore;
    }

    /**
     * Gets the error code.
     *
     * @return The error code.
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Sets the error code.
     *
     * @param errorCode The error code.
     */
    public void setErrorCode(final int errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * Gets the environment.
     *
     * @return The environment.
     */
    public String getEnvironment() {
        return environment;
    }

    /**
     * Sets the environment.
     *
     * @param environment The environment.
     */
    public void setEnvironment(final String environment) {
        this.environment = environment;
    }

    /**
     * Gets the list of tickets.
     *
     * @return The list of tickets.
     */
    public List<Integer> getTickets() {
        return tickets;
    }

    /**
     * Sets the list of tickets.
     *
     * @param tickets The list of tickets.
     */
    public void setTickets(final List<Integer> tickets) {
        this.tickets = tickets;
    }

    /**
     * Gets the list of blocking milestones.
     *
     * @return The list of blocking milestones.
     */
    public List<String> getBlockingFor() {
        return blockingFor;
    }

    /**
     * Sets the list of blocking milestones.
     *
     * @param blockingFor The list of blocking milestones.
     */
    public void setBlockingFor(final List<String> blockingFor) {
        this.blockingFor = blockingFor;
    }

    /**
     * Gets the list of assigned developers.
     *
     * @return The list of assigned developers.
     */
    public List<String> getAssignedDevs() {
        return assignedDevs;
    }

    /**
     * Sets the list of assigned developers.
     *
     * @param assignedDevs The list of assigned developers.
     */
    public void setAssignedDevs(final List<String> assignedDevs) {
        this.assignedDevs = assignedDevs;
    }

    /**
     * Gets the comment.
     *
     * @return The comment.
     */
    public String getComment() {
        return comment;
    }

    /**
     * Sets the comment.
     *
     * @param comment The comment.
     */
    public void setComment(final String comment) {
        this.comment = comment;
    }

    /**
     * Gets the search filters.
     *
     * @return The search filters.
     */
    public Map<String, Object> getFilters() {
        return filters;
    }

    /**
     * Sets the search filters.
     *
     * @param filters The search filters.
     */
    public void setFilters(final Map<String, Object> filters) {
        this.filters = filters;
    }
}
