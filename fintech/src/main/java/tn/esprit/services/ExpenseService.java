package tn.esprit.services;

import tn.esprit.dao.ExpenseDAO;
import tn.esprit.dao.BudgetDAO;
import tn.esprit.entities.Expense;
import tn.esprit.entities.Budget;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for Expense operations.
 * Handles business logic for expense management.
 */
public class ExpenseService {
    private ExpenseDAO expenseDAO;
    private BudgetDAO budgetDAO;
    private BudgetService budgetService;

    public ExpenseService() {
        this.expenseDAO = new ExpenseDAO();
        this.budgetDAO = new BudgetDAO();
        this.budgetService = new BudgetService();
    }

    // ===== CRUD Operations =====

    /**
     * Create expense and automatically update the budget's spent amount
     */
    public boolean createExpense(Expense expense) {
        boolean created = expenseDAO.create(expense);

        // If expense is linked to a budget, update the budget's spent amount
        if (created && expense.getBudgetId() != null) {
            budgetService.updateSpentAmount(expense.getBudgetId(), expense.getAmount());
        }

        return created;
    }

    public Expense getExpenseById(int id) {
        return expenseDAO.read(id);
    }

    public List<Expense> getAllExpenses() {
        return expenseDAO.readAll();
    }

    public boolean updateExpense(Expense expense) {
        // Get the old expense to check if amount changed
        Expense oldExpense = expenseDAO.read(expense.getId());

        boolean updated = expenseDAO.update(expense);

        // If expense is linked to a budget and amount changed, recalculate budget
        if (updated && expense.getBudgetId() != null) {
            budgetService.recalculateSpentAmount(expense.getBudgetId());
        }

        return updated;
    }

    public boolean deleteExpense(int id) {
        Expense expense = expenseDAO.read(id);
        boolean deleted = expenseDAO.delete(id);

        // If deleted and was linked to budget, recalculate budget's spent amount
        if (deleted && expense != null && expense.getBudgetId() != null) {
            budgetService.recalculateSpentAmount(expense.getBudgetId());
        }

        return deleted;
    }

    // ===== Business Logic Methods =====

    /**
     * Get expenses by budget ID
     */
    public List<Expense> getExpensesByBudget(int budgetId) {
        return expenseDAO.getExpensesByBudgetId(budgetId);
    }

    /**
     * Get expenses by category
     */
    public List<Expense> getExpensesByCategory(String category) {
        return expenseDAO.getExpensesByCategory(category);
    }

    /**
     * Get expenses within a date range
     */
    public List<Expense> getExpensesByDateRange(LocalDate startDate, LocalDate endDate) {
        return expenseDAO.readAll().stream()
                .filter(e -> !e.getExpenseDate().isBefore(startDate) && !e.getExpenseDate().isAfter(endDate))
                .collect(Collectors.toList());
    }

    /**
     * Calculate total expenses for a specific budget
     */
    public BigDecimal getTotalExpensesForBudget(int budgetId) {
        List<Expense> expenses = expenseDAO.getExpensesByBudgetId(budgetId);
        BigDecimal total = BigDecimal.ZERO;

        for (Expense expense : expenses) {
            total = total.add(expense.getAmount());
        }

        return total;
    }

    /**
     * Calculate total expenses by category
     */
    public BigDecimal getTotalExpensesByCategory(String category) {
        List<Expense> expenses = expenseDAO.getExpensesByCategory(category);
        BigDecimal total = BigDecimal.ZERO;

        for (Expense expense : expenses) {
            total = total.add(expense.getAmount());
        }

        return total;
    }

    /**
     * Get recent expenses (last N days)
     */
    public List<Expense> getRecentExpenses(int days) {
        LocalDate cutoffDate = LocalDate.now().minusDays(days);
        return expenseDAO.readAll().stream()
                .filter(e -> e.getExpenseDate().isAfter(cutoffDate))
                .collect(Collectors.toList());
    }

    /**
     * Get expenses for current month
     */
    public List<Expense> getCurrentMonthExpenses() {
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());

        return getExpensesByDateRange(startOfMonth, endOfMonth);
    }
}