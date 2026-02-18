package tn.esprit.entities;

import tn.esprit.enums.ContractStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class InsuredContract {

    private int id;
    private String contractNumber;
    private int assetId;
    private int userId;

    private LocalDate startDate;
    private LocalDate endDate;

    private double premiumAmount;
    private double coverageAmount;

    private ContractStatus status;
    private LocalDateTime createdAt;

    private Integer approvedBy;

    public InsuredContract() {}

    public InsuredContract(int id, String contractNumber, int assetId, int userId,
                           LocalDate startDate, LocalDate endDate,
                           double premiumAmount, double coverageAmount,
                           ContractStatus status, LocalDateTime createdAt,
                           Integer approvedBy) {
        this.id = id;
        this.contractNumber = contractNumber;
        this.assetId = assetId;
        this.userId = userId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.premiumAmount = premiumAmount;
        this.coverageAmount = coverageAmount;
        this.status = status;
        this.createdAt = createdAt;
        this.approvedBy = approvedBy;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getContractNumber() { return contractNumber; }
    public void setContractNumber(String contractNumber) { this.contractNumber = contractNumber; }

    public int getAssetId() { return assetId; }
    public void setAssetId(int assetId) { this.assetId = assetId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public double getPremiumAmount() { return premiumAmount; }
    public void setPremiumAmount(double premiumAmount) { this.premiumAmount = premiumAmount; }

    public double getCoverageAmount() { return coverageAmount; }
    public void setCoverageAmount(double coverageAmount) { this.coverageAmount = coverageAmount; }

    public ContractStatus getStatus() { return status; }
    public void setStatus(ContractStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Integer getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Integer approvedBy) { this.approvedBy = approvedBy; }

    @Override
    public String toString() {
        return "InsuredContract{" +
                "id=" + id +
                ", contractNumber='" + contractNumber + '\'' +
                ", assetId=" + assetId +
                ", userId=" + userId +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", premiumAmount=" + premiumAmount +
                ", coverageAmount=" + coverageAmount +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", approvedBy=" + approvedBy +
                '}';
    }
}
