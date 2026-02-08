package tn.esprit.tests;

import tn.esprit.dao.*;
import tn.esprit.entities.*;
import java.util.List;

public class DeleteOperationTest {
    private static int deleteRoleId = -1;
    private static int deleteUserId = -1;
    private static int deleteTransactionId = -1;
    private static int deleteBudgetId = -1;
    private static int deleteLoanId = -1;
    private static int deleteRepaymentId = -1;
    private static int deleteExpenseId = -1;
    private static int deleteComplaintId = -1;
    private static int deleteAssetId = -1;

    public static void main(String[] args) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("DELETE OPERATION TEST - ALL TABLES");
        System.out.println("=".repeat(80) + "\n");

        // First, retrieve IDs of existing records to delete
        retrieveRecordsToDelete();

        System.out.println("\n" + "█".repeat(80));
        System.out.println("PERFORMING DELETE OPERATIONS");
        System.out.println("█".repeat(80));

        // Delete in order of dependencies (reverse of creation order)
        testDeleteInsuredAsset();
        testDeleteComplaint();
        testDeleteRepayment();
        testDeleteLoan();
        testDeleteBudget();
        testDeleteTransaction();
        testDeleteExpense();
        testDeleteUser();
        testDeleteRole();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("DELETE TESTS COMPLETED");
        System.out.println("=".repeat(80) + "\n");
    }

    private static void retrieveRecordsToDelete() {
        System.out.println("Retrieving records to delete...\n");

        // Get Role
        RoleDAO roleDAO = new RoleDAO();
        List<Role> roles = roleDAO.readAll();
        if (!roles.isEmpty()) {
            deleteRoleId = roles.get(0).getId();
            System.out.println("✓ Found Role ID: " + deleteRoleId);
        }

        // Get User
        UserDAO userDAO = new UserDAO();
        List<User> users = userDAO.readAll();
        if (!users.isEmpty()) {
            deleteUserId = users.get(0).getId();
            System.out.println("✓ Found User ID: " + deleteUserId);
        }

        // Get Transaction
        TransactionDAO transactionDAO = new TransactionDAO();
        List<Transaction> transactions = transactionDAO.readAll();
        if (!transactions.isEmpty()) {
            deleteTransactionId = transactions.get(0).getId();
            System.out.println("✓ Found Transaction ID: " + deleteTransactionId);
        }

        // Get Budget
        BudgetDAO budgetDAO = new BudgetDAO();
        List<Budget> budgets = budgetDAO.readAll();
        if (!budgets.isEmpty()) {
            deleteBudgetId = budgets.get(0).getId();
            System.out.println("✓ Found Budget ID: " + deleteBudgetId);
        }

        // Get Loan
        LoanDAO loanDAO = new LoanDAO();
        List<Loan> loans = loanDAO.readAll();
        if (!loans.isEmpty()) {
            deleteLoanId = loans.get(0).getId();
            System.out.println("✓ Found Loan ID: " + deleteLoanId);
        }

        // Get Repayment
        RepaymentDAO repaymentDAO = new RepaymentDAO();
        List<Repayment> repayments = repaymentDAO.readAll();
        if (!repayments.isEmpty()) {
            deleteRepaymentId = repayments.get(0).getId();
            System.out.println("✓ Found Repayment ID: " + deleteRepaymentId);
        }

        // Get Expense
        ExpenseDAO expenseDAO = new ExpenseDAO();
        List<Expense> expenses = expenseDAO.readAll();
        if (!expenses.isEmpty()) {
            deleteExpenseId = expenses.get(0).getId();
            System.out.println("✓ Found Expense ID: " + deleteExpenseId);
        }

        // Get Complaint
        ComplaintDAO complaintDAO = new ComplaintDAO();
        List<Complaint> complaints = complaintDAO.readAll();
        if (!complaints.isEmpty()) {
            deleteComplaintId = complaints.get(0).getId();
            System.out.println("✓ Found Complaint ID: " + deleteComplaintId);
        }

        // Get InsuredAsset
        InsuredAssetDAO assetDAO = new InsuredAssetDAO();
        List<InsuredAsset> assets = assetDAO.readAll();
        if (!assets.isEmpty()) {
            deleteAssetId = assets.get(0).getId();
            System.out.println("✓ Found InsuredAsset ID: " + deleteAssetId);
        }
    }

    // ======================== DELETE TESTS ========================

    private static void testDeleteInsuredAsset() {
        System.out.println("\n>>> DELETE INSURED ASSET TEST");
        if (deleteAssetId == -1) {
            System.out.println("Delete InsuredAsset: ⊘ SKIPPED (No record found)");
            return;
        }
        InsuredAssetDAO assetDAO = new InsuredAssetDAO();
        boolean result = assetDAO.delete(deleteAssetId);
        System.out.println("Delete InsuredAsset (ID: " + deleteAssetId + "): " + (result ? "✓ SUCCESS" : "✗ FAILED"));
    }

    private static void testDeleteComplaint() {
        System.out.println("\n>>> DELETE COMPLAINT TEST");
        if (deleteComplaintId == -1) {
            System.out.println("Delete Complaint: ⊘ SKIPPED (No record found)");
            return;
        }
        ComplaintDAO complaintDAO = new ComplaintDAO();
        boolean result = complaintDAO.delete(deleteComplaintId);
        System.out.println("Delete Complaint (ID: " + deleteComplaintId + "): " + (result ? "✓ SUCCESS" : "✗ FAILED"));
    }

    private static void testDeleteRepayment() {
        System.out.println("\n>>> DELETE REPAYMENT TEST");
        if (deleteRepaymentId == -1) {
            System.out.println("Delete Repayment: ⊘ SKIPPED (No record found)");
            return;
        }
        RepaymentDAO repaymentDAO = new RepaymentDAO();
        boolean result = repaymentDAO.delete(deleteRepaymentId);
        System.out.println("Delete Repayment (ID: " + deleteRepaymentId + "): " + (result ? "✓ SUCCESS" : "✗ FAILED"));
    }

    private static void testDeleteLoan() {
        System.out.println("\n>>> DELETE LOAN TEST");
        if (deleteLoanId == -1) {
            System.out.println("Delete Loan: ⊘ SKIPPED (No record found)");
            return;
        }
        LoanDAO loanDAO = new LoanDAO();
        boolean result = loanDAO.delete(deleteLoanId);
        System.out.println("Delete Loan (ID: " + deleteLoanId + "): " + (result ? "✓ SUCCESS" : "✗ FAILED"));
    }

    private static void testDeleteBudget() {
        System.out.println("\n>>> DELETE BUDGET TEST");
        if (deleteBudgetId == -1) {
            System.out.println("Delete Budget: ⊘ SKIPPED (No record found)");
            return;
        }
        BudgetDAO budgetDAO = new BudgetDAO();
        boolean result = budgetDAO.delete(deleteBudgetId);
        System.out.println("Delete Budget (ID: " + deleteBudgetId + "): " + (result ? "✓ SUCCESS" : "✗ FAILED"));
    }

    private static void testDeleteTransaction() {
        System.out.println("\n>>> DELETE TRANSACTION TEST");
        if (deleteTransactionId == -1) {
            System.out.println("Delete Transaction: ⊘ SKIPPED (No record found)");
            return;
        }
        TransactionDAO transactionDAO = new TransactionDAO();
        boolean result = transactionDAO.delete(deleteTransactionId);
        System.out.println("Delete Transaction (ID: " + deleteTransactionId + "): " + (result ? "✓ SUCCESS" : "✗ FAILED"));
    }

    private static void testDeleteExpense() {
        System.out.println("\n>>> DELETE EXPENSE TEST");
        if (deleteExpenseId == -1) {
            System.out.println("Delete Expense: ⊘ SKIPPED (No record found)");
            return;
        }
        ExpenseDAO expenseDAO = new ExpenseDAO();
        boolean result = expenseDAO.delete(deleteExpenseId);
        System.out.println("Delete Expense (ID: " + deleteExpenseId + "): " + (result ? "✓ SUCCESS" : "✗ FAILED"));
    }

    private static void testDeleteUser() {
        System.out.println("\n>>> DELETE USER TEST");
        if (deleteUserId == -1) {
            System.out.println("Delete User: ⊘ SKIPPED (No record found)");
            return;
        }
        UserDAO userDAO = new UserDAO();
        boolean result = userDAO.delete(deleteUserId);
        System.out.println("Delete User (ID: " + deleteUserId + "): " + (result ? "✓ SUCCESS" : "✗ FAILED"));
    }

    private static void testDeleteRole() {
        System.out.println("\n>>> DELETE ROLE TEST");
        if (deleteRoleId == -1) {
            System.out.println("Delete Role: ⊘ SKIPPED (No record found)");
            return;
        }
        RoleDAO roleDAO = new RoleDAO();
        boolean result = roleDAO.delete(deleteRoleId);
        System.out.println("Delete Role (ID: " + deleteRoleId + "): " + (result ? "✓ SUCCESS" : "✗ FAILED"));
    }
}

