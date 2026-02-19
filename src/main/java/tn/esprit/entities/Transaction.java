package tn.esprit.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaction {
    private int id;
    private int senderId;      // Émetteur de la transaction
    private int receiverId;    // Destinataire de la transaction
    private BigDecimal amount;
    private String type;       // 'debit','credit'
    private String status;     // 'pending','completed','failed'
    private String description;
    private LocalDateTime createdAt;
    private String referenceType;  // 'loan','contract','repayment','budget','online'
    private Integer referenceId;
    private String currency;   // 'TND','USD'

    // Champs transients pour affichage
    private transient String senderName;
    private transient String receiverName;

    // Constructeur vide
    public Transaction() {
    }

    // Constructeur complet (avec ID et dates)
    public Transaction(int id, int senderId, int receiverId, BigDecimal amount, String type,
                       String status, String description, LocalDateTime createdAt,
                       String referenceType, Integer referenceId, String currency) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.amount = amount;
        this.type = type;
        this.status = status;
        this.description = description;
        this.createdAt = createdAt;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.currency = currency;
    }

    // Constructeur pour création (sans ID ni date)
    public Transaction(int senderId, int receiverId, BigDecimal amount, String type,
                       String status, String description, String referenceType,
                       Integer referenceId, String currency) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.amount = amount;
        this.type = type;
        this.status = status;
        this.description = description;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.currency = currency;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public int getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(int receiverId) {
        this.receiverId = receiverId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public Integer getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Integer referenceId) {
        this.referenceId = referenceId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", senderId=" + senderId +
                ", receiverId=" + receiverId +
                ", amount=" + amount +
                ", type='" + type + '\'' +
                ", status='" + status + '\'' +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                ", referenceType='" + referenceType + '\'' +
                ", referenceId=" + referenceId +
                ", currency='" + currency + '\'' +
                ", senderName='" + senderName + '\'' +
                ", receiverName='" + receiverName + '\'' +
                '}';
    }
}