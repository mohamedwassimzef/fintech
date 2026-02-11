package tn.esprit.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Budget entity representing a user's budget.
 */
public class Budget {
    private int id;
    private String name;
    private BigDecimal amount;
    private LocalDate startDate;
    private LocalDate endDate;
    private int userId;
    private String category;
    private BigDecimal spentAmount;

    // Constructors
    public Budget() {}

    public Budget(int id, String name, BigDecimal amount, LocalDate startDate,
                 LocalDate endDate, int userId, String category, BigDecimal spentAmount) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.userId = userId;
        this.category = category;
        this.spentAmount = spentAmount;
    }

    public Budget(String name, BigDecimal amount, LocalDate startDate,
                 LocalDate endDate, int userId, String category) {
        this.name = name;
        this.amount = amount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.userId = userId;
        this.category = category;
        this.spentAmount = BigDecimal.ZERO;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getSpentAmount() { return spentAmount; }
    public void setSpentAmount(BigDecimal spentAmount) { this.spentAmount = spentAmount; }

    @Override
    public String toString() {
        return "Budget{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", amount=" + amount +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", userId=" + userId +
                ", category='" + category + '\'' +
                ", spentAmount=" + spentAmount +
                '}';
    }
}

