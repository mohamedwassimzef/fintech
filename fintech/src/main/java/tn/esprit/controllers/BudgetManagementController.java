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
import tn.esprit.services.BudgetService;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class BudgetManagementController {

    @FXML
    private TableView<Budget> budgetTable;

    @FXML
    private TableColumn<Budget, Integer> idColumn;

    @FXML
    private TableColumn<Budget, String> nameColumn;

    @FXML
    private TableColumn<Budget, String> categoryColumn;

    @FXML
    private TableColumn<Budget, BigDecimal> amountColumn;

    @FXML
    private TableColumn<Budget, BigDecimal> spentColumn;

    @FXML
    private TableColumn<Budget, String> remainingColumn;

    @FXML
    private TableColumn<Budget, LocalDate> startDateColumn;

    @FXML
    private TableColumn<Budget, LocalDate> endDateColumn;

    @FXML
    private TableColumn<Budget, String> statusColumn;

    @FXML
    private TableColumn<Budget, Void> actionsColumn;

    private BudgetService budgetService;

    public BudgetManagementController() {
        this.budgetService = new BudgetService();
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        loadBudgets();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        spentColumn.setCellValueFactory(new PropertyValueFactory<>("spentAmount"));

        // Remaining column (calculated)
        remainingColumn.setCellValueFactory(cellData -> {
            Budget budget = cellData.getValue();
            BigDecimal remaining = budget.getAmount().subtract(budget.getSpentAmount());
            return new SimpleStringProperty(String.format("$%.2f", remaining));
        });

        startDateColumn.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        endDateColumn.setCellValueFactory(new PropertyValueFactory<>("endDate"));

        // Status column (calculated)
        statusColumn.setCellValueFactory(cellData -> {
            Budget budget = cellData.getValue();
            String status = budgetService.getBudgetStatus(budget.getId());
            return new SimpleStringProperty(status);
        });

        // Style status column
        statusColumn.setCellFactory(column -> new TableCell<Budget, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("SAFE")) {
                        setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                    } else if (item.equals("WARNING")) {
                        setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;");
                    } else if (item.equals("EXCEEDED")) {
                        setStyle("-fx-text-fill: #f44336; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Actions column with Edit and Delete buttons
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
                    Budget budget = getTableView().getItems().get(getIndex());
                    handleEditBudget(budget);
                });

                deleteButton.setOnAction(event -> {
                    Budget budget = getTableView().getItems().get(getIndex());
                    handleDeleteBudget(budget);
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

    private void loadBudgets() {
        List<Budget> budgets = budgetService.getAllBudgets();
        ObservableList<Budget> budgetList = FXCollections.observableArrayList(budgets);
        budgetTable.setItems(budgetList);
    }

    @FXML
    private void handleAddBudget() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddBudget.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Add New Budget");
            stage.setScene(new Scene(root, 500, 450));
            stage.showAndWait();

            loadBudgets();

        } catch (IOException e) {
            showAlert("Error", "Could not open Add Budget window: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    private void handleEditBudget(Budget budget) {
        // Simple edit using dialog
        showEditDialog(budget);
    }

    private void showEditDialog(Budget budget) {
        // Simple edit using text input dialogs
        TextInputDialog nameDialog = new TextInputDialog(budget.getName());
        nameDialog.setTitle("Edit Budget");
        nameDialog.setHeaderText("Edit Budget: " + budget.getName());
        nameDialog.setContentText("New name:");

        Optional<String> nameResult = nameDialog.showAndWait();
        if (nameResult.isPresent()) {
            budget.setName(nameResult.get());

            TextInputDialog amountDialog = new TextInputDialog(budget.getAmount().toString());
            amountDialog.setContentText("New amount:");
            Optional<String> amountResult = amountDialog.showAndWait();

            if (amountResult.isPresent()) {
                try {
                    budget.setAmount(new BigDecimal(amountResult.get()));
                    boolean success = budgetService.updateBudget(budget);

                    if (success) {
                        showAlert("Success", "Budget updated successfully!", Alert.AlertType.INFORMATION);
                        loadBudgets();
                    } else {
                        showAlert("Error", "Failed to update budget.", Alert.AlertType.ERROR);
                    }
                } catch (NumberFormatException e) {
                    showAlert("Error", "Invalid amount format.", Alert.AlertType.ERROR);
                }
            }
        }
    }

    private void handleDeleteBudget(Budget budget) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete Budget");
        confirmAlert.setHeaderText("Are you sure you want to delete this budget?");
        confirmAlert.setContentText("Budget: " + budget.getName() + "\nThis action cannot be undone.");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = budgetService.deleteBudget(budget.getId());

            if (success) {
                showAlert("Success", "Budget deleted successfully!", Alert.AlertType.INFORMATION);
                loadBudgets();
            } else {
                showAlert("Error", "Failed to delete budget.", Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleRefresh() {
        loadBudgets();
        showAlert("Success", "Budgets refreshed!", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void showWelcome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/WelcomeScreen.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) budgetTable.getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 750));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showBudgets() {
        // Already on budgets screen
        loadBudgets();
    }

    @FXML
    private void showExpenses() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ExpenseManagement.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) budgetTable.getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 750));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    @FXML
    private void showConverter() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CurrencyConverter.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) budgetTable.getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 750));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showBills() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/BillManagement.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) budgetTable.getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 750));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void toggleNotifications() {}
}