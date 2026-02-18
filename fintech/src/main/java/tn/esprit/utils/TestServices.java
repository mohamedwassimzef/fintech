package tn.esprit.utils;

import tn.esprit.entities.Budget;
import tn.esprit.entities.Expense;
import tn.esprit.services.BudgetService;
import tn.esprit.services.ExpenseService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class TestServices {
    public static void main(String[] args) {
        System.out.println("=== Testing Budget and Expense Services ===\n");

        BudgetService budgetService = new BudgetService();
        ExpenseService expenseService = new ExpenseService();

        // 1. CREATE A BUDGET
        System.out.println("1️⃣ Creating a new budget: 'February Groceries' with 600.00 limit");
        Budget budget = new Budget(
                "February Groceries",
                new BigDecimal("600.00"),
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 28),
                1,
                "Food"
        );
        budgetService.createBudget(budget);

        // Get the created budget
        List<Budget> budgets = budgetService.getAllBudgets();
        Budget createdBudget = budgets.get(budgets.size() - 1); // Get the last one
        int budgetId = createdBudget.getId();
        System.out.println("✅ Budget created with ID: " + budgetId);
        System.out.println("   Remaining: " + budgetService.getRemainingBudget(budgetId));
        System.out.println("   Status: " + budgetService.getBudgetStatus(budgetId));
        System.out.println();

        // 2. ADD FIRST EXPENSE (Small)
        System.out.println("2️⃣ Adding expense: Supermarket - 120.00");
        Expense expense1 = new Expense(
                new BigDecimal("120.00"),
                "Food",
                LocalDate.now(),
                "Weekly supermarket shopping",
                budgetId
        );
        expenseService.createExpense(expense1);

        System.out.println("✅ Expense added!");
        System.out.println("   Budget utilization: " +
                String.format("%.1f%%", budgetService.getBudgetUtilizationPercentage(budgetId)));
        System.out.println("   Remaining: " + budgetService.getRemainingBudget(budgetId));
        System.out.println("   Status: " + budgetService.getBudgetStatus(budgetId));
        System.out.println();

        // 3. ADD SECOND EXPENSE (Medium)
        System.out.println("3️⃣ Adding expense: Restaurant - 250.00");
        Expense expense2 = new Expense(
                new BigDecimal("250.00"),
                "Food",
                LocalDate.now(),
                "Dinner with friends",
                budgetId
        );
        expenseService.createExpense(expense2);

        System.out.println("✅ Expense added!");
        System.out.println("   Budget utilization: " +
                String.format("%.1f%%", budgetService.getBudgetUtilizationPercentage(budgetId)));
        System.out.println("   Remaining: " + budgetService.getRemainingBudget(budgetId));
        System.out.println("   Status: " + budgetService.getBudgetStatus(budgetId));
        System.out.println();

        // 4. ADD THIRD EXPENSE (Pushes to WARNING zone)
        System.out.println("4️⃣ Adding expense: Grocery store - 150.00");
        Expense expense3 = new Expense(
                new BigDecimal("150.00"),
                "Food",
                LocalDate.now(),
                "More groceries",
                budgetId
        );
        expenseService.createExpense(expense3);

        System.out.println("✅ Expense added!");
        System.out.println("   Budget utilization: " +
                String.format("%.1f%%", budgetService.getBudgetUtilizationPercentage(budgetId)));
        System.out.println("   Remaining: " + budgetService.getRemainingBudget(budgetId));
        System.out.println("   Status: " + budgetService.getBudgetStatus(budgetId));
        System.out.println();

        // 5. ADD FOURTH EXPENSE (EXCEEDS BUDGET!)
        System.out.println("5️⃣ Adding expense: Pizza delivery - 100.00");
        Expense expense4 = new Expense(
                new BigDecimal("100.00"),
                "Food",
                LocalDate.now(),
                "Late night pizza",
                budgetId
        );
        expenseService.createExpense(expense4);

        System.out.println("✅ Expense added!");
        System.out.println("   Budget utilization: " +
                String.format("%.1f%%", budgetService.getBudgetUtilizationPercentage(budgetId)));
        System.out.println("   Remaining: " + budgetService.getRemainingBudget(budgetId));
        System.out.println("   Status: " + budgetService.getBudgetStatus(budgetId));
        System.out.println("   ⚠️ Budget exceeded: " + budgetService.isBudgetExceeded(budgetId));
        System.out.println();

        // 6. VIEW ALL EXPENSES FOR THIS BUDGET
        System.out.println("6️⃣ All expenses for this budget:");
        List<Expense> budgetExpenses = expenseService.getExpensesByBudget(budgetId);
        for (Expense exp : budgetExpenses) {
            System.out.println("   - " + exp.getDescription() + ": " + exp.getAmount());
        }

        BigDecimal total = expenseService.getTotalExpensesForBudget(budgetId);
        System.out.println("   📊 Total expenses: " + total);
        System.out.println();

        System.out.println("=== Test Complete! ===");
        System.out.println("\n💡 Check phpMyAdmin to see the data!");
    }
}