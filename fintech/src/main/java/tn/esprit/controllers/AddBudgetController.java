package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.entities.Budget;
import tn.esprit.services.BudgetService;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AddBudgetController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField amountField;

    @FXML
    private ComboBox<String> categoryComboBox;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private TextField userIdField;

    private BudgetService budgetService;

    public AddBudgetController() {
        this.budgetService = new BudgetService();
    }

    @FXML
    public void initialize() {
        // Populate category dropdown
        categoryComboBox.setItems(FXCollections.observableArrayList(
                "Food", "Transportation", "Entertainment", "Shopping",
                "Bills", "Healthcare", "Education", "Other"
        ));

        // Set default dates
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusMonths(1));
    }

    @FXML
    private void handleSave() {
        try {
            // Validate inputs
            if (nameField.getText().isEmpty()) {
                showAlert("Validation Error", "Please enter a budget name.");
                return;
            }

            if (amountField.getText().isEmpty()) {
                showAlert("Validation Error", "Please enter an amount.");
                return;
            }

            if (categoryComboBox.getValue() == null) {
                showAlert("Validation Error", "Please select a category.");
                return;
            }

            if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
                showAlert("Validation Error", "Please select start and end dates.");
                return;
            }

            // Create budget object
            String name = nameField.getText();
            BigDecimal amount = new BigDecimal(amountField.getText());
            String category = categoryComboBox.getValue();
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            int userId = Integer.parseInt(userIdField.getText());

            Budget budget = new Budget(name, amount, startDate, endDate, userId, category);

            // Save to database
            boolean success = budgetService.createBudget(budget);

            if (success) {
                showAlert("Success", "Budget created successfully!");
                closeWindow();
            } else {
                showAlert("Error", "Failed to create budget. Please try again.");
            }

        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Please enter a valid amount (e.g., 500.00).");
        } catch (Exception e) {
            showAlert("Error", "An error occurred: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) nameField.getScene().getWindow();
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