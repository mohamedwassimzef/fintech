package tn.esprit.services;

import tn.esprit.dao.BudgetDAO;
import tn.esprit.dao.ExpenseDAO;
import tn.esprit.entities.Budget;
import tn.esprit.entities.Expense;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service layer for Budget operations.
 * Handles business logic for budget management.
 */
public class BudgetService {
    private BudgetDAO budgetDAO;
    private ExpenseDAO expenseDAO;

    public BudgetService() {
        this.budgetDAO = new BudgetDAO();
        this.expenseDAO = new ExpenseDAO();
    }

    // ===== CRUD Operations =====

    public boolean createBudget(Budget budget) {
        return budgetDAO.create(budget);
    }

    public Budget getBudgetById(int id) {
        return budgetDAO.read(id);
    }

    public List<Budget> getAllBudgets() {
        return budgetDAO.readAll();
    }

    public boolean updateBudget(Budget budget) {
        return budgetDAO.update(budget);
    }

    public boolean deleteBudget(int id) {
        return budgetDAO.delete(id);
    }

    // ===== Business Logic Methods =====

    /**
     * Calculate how much money is remaining in the budget
     */
    public BigDecimal getRemainingBudget(int budgetId) {
        Budget budget = budgetDAO.read(budgetId);
        if (budget != null) {
            return budget.getAmount().subtract(budget.getSpentAmount());
        }
        return BigDecimal.ZERO;
    }

    /**
     * Check if budget has been exceeded
     */
    public boolean isBudgetExceeded(int budgetId) {
        Budget budget = budgetDAO.read(budgetId);
        if (budget != null) {
            return budget.getSpentAmount().compareTo(budget.getAmount()) > 0;
        }
        return false;
    }

    /**
     * Get budget utilization as a percentage (0-100+)
     * Example: If budget is 500 and spent is 250, returns 50.0
     */
    public double getBudgetUtilizationPercentage(int budgetId) {
        Budget budget = budgetDAO.read(budgetId);
        if (budget != null && budget.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            return budget.getSpentAmount()
                    .divide(budget.getAmount(), 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .doubleValue();
        }
        return 0.0;
    }

    /**
     * Update the spent amount of a budget
     * Call this when an expense is added to a budget
     */
    public boolean updateSpentAmount(int budgetId, BigDecimal additionalAmount) {
        Budget budget = budgetDAO.read(budgetId);
        if (budget != null) {
            BigDecimal newSpent = budget.getSpentAmount().add(additionalAmount);
            budget.setSpentAmount(newSpent);
            return budgetDAO.update(budget);
        }
        return false;
    }

    /**
     * Get warning level for budget
     * Returns: "SAFE" if < 80%, "WARNING" if 80-100%, "EXCEEDED" if > 100%
     */
    public String getBudgetStatus(int budgetId) {
        double utilization = getBudgetUtilizationPercentage(budgetId);

        if (utilization > 100) {
            return "EXCEEDED";
        } else if (utilization >= 80) {
            return "WARNING";
        } else {
            return "SAFE";
        }
    }

    /**
     * Get all expenses for a specific budget
     */
    public List<Expense> getBudgetExpenses(int budgetId) {
        return expenseDAO.getExpensesByBudgetId(budgetId);
    }

    /**
     * Recalculate spent amount based on actual expenses
     * Useful for syncing data
     */
    public boolean recalculateSpentAmount(int budgetId) {
        Budget budget = budgetDAO.read(budgetId);
        if (budget != null) {
            List<Expense> expenses = expenseDAO.getExpensesByBudgetId(budgetId);

            BigDecimal total = BigDecimal.ZERO;
            for (Expense expense : expenses) {
                total = total.add(expense.getAmount());
            }

            budget.setSpentAmount(total);
            return budgetDAO.update(budget);
        }
        return false;
    }
}