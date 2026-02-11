package tn.esprit.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Complaint entity representing a user's complaint.
 */
public class Complaint {
    private int id;
    private String subject;
    private String status; // pending, resolved, rejected
    private LocalDate complaintDate;
    private String response;
    private Integer userId;
    private LocalDateTime createdAt;

    // Constructors
    public Complaint() {}

    public Complaint(int id, String subject, String status, LocalDate complaintDate,
                    String response, Integer userId, LocalDateTime createdAt) {
        this.id = id;
        this.subject = subject;
        this.status = status;
        this.complaintDate = complaintDate;
        this.response = response;
        this.userId = userId;
        this.createdAt = createdAt;
    }

    public Complaint(String subject, LocalDate complaintDate, Integer userId) {
        this.subject = subject;
        this.complaintDate = complaintDate;
        this.userId = userId;
        this.status = "pending";
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getComplaintDate() { return complaintDate; }
    public void setComplaintDate(LocalDate complaintDate) { this.complaintDate = complaintDate; }

    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Complaint{" +
                "id=" + id +
                ", subject='" + subject + '\'' +
                ", status='" + status + '\'' +
                ", complaintDate=" + complaintDate +
                ", response='" + response + '\'' +
                ", userId=" + userId +
                ", createdAt=" + createdAt +
                '}';
    }
}

