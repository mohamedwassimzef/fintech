package tn.esprit.services;

import tn.esprit.entities.Bill;
import tn.esprit.entities.Expense;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import tn.esprit.utils.MyDB;
public class BillService {

    private Connection conn;
    private ExpenseService expenseService;

    public BillService() {
        this.conn = MyDB.getInstance().getConx();
        this.expenseService = new ExpenseService();
    }

    public boolean createBill(Bill bill) {
        String sql = "INSERT INTO bills (name, amount, due_day, frequency, category, description, budget_id, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, bill.getName());
            ps.setBigDecimal(2, bill.getAmount());
            ps.setInt(3, bill.getDueDay());
            ps.setString(4, bill.getFrequency());
            ps.setString(5, bill.getCategory());
            ps.setString(6, bill.getDescription());
            if (bill.getBudgetId() != null) ps.setInt(7, bill.getBudgetId());
            else ps.setNull(7, Types.INTEGER);
            ps.setString(8, bill.getStatus());
            ps.setDate(9, Date.valueOf(bill.getCreatedAt()));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Bill> getAllBills() {
        List<Bill> bills = new ArrayList<>();
        String sql = "SELECT * FROM bills ORDER BY due_day ASC";
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                Bill bill = new Bill();
                bill.setId(rs.getInt("id"));
                bill.setName(rs.getString("name"));
                bill.setAmount(rs.getBigDecimal("amount"));
                bill.setDueDay(rs.getInt("due_day"));
                bill.setFrequency(rs.getString("frequency"));
                bill.setCategory(rs.getString("category"));
                bill.setDescription(rs.getString("description"));
                int budgetId = rs.getInt("budget_id");
                if (!rs.wasNull()) bill.setBudgetId(budgetId);
                bill.setStatus(rs.getString("status"));
                Date createdAt = rs.getDate("created_at");
                if (createdAt != null) bill.setCreatedAt(createdAt.toLocalDate());
                bills.add(bill);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bills;
    }

    public boolean markAsPaid(int billId) {
        // Get bill first
        Bill bill = getBillById(billId);
        if (bill == null) return false;

        // Create expense automatically
        if (bill.getBudgetId() != null) {
            Expense expense = new Expense(
                    bill.getAmount(),
                    bill.getCategory(),
                    LocalDate.now(),
                    "Bill payment - " + bill.getName(),
                    bill.getBudgetId()
            );
            expenseService.createExpense(expense);
        }

        // Update status to PAID
        String sql = "UPDATE bills SET status = 'PAID' WHERE id = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, billId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean markAsUnpaid(int billId) {
        String sql = "UPDATE bills SET status = 'UNPAID' WHERE id = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, billId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteBill(int billId) {
        String sql = "DELETE FROM bills WHERE id = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, billId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Bill getBillById(int id) {
        String sql = "SELECT * FROM bills WHERE id = ?";
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Bill bill = new Bill();
                bill.setId(rs.getInt("id"));
                bill.setName(rs.getString("name"));
                bill.setAmount(rs.getBigDecimal("amount"));
                bill.setDueDay(rs.getInt("due_day"));
                bill.setFrequency(rs.getString("frequency"));
                bill.setCategory(rs.getString("category"));
                bill.setDescription(rs.getString("description"));
                int budgetId = rs.getInt("budget_id");
                if (!rs.wasNull()) bill.setBudgetId(budgetId);
                bill.setStatus(rs.getString("status"));
                Date createdAt = rs.getDate("created_at");
                if (createdAt != null) bill.setCreatedAt(createdAt.toLocalDate());
                return bill;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Bill> getUpcomingBills(int days) {
        List<Bill> all = getAllBills();
        List<Bill> upcoming = new ArrayList<>();
        for (Bill bill : all) {
            if (bill.getStatus().equals("UNPAID") && bill.getDaysUntilDue() <= days) {
                upcoming.add(bill);
            }
        }
        return upcoming;
    }
}