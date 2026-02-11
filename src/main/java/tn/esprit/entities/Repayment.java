package tn.esprit.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Repayment entity representing a loan repayment.
 */
public class Repayment {
    private int id;
    private int loanId;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String paymentType;
    private String status; // pending, paid, late
    private BigDecimal monthlyPayment;

    // Constructors
    public Repayment() {}

    public Repayment(int id, int loanId, BigDecimal amount, LocalDate paymentDate,
                    String paymentType, String status, BigDecimal monthlyPayment) {
        this.id = id;
        this.loanId = loanId;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.paymentType = paymentType;
        this.status = status;
        this.monthlyPayment = monthlyPayment;
    }

    public Repayment(int loanId, BigDecimal amount, LocalDate paymentDate, String paymentType) {
        this.loanId = loanId;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.paymentType = paymentType;
        this.status = "pending";
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getLoanId() { return loanId; }
    public void setLoanId(int loanId) { this.loanId = loanId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getMonthlyPayment() { return monthlyPayment; }
    public void setMonthlyPayment(BigDecimal monthlyPayment) { this.monthlyPayment = monthlyPayment; }

    @Override
    public String toString() {
        return "Repayment{" +
                "id=" + id +
                ", loanId=" + loanId +
                ", amount=" + amount +
                ", paymentDate=" + paymentDate +
                ", paymentType='" + paymentType + '\'' +
                ", status='" + status + '\'' +
                ", monthlyPayment=" + monthlyPayment +
                '}';
    }
}

