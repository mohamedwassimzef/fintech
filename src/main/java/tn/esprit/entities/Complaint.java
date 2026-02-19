package tn.esprit.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Complaint {
    private int id;
    private String subject;
    private String status;  // 'pending', 'resolved', 'rejected'
    private LocalDate complaintDate;
    private String response;
    private int userId;
    private LocalDateTime createdAt;

    // Champ transient pour afficher le nom de l'utilisateur
    private transient String userName;

    // Constructeur vide
    public Complaint() {
    }

    // Constructeur complet (avec ID et createdAt)
    public Complaint(int id, String subject, String status, LocalDate complaintDate,
                     String response, int userId, LocalDateTime createdAt) {
        this.id = id;
        this.subject = subject;
        this.status = status;
        this.complaintDate = complaintDate;
        this.response = response;
        this.userId = userId;
        this.createdAt = createdAt;
    }

    // Constructeur sans response (pour création simple)
    public Complaint(String subject, String status, LocalDate complaintDate, int userId) {
        this.subject = subject;
        this.status = status;
        this.complaintDate = complaintDate;
        this.userId = userId;
    }

    // ✅ NOUVEAU CONSTRUCTEUR - Avec response (sans ID et createdAt)
    public Complaint(String subject, String status, LocalDate complaintDate,
                     String response, int userId) {
        this.subject = subject;
        this.status = status;
        this.complaintDate = complaintDate;
        this.response = response;
        this.userId = userId;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getComplaintDate() {
        return complaintDate;
    }

    public void setComplaintDate(LocalDate complaintDate) {
        this.complaintDate = complaintDate;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public String toString() {
        return "Complaint{" +
                "id=" + id +
                ", subject='" + subject + '\'' +
                ", status='" + status + '\'' +
                ", complaintDate=" + complaintDate +
                ", response='" + response + '\'' +
                ", userId=" + userId +
                ", userName='" + userName + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}