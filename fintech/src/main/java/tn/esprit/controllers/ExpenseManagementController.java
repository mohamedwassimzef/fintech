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
import java.util.ArrayList;
import java.io.*;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.stage.FileChooser;
import javafx.application.Platform;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;


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
            showAlert("Error", "Could not load Welcome Screen", Alert.AlertType.ERROR);
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
            showAlert("Error", "Could not load Budgets Screen", Alert.AlertType.ERROR);
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
    @FXML
    private void showConverter() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CurrencyConverter.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) expenseTable.getScene().getWindow();
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
            Stage stage = (Stage) expenseTable.getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 750));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void toggleNotifications() {}

    @FXML
    private void handleScanReceipt() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Receipt Image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp")
        );

        Stage stage = (Stage) expenseTable.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            new Thread(() -> {
                try {
                    String amount = detectAmountFromImage(file);

                    Platform.runLater(() -> {
                        if (amount != null) {
                            showAlert("Success", "Detected amount: $" + amount);
                            prefillAmount(amount);
                        } else {
                            showAlert("Error", "No amount found in receipt");
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert("Error", e.getMessage()));
                }
            }).start();
        }
    }
    // Add this method for 2-parameter alerts
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    private void prefillAmount(String amount) {
        // Create dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Expense from Receipt");
        dialog.setHeaderText("Detected Amount: $" + amount);

        // Set button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create form grid
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Amount field (pre-filled)
        TextField amountField = new TextField(amount);
        amountField.setEditable(true);

        // Description field
        TextField descriptionField = new TextField();
        descriptionField.setPromptText("Enter description");

        // Category field
        TextField categoryField = new TextField();
        categoryField.setPromptText("Enter category");

        // Date picker
        DatePicker datePicker = new DatePicker(LocalDate.now());

        // Budget ComboBox
        ComboBox<Budget> budgetCombo = new ComboBox<>();
        List<Budget> budgets = budgetService.getAllBudgets();
        budgetCombo.setItems(FXCollections.observableArrayList(budgets));

        // Custom display for budget items
        budgetCombo.setCellFactory(lv -> new ListCell<Budget>() {
            @Override
            protected void updateItem(Budget budget, boolean empty) {
                super.updateItem(budget, empty);
                setText(empty || budget == null ? null : budget.getName() + " ($" + budget.getAmount() + ")");
            }
        });

        budgetCombo.setButtonCell(new ListCell<Budget>() {
            @Override
            protected void updateItem(Budget budget, boolean empty) {
                super.updateItem(budget, empty);
                setText(empty || budget == null ? null : budget.getName());
            }
        });

        // Add "No Budget" option
        budgetCombo.getItems().add(0, null);
        budgetCombo.setPromptText("Select a budget (optional)");

        // Add to grid
        grid.add(new Label("Amount:"), 0, 0);
        grid.add(amountField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descriptionField, 1, 1);
        grid.add(new Label("Category:"), 0, 2);
        grid.add(categoryField, 1, 2);
        grid.add(new Label("Date:"), 0, 3);
        grid.add(datePicker, 1, 3);
        grid.add(new Label("Link to Budget:"), 0, 4);
        grid.add(budgetCombo, 1, 4);

        dialog.getDialogPane().setContent(grid);

        // Request focus on description field
        Platform.runLater(() -> descriptionField.requestFocus());

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == saveButtonType) {
            try {
                // Create new expense
                Expense expense = new Expense();
                expense.setAmount(new BigDecimal(amountField.getText()));
                expense.setDescription(descriptionField.getText());
                expense.setCategory(categoryField.getText());
                expense.setExpenseDate(datePicker.getValue());

                // Link to selected budget if any
                Budget selectedBudget = budgetCombo.getValue();
                if (selectedBudget != null) {
                    expense.setBudgetId(selectedBudget.getId());
                }

                // Save to database
                boolean success = expenseService.createExpense(expense);

                if (success) {
                    showAlert("Success", "Expense added successfully!");
                    loadExpenses(); // Refresh the table
                } else {
                    showAlert("Error", "Failed to add expense.");
                }
            } catch (NumberFormatException e) {
                showAlert("Error", "Invalid amount format.");
            } catch (Exception e) {
                showAlert("Error", "Error adding expense: " + e.getMessage());
            }
        }
    }










    private String detectAmountFromImage(File imageFile) throws Exception {
        String apiKey = "K86064184588957"; // Your OCR.space key

        // Use multipart/form-data to send the file directly
        String boundary = "---" + System.currentTimeMillis() + "---";

        URL url = new URL("https://api.ocr.space/parse/image");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"), true)) {

            // Add API key
            writer.append("--" + boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"apikey\"").append("\r\n");
            writer.append("Content-Type: text/plain; charset=UTF-8").append("\r\n");
            writer.append("\r\n");
            writer.append(apiKey).append("\r\n");

            // Add file
            writer.append("--" + boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + imageFile.getName() + "\"").append("\r\n");
            writer.append("Content-Type: " + Files.probeContentType(imageFile.toPath())).append("\r\n");
            writer.append("\r\n");
            writer.flush();

            // Write file bytes
            Files.copy(imageFile.toPath(), os);
            os.flush();

            // End boundary
            writer.append("\r\n");
            writer.append("--" + boundary + "--").append("\r\n");
            writer.flush();
        }

        // Read response
        int responseCode = conn.getResponseCode();
        BufferedReader br = new BufferedReader(new InputStreamReader(
                responseCode == 200 ? conn.getInputStream() : conn.getErrorStream(), "UTF-8"));

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line);
        }
        br.close();

        String responseStr = response.toString();
        System.out.println("OCR Response: " + responseStr);

        if (responseCode != 200) {
            throw new Exception("HTTP Error " + responseCode + ": " + responseStr);
        }

        // Parse JSON
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(responseStr);

        if (root.has("ErrorMessage") && !root.get("ErrorMessage").isNull()) {
            throw new Exception("OCR Error: " + root.get("ErrorMessage").asText());
        }

        if (root.has("ParsedResults") && root.get("ParsedResults").size() > 0) {
            String text = root.path("ParsedResults").get(0).path("ParsedText").asText();
            System.out.println("OCR Text: " + text);

            Pattern pattern = Pattern.compile("\\$?\\s*(\\d+[.,]\\d{2})\\b");
            Matcher matcher = pattern.matcher(text);

            List<String> amounts = new ArrayList<>();
            while (matcher.find()) {
                amounts.add(matcher.group(1).replace(",", "."));
            }

            if (!amounts.isEmpty()) {
                return amounts.stream()
                        .max(java.util.Comparator.comparingDouble(Double::parseDouble))
                        .orElse(null);
            }
        }
        return null;
    }






}
