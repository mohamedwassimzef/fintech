package tn.esprit.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Expense entity representing a user's expense.
 */
public class Expense {
    private int id;
    private BigDecimal amount;
    private String category;
    private LocalDate expenseDate;
    private String description;
    private Integer budgetId;
    private LocalDateTime createdAt;

    // Constructors
    public Expense() {}

    public Expense(int id, BigDecimal amount, String category, LocalDate expenseDate,
                  String description, Integer budgetId, LocalDateTime createdAt) {
        this.id = id;
        this.amount = amount;
        this.category = category;
        this.expenseDate = expenseDate;
        this.description = description;
        this.budgetId = budgetId;
        this.createdAt = createdAt;
    }

    public Expense(BigDecimal amount, String category, LocalDate expenseDate,
                  String description, Integer budgetId) {
        this.amount = amount;
        this.category = category;
        this.expenseDate = expenseDate;
        this.description = description;
        this.budgetId = budgetId;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getBudgetId() { return budgetId; }
    public void setBudgetId(Integer budgetId) { this.budgetId = budgetId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Expense{" +
                "id=" + id +
                ", amount=" + amount +
                ", category='" + category + '\'' +
                ", expenseDate=" + expenseDate +
                ", description='" + description + '\'' +
                ", budgetId=" + budgetId +
                ", createdAt=" + createdAt +
                '}';
    }
}

