package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.Bill;
import tn.esprit.entities.Budget;
import tn.esprit.services.BillService;
import tn.esprit.services.BudgetService;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class BillManagementController implements Initializable {

    @FXML private TableView<Bill> billsTable;
    @FXML private TableColumn<Bill, String> nameColumn;
    @FXML private TableColumn<Bill, BigDecimal> amountColumn;
    @FXML private TableColumn<Bill, String> categoryColumn;
    @FXML private TableColumn<Bill, Integer> dueDayColumn;
    @FXML private TableColumn<Bill, String> frequencyColumn;
    @FXML private TableColumn<Bill, String> statusColumn;
    @FXML private TableColumn<Bill, Void> actionsColumn;

    @FXML private Label totalBillsLabel;
    @FXML private Label totalAmountLabel;
    @FXML private Label paidBillsLabel;
    @FXML private Label paidAmountLabel;
    @FXML private Label unpaidBillsLabel;
    @FXML private Label unpaidAmountLabel;
    @FXML private Label dueSoonLabel;

    private BillService billService;
    private BudgetService budgetService;

    public BillManagementController() {
        this.billService = new BillService();
        this.budgetService = new BudgetService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("Initializing BillManagementController...");

        // SAFETY CHECK: Only setup table columns if the table and columns exist
        if (billsTable != null && nameColumn != null) {
            setupTableColumns();
            loadBills();
        } else {
            System.out.println("Warning: Table or columns are null - skipping table setup");
            System.out.println("billsTable: " + (billsTable == null ? "null" : "ok"));
            System.out.println("nameColumn: " + (nameColumn == null ? "null" : "ok"));
        }

        // Still try to load summary data if labels exist
        if (totalBillsLabel != null) {
            loadSummaryData();
        }
    }

    private void setupTableColumns() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        dueDayColumn.setCellValueFactory(new PropertyValueFactory<>("dueDay"));
        frequencyColumn.setCellValueFactory(new PropertyValueFactory<>("frequency"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        addActionButtons();
    }

    private void addActionButtons() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button payButton = new Button("Mark Paid");
            private final Button deleteButton = new Button("Delete");

            {
                payButton.setStyle("-fx-background-color: #4ecca3; -fx-text-fill: #1a1a2e; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 5;");
                deleteButton.setStyle("-fx-background-color: #e94560; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 5;");

                payButton.setOnAction(event -> {
                    Bill bill = getTableView().getItems().get(getIndex());
                    if (bill.getStatus().equals("PAID")) {
                        billService.markAsUnpaid(bill.getId());
                    } else {
                        billService.markAsPaid(bill.getId());
                    }
                    loadBills();
                });

                deleteButton.setOnAction(event -> {
                    Bill bill = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Delete Bill");
                    confirm.setHeaderText("Delete " + bill.getName() + "?");
                    confirm.setContentText("This action cannot be undone.");
                    Optional<ButtonType> result = confirm.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        billService.deleteBill(bill.getId());
                        loadBills();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Bill bill = getTableView().getItems().get(getIndex());
                    payButton.setText(bill.getStatus().equals("PAID") ? "Mark Unpaid" : "Mark Paid");
                    HBox buttons = new HBox(8, payButton, deleteButton);
                    setGraphic(buttons);
                }
            }
        });
    }

    private void loadBills() {
        List<Bill> bills = billService.getAllBills();
        ObservableList<Bill> billList = FXCollections.observableArrayList(bills);
        billsTable.setItems(billList);
        updateSummary(bills);
    }

    private void loadSummaryData() {
        List<Bill> bills = billService.getAllBills();
        updateSummary(bills);
    }

    private void updateSummary(List<Bill> bills) {
        if (totalBillsLabel == null) return;

        int total = bills.size();
        long paid = bills.stream().filter(b -> b.getStatus().equals("PAID")).count();
        long unpaid = bills.stream().filter(b -> b.getStatus().equals("UNPAID")).count();
        long dueSoon = billService.getUpcomingBills(7).size();

        BigDecimal totalAmount = bills.stream().map(Bill::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paidAmount = bills.stream().filter(b -> b.getStatus().equals("PAID")).map(Bill::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unpaidAmount = bills.stream().filter(b -> b.getStatus().equals("UNPAID")).map(Bill::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        totalBillsLabel.setText(String.valueOf(total));
        totalAmountLabel.setText("$" + String.format("%.2f", totalAmount) + " / month");
        paidBillsLabel.setText(String.valueOf(paid));
        paidAmountLabel.setText("$" + String.format("%.2f", paidAmount));
        unpaidBillsLabel.setText(String.valueOf(unpaid));
        unpaidAmountLabel.setText("$" + String.format("%.2f", unpaidAmount));
        dueSoonLabel.setText(String.valueOf(dueSoon));
    }

    @FXML
    private void handleAddBill() {
        // Your existing add bill code...
        Dialog<Bill> dialog = new Dialog<>();
        dialog.setTitle("Add New Bill");
        dialog.setHeaderText("Enter bill details");

        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 20;");

        TextField nameField = new TextField();
        nameField.setPromptText("Bill name (e.g. Netflix)");

        TextField amountField = new TextField();
        amountField.setPromptText("Amount");

        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll("Subscription", "Housing", "Transport", "Utilities", "Insurance", "Healthcare", "Other");
        categoryBox.setPromptText("Category");

        ComboBox<Integer> dueDayBox = new ComboBox<>();
        for (int i = 1; i <= 28; i++) dueDayBox.getItems().add(i);
        dueDayBox.setPromptText("Due day of month");

        ComboBox<String> frequencyBox = new ComboBox<>();
        frequencyBox.getItems().addAll("Monthly", "Yearly");
        frequencyBox.setValue("Monthly");

        ComboBox<String> budgetBox = new ComboBox<>();
        budgetBox.getItems().add("None");
        List<Budget> budgets = budgetService.getAllBudgets();
        for (Budget b : budgets) budgetBox.getItems().add(b.getName() + " (" + b.getCategory() + ")");
        budgetBox.setValue("None");

        TextField descField = new TextField();
        descField.setPromptText("Description (optional)");

        content.getChildren().addAll(
                new Label("Bill Name:"), nameField,
                new Label("Amount:"), amountField,
                new Label("Category:"), categoryBox,
                new Label("Due Day:"), dueDayBox,
                new Label("Frequency:"), frequencyBox,
                new Label("Link to Budget:"), budgetBox,
                new Label("Description:"), descField
        );

        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(button -> {
            if (button == saveButton) {
                try {
                    String name = nameField.getText().trim();
                    BigDecimal amount = new BigDecimal(amountField.getText().trim());
                    String category = categoryBox.getValue();
                    int dueDay = dueDayBox.getValue();
                    String frequency = frequencyBox.getValue();
                    String description = descField.getText().trim();

                    Integer budgetId = null;
                    String selectedBudget = budgetBox.getValue();
                    if (!selectedBudget.equals("None")) {
                        int index = budgetBox.getSelectionModel().getSelectedIndex() - 1;
                        if (index >= 0 && index < budgets.size()) {
                            budgetId = budgets.get(index).getId();
                        }
                    }

                    return new Bill(name, amount, dueDay, frequency, category, description, budgetId);
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        });

        Optional<Bill> result = dialog.showAndWait();
        result.ifPresent(bill -> {
            billService.createBill(bill);
            loadBills();
        });
    }

    @FXML
    private void showWelcome() { navigateTo("/fxml/WelcomeScreen.fxml"); }
    @FXML
    private void showBudgets() { navigateTo("/fxml/BudgetManagement.fxml"); }
    @FXML
    private void showExpenses() { navigateTo("/fxml/ExpenseManagement.fxml"); }
    @FXML
    private void showConverter() { navigateTo("/fxml/CurrencyConverter.fxml"); }
    @FXML
    private void showBills() { loadBills(); }
    @FXML
    private void toggleNotifications() {}

    private void navigateTo(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Parent root = loader.load();
            Stage stage = (Stage) billsTable.getScene().getWindow();
            stage.setScene(new Scene(root, 1100, 750));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}