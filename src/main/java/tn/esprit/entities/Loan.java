package tn.esprit.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Loan entity representing a user's loan.
 */
public class Loan {
    private int id;
    private int userId;
    private BigDecimal amount;
    private BigDecimal interestRate;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status; // active, closed, defaulted
    private LocalDateTime createdAt;

    // Constructors
    public Loan() {}

    public Loan(int id, int userId, BigDecimal amount, BigDecimal interestRate,
               LocalDate startDate, LocalDate endDate, String status, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.interestRate = interestRate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Loan(int userId, BigDecimal amount, BigDecimal interestRate,
               LocalDate startDate, LocalDate endDate) {
        this.userId = userId;
        this.amount = amount;
        this.interestRate = interestRate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = "active";
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Loan{" +
                "id=" + id +
                ", userId=" + userId +
                ", amount=" + amount +
                ", interestRate=" + interestRate +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

