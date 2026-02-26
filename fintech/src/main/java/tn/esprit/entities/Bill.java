package tn.esprit.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Bill {

    private int id;
    private String name;
    private BigDecimal amount;
    private int dueDay;
    private String frequency;
    private String category;
    private String description;
    private Integer budgetId;
    private String status;
    private LocalDate createdAt;

    public Bill() {}

    public Bill(String name, BigDecimal amount, int dueDay, String frequency,
                String category, String description, Integer budgetId) {
        this.name = name;
        this.amount = amount;
        this.dueDay = dueDay;
        this.frequency = frequency;
        this.category = category;
        this.description = description;
        this.budgetId = budgetId;
        this.status = "UNPAID";
        this.createdAt = LocalDate.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public int getDueDay() { return dueDay; }
    public void setDueDay(int dueDay) { this.dueDay = dueDay; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getBudgetId() { return budgetId; }
    public void setBudgetId(Integer budgetId) { this.budgetId = budgetId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }

    public LocalDate getNextDueDate() {
        LocalDate today = LocalDate.now();
        LocalDate dueDate = today.withDayOfMonth(Math.min(dueDay, today.lengthOfMonth()));
        if (dueDate.isBefore(today)) {
            dueDate = dueDate.plusMonths(1);
        }
        return dueDate;
    }

    public int getDaysUntilDue() {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), getNextDueDate());
    }
}