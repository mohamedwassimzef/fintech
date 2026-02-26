package tn.esprit.controllers;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import javafx.application.Platform;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.layout.Pane;
import javafx.scene.control.Alert;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.scene.Node;
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
    @FXML
    private Label newsTicker;
    @FXML
    private Pane tickerPane;
    @FXML
    private Label healthScoreLabel;

    @FXML
    private Label healthStatusLabel;
    private BudgetService budgetService;
    private ExpenseService expenseService;
    @FXML
    private VBox patternContainer;


    private List<String> notificationHistory = new ArrayList<>();
    private boolean notifPanelVisible = false;

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
        loadNews();
        loadHealthScore();
        loadSpendingPatterns();
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

        // Update labels - ONLY SET TEXT, NO STYLES
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

        // Add data to pie chart - ONLY DATA, NO STYLES
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

            // Create budget status card - NO STYLES, just basic JavaFX controls
            VBox card = new VBox(5);

            Label nameLabel = new Label(budget.getName());

            Label amountLabel = new Label(String.format("$%.2f / $%.2f",
                    budget.getSpentAmount(), budget.getAmount()));

            ProgressBar progressBar = new ProgressBar(utilization / 100.0);
            progressBar.setPrefWidth(350);

            Label statusLabel = new Label(status + " - " + String.format("%.1f%%", utilization));

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
        loadHealthScore();
        loadSpendingPatterns();
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

    @FXML
    private void showConverter() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CurrencyConverter.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) dateLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 1000, 700));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadNews() {
        new Thread(() -> {
            try {
                String apiKey = "ea5e36f1d7614484b6850ed19f1732e9";
                String url = "https://newsapi.org/v2/everything?q=finance+budget+money&language=en&pageSize=10&apiKey=" + apiKey;

                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                ObjectMapper mapper = new ObjectMapper();
                JsonNode json = mapper.readTree(sb.toString());
                JsonNode articles = json.get("articles");

                List<String> headlines = new ArrayList<>();
                for (JsonNode article : articles) {
                    String title = article.get("title").asText();
                    if (!title.contains("[Removed]")) {
                        headlines.add(title);
                    }
                }

                Platform.runLater(() -> startTicker(headlines));

            } catch (Exception e) {
                Platform.runLater(() -> newsTicker.setText("Could not load news: " + e.getMessage()));
            }
        }).start();
    }

    private void startTicker(List<String> headlines) {
        if (headlines.isEmpty()) return;

        // Show first headline immediately
        newsTicker.setText("🔹 " + headlines.stream().reduce((a, b) -> a + "          🔸          " + b).orElse(""));

        // Wait for layout to compute width
        tickerPane.layoutBoundsProperty().addListener((obs, oldVal, newVal) -> {
            double paneWidth = newVal.getWidth();
            double textWidth = newsTicker.prefWidth(-1);

            if (paneWidth <= 0) return;

            // Start off screen to the right
            newsTicker.setTranslateX(paneWidth);

            TranslateTransition transition = new TranslateTransition(Duration.seconds(headlines.size() * 8), newsTicker);
            transition.setFromX(paneWidth);
            transition.setToX(-textWidth);
            transition.setCycleCount(TranslateTransition.INDEFINITE);
            transition.setInterpolator(javafx.animation.Interpolator.LINEAR);
            transition.play();
        });
    }

    private void loadHealthScore() {
        List<Budget> budgets = budgetService.getAllBudgets();

        if (budgets.isEmpty()) {
            healthScoreLabel.setText("N/A");
            healthStatusLabel.setText("No data yet");
            return;
        }

        double totalScore = 0;
        int count = 0;

        for (Budget budget : budgets) {
            double utilization = budgetService.getBudgetUtilizationPercentage(budget.getId());

            if (utilization <= 50) totalScore += 100;
            else if (utilization <= 75) totalScore += 80;
            else if (utilization <= 90) totalScore += 60;
            else if (utilization <= 100) totalScore += 40;
            else totalScore += 10; // overspent

            count++;
        }

        int finalScore = (int) (totalScore / count);

        String status;

        if (finalScore >= 80) {
            status = "🟢 Excellent!";
        } else if (finalScore >= 60) {
            status = "🟡 Good";
        } else if (finalScore >= 40) {
            status = "🟠 Fair";
        } else {
            status = "🔴 Poor";
        }

        healthScoreLabel.setText(finalScore + "/100");
        healthStatusLabel.setText(status);
    }

    private void loadSpendingPatterns() {
        List<Expense> expenses = expenseService.getAllExpenses();
        patternContainer.getChildren().clear();

        if (expenses.isEmpty()) {
            addPatternLabel("📭 No expenses yet to analyze.");
            return;
        }

        // Pattern 1: Biggest spending category
        Map<String, BigDecimal> categoryTotals = new HashMap<>();
        for (Expense expense : expenses) {
            String category = expense.getCategory();
            categoryTotals.put(category,
                    categoryTotals.getOrDefault(category, BigDecimal.ZERO).add(expense.getAmount()));
        }
        String biggestCategory = categoryTotals.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("N/A");
        BigDecimal biggestAmount = categoryTotals.getOrDefault(biggestCategory, BigDecimal.ZERO);
        BigDecimal totalSpent = categoryTotals.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        double biggestPercent = totalSpent.compareTo(BigDecimal.ZERO) > 0
                ? biggestAmount.divide(totalSpent, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100")).doubleValue()
                : 0;
        addPatternLabel("🏆 Your biggest spending category is " + biggestCategory +
                " (" + String.format("%.1f%%", biggestPercent) + " of total spending)");

        // Pattern 2: Most expensive day of week
        Map<String, BigDecimal> dayTotals = new HashMap<>();
        for (Expense expense : expenses) {
            String day = expense.getExpenseDate().getDayOfWeek().toString();
            dayTotals.put(day,
                    dayTotals.getOrDefault(day, BigDecimal.ZERO).add(expense.getAmount()));
        }
        String busiestDay = dayTotals.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("N/A");
        addPatternLabel("📅 You spend the most on " + busiestDay + "s");

        // Pattern 3: Average expense
        BigDecimal average = totalSpent.divide(new BigDecimal(expenses.size()), 2, BigDecimal.ROUND_HALF_UP);
        addPatternLabel("💰 Your average expense is $" + average);

        // Pattern 4: Overspending warning
        long overspentBudgets = budgetService.getAllBudgets().stream()
                .filter(b -> budgetService.getBudgetUtilizationPercentage(b.getId()) > 100)
                .count();
        if (overspentBudgets > 0) {
            addPatternLabel("⚠️ You have " + overspentBudgets + " overspent budget(s) — consider reducing expenses!");
        } else {
            addPatternLabel("✅ All budgets are under control — great job!");
        }

        // Pattern 5: Most frequent category
        Map<String, Long> categoryCount = new HashMap<>();
        for (Expense expense : expenses) {
            String category = expense.getCategory();
            categoryCount.put(category, categoryCount.getOrDefault(category, 0L) + 1);
        }
        String mostFrequent = categoryCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("N/A");
        long frequency = categoryCount.getOrDefault(mostFrequent, 0L);
        addPatternLabel("🔁 You make the most transactions in " + mostFrequent +
                " (" + frequency + " times)");
    }

    private void addPatternLabel(String text) {
        Label label = new Label("• " + text);
        label.setWrapText(true);
        label.setMaxWidth(780);
        patternContainer.getChildren().add(label);
    }
    @FXML
    private void showNotifications() {
        List<String> notifications = new ArrayList<>();

        List<Budget> budgets = budgetService.getAllBudgets();
        List<Expense> expenses = expenseService.getAllExpenses();

        // Check 1: Overspent budgets
        long overspent = budgets.stream()
                .filter(b -> budgetService.getBudgetUtilizationPercentage(b.getId()) > 100)
                .count();
        if (overspent > 0) {
            notifications.add("🔴 WARNING: " + overspent + " budget(s) are overspent!");
        }

        // Check 2: Budgets near limit (>80%)
        long nearLimit = budgets.stream()
                .filter(b -> {
                    double u = budgetService.getBudgetUtilizationPercentage(b.getId());
                    return u >= 80 && u <= 100;
                })
                .count();
        if (nearLimit > 0) {
            notifications.add("🟠 ALERT: " + nearLimit + " budget(s) are above 80% usage!");
        }

        // Check 3: No expenses recently
        boolean hasRecentExpense = expenses.stream()
                .anyMatch(e -> e.getExpenseDate().isAfter(LocalDate.now().minusDays(3)));
        if (!hasRecentExpense && !expenses.isEmpty()) {
            notifications.add("💡 TIP: You haven't logged any expenses in the last 3 days.");
        }

        // Check 4: All good
        if (overspent == 0 && nearLimit == 0) {
            notifications.add("✅ Great job! All budgets are under control.");
        }

        // Check 5: No budgets
        if (budgets.isEmpty()) {
            notifications.add("💡 TIP: You have no budgets yet. Create one to start tracking!");
        }

        // Check 6: Health score warning
        if (budgets.size() > 0) {
            double totalUtil = budgets.stream()
                    .mapToDouble(b -> budgetService.getBudgetUtilizationPercentage(b.getId()))
                    .average().orElse(0);
            if (totalUtil > 75) {
                notifications.add("📊 Your average budget usage is " + String.format("%.1f%%", totalUtil) + " — consider reducing spending.");
            }
        }

        // Build and show popup
        String message = notifications.isEmpty()
                ? "No notifications at this time."
                : String.join("\n\n", notifications);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Notifications");
        alert.setHeaderText("Your Financial Alerts");
        alert.setContentText(message);
        alert.showAndWait();
    }
    @FXML
    private void showBills(ActionEvent event) {  // Add ActionEvent parameter
        try {
            System.out.println("Loading BillManagement.fxml...");
            System.out.println("Resource URL: " + getClass().getResource("/fxml/BillManagement.fxml"));

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/BillManagement.fxml"));
            Parent root = loader.load();

            // Get the current stage from the event source
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load BillManagement.fxml");
        }
    }
    }

