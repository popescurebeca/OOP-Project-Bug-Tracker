package main.model.ticket;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import main.model.Priority;
import main.visitor.Visitable;
import main.visitor.Visitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for all ticket types.
 */
public abstract class Ticket implements Visitable {
    private final int id;
    private final String type;
    private final String title;
    private final String description;

    @JsonProperty("businessPriority")
    private Priority priority;
    private Priority initialPriority;
    private Priority forcePriority; // Priority calculated by Milestone logic

    private String status;
    private final String createdAt;

    // Empty initialized fields
    private String assignedAt = "";
    private String solvedAt = "";
    private String assignedTo = "";
    private String reportedBy = "";
    private String expertiseArea;

    private List<Comment> comments = new ArrayList<>();
    private List<HistoryEntry> history = new ArrayList<>();

    /**
     * Inner class representing a comment on a ticket.
     */
    public static class Comment {
        private String author;
        private String content;
        private String createdAt;

        /**
         * Constructor for Comment.
         *
         * @param author    The author of the comment.
         * @param content   The content of the comment.
         * @param createdAt The creation timestamp.
         */
        public Comment(final String author, final String content, final String createdAt) {
            this.author = author;
            this.content = content;
            this.createdAt = createdAt;
        }

        public String getAuthor() {
            return author;
        }

        public String getContent() {
            return content;
        }

        public String getCreatedAt() {
            return createdAt;
        }
    }

    /**
     * Inner class representing a history entry for ticket actions.
     */
    public static class HistoryEntry {
        private String action;      // ASSIGNED, STATUS_CHANGED, etc.
        private String by;
        private String timestamp;

        // Optional fields
        private String from;
        private String to;
        private String milestone;

        /**
         * Constructor for HistoryEntry.
         *
         * @param action    The action name.
         * @param by        The user who performed the action.
         * @param timestamp The timestamp of the action.
         */
        public HistoryEntry(final String action, final String by, final String timestamp) {
            this.action = action;
            this.by = by;
            this.timestamp = timestamp;
        }

        /**
         * Sets the 'from' and 'to' fields for status changes.
         *
         * @param from The old status.
         * @param to   The new status.
         * @return The current HistoryEntry instance.
         */
        public HistoryEntry setFromTo(final String from, final String to) {
            this.from = from;
            this.to = to;
            return this;
        }

        /**
         * Sets the milestone related to the action.
         *
         * @param milestone The milestone name.
         * @return The current HistoryEntry instance.
         */
        public HistoryEntry setMilestone(final String milestone) {
            this.milestone = milestone;
            return this;
        }

        public String getAction() {
            return action;
        }

        public String getBy() {
            return by;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        public String getMilestone() {
            return milestone;
        }
    }

    /**
     * Constructor for Ticket.
     *
     * @param id          The unique ID of the ticket.
     * @param type        The type of the ticket.
     * @param title       The title of the ticket.
     * @param description The description of the ticket.
     * @param priority    The priority of the ticket.
     * @param status      The status of the ticket.
     * @param reportedBy  The username of the reporter.
     * @param createdAt   The creation timestamp.
     */
    public Ticket(final int id, final String type, final String title, final String description,
                  final Priority priority, final String status, final String reportedBy,
                  final String createdAt) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.reportedBy = reportedBy;
        this.createdAt = createdAt;
    }

    // --- METHODS FOR COMMENTS ---

    public List<Comment> getComments() {
        return comments;
    }

    /**
     * Adds a comment to the ticket.
     *
     * @param author    The author of the comment.
     * @param content   The content of the comment.
     * @param createdAt The creation timestamp.
     */
    public void addComment(final String author, final String content, final String createdAt) {
        this.comments.add(new Comment(author, content, createdAt));
    }

    /**
     * Removes a comment from the ticket.
     *
     * @param c The comment to remove.
     */
    public void removeComment(final Comment c) {
        this.comments.remove(c);
    }

    // --- METHODS FOR HISTORY ---

    public List<HistoryEntry> getHistory() {
        return history;
    }

    /**
     * Adds a history entry to the ticket.
     *
     * @param entry The history entry to add.
     */
    public void addHistory(final HistoryEntry entry) {
        this.history.add(entry);
    }

    /**
     * Checks if the ticket is anonymous.
     *
     * @return True if reportedBy is null or empty, false otherwise.
     */
    @JsonIgnore
    public boolean isAnonymous() {
        return this.reportedBy == null || this.reportedBy.trim().isEmpty();
    }

    // --- GETTERS & SETTERS ---

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Sets the priority of the ticket.
     * Initializes initialPriority if it's null.
     *
     * @param priority The priority to set.
     */
    public void setPriority(final Priority priority) {
        this.priority = priority;
        if (this.initialPriority == null) {
            this.initialPriority = priority;
        }
    }

    /**
     * Sets a forced priority (e.g., from Milestone logic).
     *
     * @param p The priority to force set.
     */
    public void setForcePriority(final Priority p) {
        this.forcePriority = p;
    }

    /**
     * Gets the priority of the ticket.
     * Returns the forced priority if set, otherwise the base priority.
     *
     * @return The effective priority.
     */
    public Priority getPriority() {
        if (this.forcePriority != null) {
            return this.forcePriority;
        }
        return this.priority;
    }

    /**
     * Gets the initial priority of the ticket.
     *
     * @return The initial priority.
     */
    public Priority getInitialPriority() {
        return initialPriority != null ? initialPriority : priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getAssignedAt() {
        return assignedAt != null ? assignedAt : "";
    }

    public void setAssignedAt(final String assignedAt) {
        this.assignedAt = assignedAt;
    }

    public String getSolvedAt() {
        return solvedAt != null ? solvedAt : "";
    }

    public void setSolvedAt(final String solvedAt) {
        this.solvedAt = solvedAt;
    }

    /**
     * Gets the assignee of the ticket.
     *
     * @return The assignee username or empty string if null.
     */
    public String getAssignee() {
        return assignedTo != null ? assignedTo : "";
    }

    /**
     * Sets the assignee of the ticket.
     *
     * @param assignee The assignee username.
     */
    public void setAssignee(final String assignee) {
        this.assignedTo = assignee;
    }

    // This helps serialization if JSON uses 'assignedTo' key
    public String getAssignedTo() {
        return getAssignee();
    }

    public String getReportedBy() {
        return reportedBy;
    }

    public String getExpertiseArea() {
        return expertiseArea;
    }

    public void setExpertiseArea(final String expertiseArea) {
        this.expertiseArea = expertiseArea;
    }

    @Override
    public abstract void accept(final Visitor v);
}