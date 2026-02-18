package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.entities.Budget;
import tn.esprit.entities.Expense;
import tn.esprit.services.BudgetService;
import tn.esprit.services.ExpenseService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class AddExpenseController {

    @FXML
    private TextField amountField;

    @FXML
    private ComboBox<String> categoryComboBox;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private DatePicker expenseDatePicker;

    @FXML
    private ComboBox<String> budgetComboBox;

    private ExpenseService expenseService;
    private BudgetService budgetService;
    private List<Budget> budgets;

    public AddExpenseController() {
        this.expenseService = new ExpenseService();
        this.budgetService = new BudgetService();
    }

    @FXML
    public void initialize() {
        // Populate category dropdown
        categoryComboBox.setItems(FXCollections.observableArrayList(
                "Food", "Transportation", "Entertainment", "Shopping",
                "Bills", "Healthcare", "Education", "Other"
        ));

        // Set default date to today
        expenseDatePicker.setValue(LocalDate.now());

        // Load budgets
        loadBudgets();
    }

    private void loadBudgets() {
        budgets = budgetService.getAllBudgets();
        ObservableList<String> budgetNames = FXCollections.observableArrayList();
        budgetNames.add("None"); // Option to not link to any budget

        for (Budget budget : budgets) {
            budgetNames.add(budget.getName() + " (" + budget.getCategory() + ")");
        }

        budgetComboBox.setItems(budgetNames);
        budgetComboBox.setValue("None");
    }

    @FXML
    private void handleSave() {
        try {
            // Validate inputs
            if (amountField.getText().isEmpty()) {
                showAlert("Validation Error", "Please enter an amount.");
                return;
            }

            if (categoryComboBox.getValue() == null) {
                showAlert("Validation Error", "Please select a category.");
                return;
            }

            if (expenseDatePicker.getValue() == null) {
                showAlert("Validation Error", "Please select a date.");
                return;
            }

            // Create expense object
            BigDecimal amount = new BigDecimal(amountField.getText());
            String category = categoryComboBox.getValue();
            String description = descriptionArea.getText();
            LocalDate expenseDate = expenseDatePicker.getValue();

            // Get selected budget ID (if any)
            Integer budgetId = null;
            String selectedBudget = budgetComboBox.getValue();
            if (selectedBudget != null && !selectedBudget.equals("None")) {
                int index = budgetComboBox.getSelectionModel().getSelectedIndex() - 1; // -1 because "None" is first
                if (index >= 0 && index < budgets.size()) {
                    budgetId = budgets.get(index).getId();
                }
            }

            Expense expense = new Expense(amount, category, expenseDate, description, budgetId);

            // Save to database
            boolean success = expenseService.createExpense(expense);

            if (success) {
                showAlert("Success", "Expense added successfully!");
                closeWindow();
            } else {
                showAlert("Error", "Failed to add expense. Please try again.");
            }

        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Please enter a valid amount (e.g., 45.50).");
        } catch (Exception e) {
            showAlert("Error", "An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) amountField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}