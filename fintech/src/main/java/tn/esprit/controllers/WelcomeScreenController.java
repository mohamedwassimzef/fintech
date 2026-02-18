package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.Budget;
import tn.esprit.entities.Expense;
import tn.esprit.services.BudgetService;
import tn.esprit.services.ExpenseService;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WelcomeScreenController {

    @FXML
    private Label dateLabel;

    @FXML
    private Label totalBudgetsLabel;

    @FXML
    private Label totalBudgetAmountLabel;

    @FXML
    private Label totalSpentLabel;

    @FXML
    private Label expenseCountLabel;

    @FXML
    private Label remainingLabel;

    @FXML
    private Label percentageLabel;

    @FXML
    private PieChart categoryPieChart;

    @FXML
    private VBox budgetStatusContainer;

    private BudgetService budgetService;
    private ExpenseService expenseService;

    public WelcomeScreenController() {
        this.budgetService = new BudgetService();
        this.expenseService = new ExpenseService();
    }

    @FXML
    public void initialize() {
        // Set current date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy");
        dateLabel.setText(LocalDate.now().format(formatter));

        // Load data
        loadStatistics();
        loadCategoryChart();
        loadBudgetStatus();
    }

    private void loadStatistics() {
        List<Budget> budgets = budgetService.getAllBudgets();
        List<Expense> expenses = expenseService.getAllExpenses();

        // Calculate totals
        int budgetCount = budgets.size();
        BigDecimal totalBudgetAmount = BigDecimal.ZERO;
        BigDecimal totalSpent = BigDecimal.ZERO;

        for (Budget budget : budgets) {
            totalBudgetAmount = totalBudgetAmount.add(budget.getAmount());
            totalSpent = totalSpent.add(budget.getSpentAmount());
        }

        BigDecimal remaining = totalBudgetAmount.subtract(totalSpent);
        double percentage = totalBudgetAmount.compareTo(BigDecimal.ZERO) > 0
                ? totalSpent.divide(totalBudgetAmount, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100")).doubleValue()
                : 0.0;

        // Update labels
        totalBudgetsLabel.setText(String.valueOf(budgetCount));
        totalBudgetAmountLabel.setText(String.format("$%.2f budgeted", totalBudgetAmount));
        totalSpentLabel.setText(String.format("$%.2f", totalSpent));
        expenseCountLabel.setText(expenses.size() + " expenses");
        remainingLabel.setText(String.format("$%.2f", remaining));
        percentageLabel.setText(String.format("%.1f%% used", percentage));
    }

    private void loadCategoryChart() {
        List<Expense> expenses = expenseService.getAllExpenses();

        // Group expenses by category
        Map<String, BigDecimal> categoryTotals = new HashMap<>();
        for (Expense expense : expenses) {
            String category = expense.getCategory();
            categoryTotals.put(category,
                    categoryTotals.getOrDefault(category, BigDecimal.ZERO).add(expense.getAmount()));
        }

        // Add data to pie chart
        categoryPieChart.getData().clear();
        for (Map.Entry<String, BigDecimal> entry : categoryTotals.entrySet()) {
            PieChart.Data slice = new PieChart.Data(
                    entry.getKey() + " ($" + String.format("%.2f", entry.getValue()) + ")",
                    entry.getValue().doubleValue()
            );
            categoryPieChart.getData().add(slice);
        }
    }

    private void loadBudgetStatus() {
        List<Budget> budgets = budgetService.getAllBudgets();
        budgetStatusContainer.getChildren().clear();

        for (Budget budget : budgets) {
            double utilization = budgetService.getBudgetUtilizationPercentage(budget.getId());
            String status = budgetService.getBudgetStatus(budget.getId());

            // Create budget status card
            VBox card = new VBox(5);
            card.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 8; -fx-padding: 10;");

            Label nameLabel = new Label(budget.getName());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            Label amountLabel = new Label(String.format("$%.2f / $%.2f",
                    budget.getSpentAmount(), budget.getAmount()));
            amountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

            ProgressBar progressBar = new ProgressBar(utilization / 100.0);
            progressBar.setPrefWidth(350);

            // Color based on status
            String barColor = status.equals("SAFE") ? "#4CAF50" :
                    status.equals("WARNING") ? "#FF9800" : "#f44336";
            progressBar.setStyle("-fx-accent: " + barColor + ";");

            Label statusLabel = new Label(status + " - " + String.format("%.1f%%", utilization));
            statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + barColor + ";");

            card.getChildren().addAll(nameLabel, amountLabel, progressBar, statusLabel);
            budgetStatusContainer.getChildren().add(card);
        }
    }

    @FXML
    private void showWelcome() {
        // Already on welcome screen
        loadStatistics();
        loadCategoryChart();
        loadBudgetStatus();
    }

    @FXML
    private void showBudgets() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/BudgetManagement.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) dateLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 1000, 700));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showExpenses() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ExpenseManagement.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) dateLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 1000, 700));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}