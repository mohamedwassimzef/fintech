package tn.esprit.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import tn.esprit.entities.Budget;
import tn.esprit.entities.Expense;
import tn.esprit.services.BudgetService;
import tn.esprit.services.ExpenseService;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ExpenseManagementController {

    @FXML
    private TableView<Expense> expenseTable;

    @FXML
    private TableColumn<Expense, Integer> idColumn;

    @FXML
    private TableColumn<Expense, BigDecimal> amountColumn;

    @FXML
    private TableColumn<Expense, String> categoryColumn;

    @FXML
    private TableColumn<Expense, String> descriptionColumn;

    @FXML
    private TableColumn<Expense, LocalDate> dateColumn;

    @FXML
    private TableColumn<Expense, String> budgetColumn;

    @FXML
    private TableColumn<Expense, Void> actionsColumn;

    private ExpenseService expenseService;
    private BudgetService budgetService;

    public ExpenseManagementController() {
        this.expenseService = new ExpenseService();
        this.budgetService = new BudgetService();
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        loadExpenses();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("expenseDate"));

        // Budget column (show budget name if linked)
        budgetColumn.setCellValueFactory(cellData -> {
            Expense expense = cellData.getValue();
            if (expense.getBudgetId() != null) {
                Budget budget = budgetService.getBudgetById(expense.getBudgetId());
                if (budget != null) {
                    return new SimpleStringProperty(budget.getName());
                }
            }
            return new SimpleStringProperty("None");
        });

        // Actions column
        addActionButtons();
    }

    private void addActionButtons() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("✏️ Edit");
            private final Button deleteButton = new Button("🗑️ Delete");

            {
                editButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10;");
                deleteButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10;");

                editButton.setOnAction(event -> {
                    Expense expense = getTableView().getItems().get(getIndex());
                    handleEditExpense(expense);
                });

                deleteButton.setOnAction(event -> {
                    Expense expense = getTableView().getItems().get(getIndex());
                    handleDeleteExpense(expense);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(10, editButton, deleteButton);
                    setGraphic(buttons);
                }
            }
        });
    }

    private void loadExpenses() {
        List<Expense> expenses = expenseService.getAllExpenses();
        ObservableList<Expense> expenseList = FXCollections.observableArrayList(expenses);
        expenseTable.setItems(expenseList);
    }

    @FXML
    private void handleAddExpense() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddExpense.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Add New Expense");
            stage.setScene(new Scene(root, 500, 450));
            stage.showAndWait();

            loadExpenses();

        } catch (IOException e) {
            showAlert("Error", "Could not open Add Expense window: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void handleEditExpense(Expense expense) {
        // Simple edit dialog
        TextInputDialog amountDialog = new TextInputDialog(expense.getAmount().toString());
        amountDialog.setTitle("Edit Expense");
        amountDialog.setHeaderText("Edit Expense: " + expense.getDescription());
        amountDialog.setContentText("New amount:");

        Optional<String> amountResult = amountDialog.showAndWait();
        if (amountResult.isPresent()) {
            try {
                BigDecimal newAmount = new BigDecimal(amountResult.get());
                expense.setAmount(newAmount);

                TextInputDialog descDialog = new TextInputDialog(expense.getDescription());
                descDialog.setContentText("New description:");
                Optional<String> descResult = descDialog.showAndWait();

                if (descResult.isPresent()) {
                    expense.setDescription(descResult.get());

                    boolean success = expenseService.updateExpense(expense);

                    if (success) {
                        showAlert("Success", "Expense updated successfully!", Alert.AlertType.INFORMATION);
                        loadExpenses();
                    } else {
                        showAlert("Error", "Failed to update expense.", Alert.AlertType.ERROR);
                    }
                }
            } catch (NumberFormatException e) {
                showAlert("Error", "Invalid amount format.", Alert.AlertType.ERROR);
            }
        }
    }

    private void handleDeleteExpense(Expense expense) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete Expense");
        confirmAlert.setHeaderText("Are you sure you want to delete this expense?");
        confirmAlert.setContentText("Expense: " + expense.getDescription() + " ($" + expense.getAmount() + ")\nThis action cannot be undone.");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = expenseService.deleteExpense(expense.getId());

            if (success) {
                showAlert("Success", "Expense deleted successfully!", Alert.AlertType.INFORMATION);
                loadExpenses();
            } else {
                showAlert("Error", "Failed to delete expense.", Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleRefresh() {
        loadExpenses();
        showAlert("Success", "Expenses refreshed!", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void showWelcome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/WelcomeScreen.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) expenseTable.getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 750));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showBudgets() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/BudgetManagement.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) expenseTable.getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 750));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showExpenses() {
        // Already on expenses screen
        loadExpenses();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}