package tn.esprit.utils;

import tn.esprit.dao.BudgetDAO;
import tn.esprit.dao.ExpenseDAO;
import tn.esprit.entities.Budget;
import tn.esprit.entities.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class TestBudgetAndExpense {
    public static void main(String[] args) {
        System.out.println("=== Testing Budget and Expense DAOs ===\n");

        // Create DAOs
        BudgetDAO budgetDAO = new BudgetDAO();
        ExpenseDAO expenseDAO = new ExpenseDAO();

        // 1. CREATE A BUDGET
        System.out.println("1. Creating a new budget...");
        Budget budget = new Budget(
                "Monthly Groceries",
                new BigDecimal("500.00"),
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 28),
                1, // user_id (you can use any number for testing)
                "Food"
        );

        boolean budgetCreated = budgetDAO.create(budget);
        System.out.println("Budget created: " + budgetCreated);

        // 2. READ ALL BUDGETS
        System.out.println("\n2. Reading all budgets...");
        List<Budget> budgets = budgetDAO.readAll();
        System.out.println("Total budgets: " + budgets.size());
        for (Budget b : budgets) {
            System.out.println("  - " + b.getName() + " | Amount: " + b.getAmount() + " | Category: " + b.getCategory());
        }

        // 3. CREATE AN EXPENSE
        if (!budgets.isEmpty()) {
            System.out.println("\n3. Creating a new expense...");
            Budget firstBudget = budgets.get(0);

            Expense expense = new Expense(
                    new BigDecimal("45.50"),
                    "Food",
                    LocalDate.now(),
                    "Supermarket shopping",
                    firstBudget.getId()
            );

            boolean expenseCreated = expenseDAO.create(expense);
            System.out.println("Expense created: " + expenseCreated);
        }

        // 4. READ ALL EXPENSES
        System.out.println("\n4. Reading all expenses...");
        List<Expense> expenses = expenseDAO.readAll();
        System.out.println("Total expenses: " + expenses.size());
        for (Expense e : expenses) {
            System.out.println("  - " + e.getDescription() + " | Amount: " + e.getAmount() + " | Date: " + e.getExpenseDate());
        }

        System.out.println("\n=== Test Complete! ===");
    }
}
