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
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        File file = fileChooser.showOpenDialog(expenseTable.getScene().getWindow());
        if (file == null) return;

        showAlert("Scanning", "📸 Scanning your receipt, please wait...", Alert.AlertType.INFORMATION);

        new Thread(() -> {
            try {
                System.out.println("=== Starting OCR Scan ===");
                String apiKey = "12654e13-1370-4b01-bb50-6a44998c1a33";

                // Test internet connection first
                if (!testInternetConnection()) {
                    Platform.runLater(() -> showAlert("Error", "No internet connection available", Alert.AlertType.ERROR));
                    return;
                }

                // Perform the OCR scan
                performOCRScan(file, apiKey);

            } catch (Exception e) {
                e.printStackTrace();
                final String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                Platform.runLater(() -> showAlert("Error", "Scan failed: " + errorMsg, Alert.AlertType.ERROR));
            }
        }).start();
    }

    private boolean testInternetConnection() {
        try {
            System.out.println("Testing internet connection...");
            URL testUrl = new URL("https://www.google.com");
            HttpURLConnection testConn = (HttpURLConnection) testUrl.openConnection();
            testConn.setRequestMethod("GET");
            testConn.setConnectTimeout(5000);
            testConn.setReadTimeout(5000);
            int responseCode = testConn.getResponseCode();
            System.out.println("Internet connection test: " + responseCode);
            testConn.disconnect();
            return responseCode == 200;
        } catch (Exception e) {
            System.out.println("Internet connection test failed: " + e.getMessage());
            return false;
        }
    }

    private void performOCRScan(File file, String apiKey) {
        HttpURLConnection conn = null;
        try {
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            // Using the correct Cloudmersive OCR endpoint
            URL url = new URL("https://api.cloudmersive.com/ocr/photo/toText");
            conn = (HttpURLConnection) url.openConnection();

            // Set up the connection
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Apikey", apiKey);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            System.out.println("Request URL: " + url);
            System.out.println("Request Method: POST");
            System.out.println("File: " + file.getName());
            System.out.println("File size: " + file.length() + " bytes");

            // Write the multipart data
            try (DataOutputStream request = new DataOutputStream(conn.getOutputStream())) {
                // Start boundary with file part
                request.writeBytes("--" + boundary + "\r\n");
                request.writeBytes("Content-Disposition: form-data; name=\"imageFile\"; filename=\"" + file.getName() + "\"\r\n");
                request.writeBytes("Content-Type: " + getMimeType(file.getName()) + "\r\n");
                request.writeBytes("\r\n");

                // Write image data
                byte[] imageData = Files.readAllBytes(file.toPath());
                request.write(imageData);
                request.writeBytes("\r\n");

                // End boundary
                request.writeBytes("--" + boundary + "--\r\n");
                request.flush();
            }

            // Get response code
            int responseCode = conn.getResponseCode();
            String responseMessage = conn.getResponseMessage();

            System.out.println("OCR API Response Code: " + responseCode);
            System.out.println("Response Message: " + responseMessage);

            if (responseCode == 200) {
                // Success - read response
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }

                System.out.println("OCR Response received, processing...");
                processOCRResponse(response.toString());

            } else {
                // Handle error response
                StringBuilder errorResponse = new StringBuilder();

                // Try to read error stream if available
                InputStream errorStream = conn.getErrorStream();
                if (errorStream != null) {
                    try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream))) {
                        String line;
                        while ((line = errorReader.readLine()) != null) {
                            errorResponse.append(line);
                        }
                    }
                }

                String errorMsg = "HTTP " + responseCode + " - " + responseMessage;
                if (errorResponse.length() > 0) {
                    errorMsg += "\nDetails: " + errorResponse.toString();
                }

                System.out.println("OCR Error: " + errorMsg);

                final String finalErrorMsg = errorMsg;
                Platform.runLater(() -> showAlert("OCR Error", "Failed to scan receipt.\n" + finalErrorMsg, Alert.AlertType.ERROR));
            }

        } catch (Exception e) {
            System.out.println("OCR scan failed: " + e.getMessage());
            e.printStackTrace();

            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "Unknown error occurred";
            }

            final String finalError = errorMessage;
            Platform.runLater(() -> showAlert("Error", "Scan failed: " + finalError, Alert.AlertType.ERROR));

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String getMimeType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "png": return "image/png";
            case "jpg":
            case "jpeg": return "image/jpeg";
            default: return "application/octet-stream";
        }
    }

    private void processOCRResponse(String jsonResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(jsonResponse);

            StringBuilder extractedText = new StringBuilder();

            // Navigate through the JSON structure
            JsonNode pages = json.path("Pages");
            if (pages.isArray()) {
                for (JsonNode page : pages) {
                    JsonNode lines = page.path("Lines");
                    if (lines.isArray()) {
                        for (JsonNode line : lines) {
                            extractedText.append(line.path("LineText").asText()).append(" ");
                        }
                    }
                }
            }

            String finalText = extractedText.toString().trim();
            System.out.println("Extracted Text: " + finalText);

            String amount = extractAmount(finalText);

            Platform.runLater(() -> {
                if (!amount.isEmpty()) {
                    openExpenseFormWithAmount(amount);
                } else {
                    // Show extracted text even if no amount found
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Receipt Scanned");
                    alert.setHeaderText("Text extracted from receipt:");

                    TextArea textArea = new TextArea(finalText);
                    textArea.setEditable(false);
                    textArea.setWrapText(true);
                    textArea.setMaxWidth(Double.MAX_VALUE);
                    textArea.setMaxHeight(Double.MAX_VALUE);

                    alert.getDialogPane().setContent(textArea);
                    alert.getDialogPane().setPrefSize(500, 400);
                    alert.showAndWait();
                }
            });

        } catch (Exception e) {
            System.out.println("Error parsing OCR response: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> showAlert("Error", "Failed to parse OCR response", Alert.AlertType.ERROR));
        }
    }

    private void openExpenseFormWithAmount(String amount) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddExpense.fxml"));
            Parent root = loader.load();

            AddExpenseController controller = loader.getController();
            controller.prefillAmount(amount);

            Stage stage = new Stage();
            stage.setTitle("Add Expense from Receipt");
            stage.setScene(new Scene(root, 500, 450));
            stage.showAndWait();

            // Refresh the expenses list after adding
            loadExpenses();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not open expense form: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private String extractAmount(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Pattern to find amounts like 10.99, 10,99, $10.99, total 10.99, etc.
        Pattern pattern = Pattern.compile(
                "(?:total|amount|sum|\\$|€|£)?\\s*([0-9]+[.,][0-9]{2})",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            String amount = matcher.group(1).replace(",", ".");
            System.out.println("Found amount: " + amount);
            return amount;
        }

        // Try alternative pattern for whole numbers
        pattern = Pattern.compile(
                "(?:total|amount|sum|\\$|€|£)?\\s*([0-9]+)",
                Pattern.CASE_INSENSITIVE
        );
        matcher = pattern.matcher(text);

        if (matcher.find()) {
            String amount = matcher.group(1) + ".00";
            System.out.println("Found amount (whole number): " + amount);
            return amount;
        }

        return "";
    }

}
