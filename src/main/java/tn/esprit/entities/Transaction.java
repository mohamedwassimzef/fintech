package tn.esprit.entities;

import tn.esprit.enums.Currency;
import tn.esprit.enums.ReferenceType;
import tn.esprit.enums.TransactionStatus;
import tn.esprit.enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction entity representing a financial transaction.
 */
public class Transaction {
    private int id;
    private int userId;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionStatus status;
    private String description;
    private LocalDateTime createdAt;
    private ReferenceType referenceType;
    private Integer referenceId;
    private Currency currency;

    // Constructors
    public Transaction() {}

    public Transaction(int id, int userId, BigDecimal amount, TransactionType type,
                      TransactionStatus status, String description, LocalDateTime createdAt,
                      ReferenceType referenceType, Integer referenceId, Currency currency) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.status = status;
        this.description = description;
        this.createdAt = createdAt;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.currency = currency;
    }

    public Transaction(int userId, BigDecimal amount, TransactionType type,
                      String description, ReferenceType referenceType, Integer referenceId) {
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.status = TransactionStatus.PENDING;
        this.description = description;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.currency = Currency.TND;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public ReferenceType getReferenceType() { return referenceType; }
    public void setReferenceType(ReferenceType referenceType) { this.referenceType = referenceType; }

    public Integer getReferenceId() { return referenceId; }
    public void setReferenceId(Integer referenceId) { this.referenceId = referenceId; }

    public Currency getCurrency() { return currency; }
    public void setCurrency(Currency currency) { this.currency = currency; }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", userId=" + userId +
                ", amount=" + amount +
                ", type=" + type +
                ", status=" + status +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                ", referenceType=" + referenceType +
                ", referenceId=" + referenceId +
                ", currency=" + currency +
                '}';
    }
}

