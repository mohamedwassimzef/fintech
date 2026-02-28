package tn.esprit.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class CurrencyConverterController implements Initializable {

    private static final String API_KEY = "0259e85880-814428cf3c-tavrz0";
    private static final String BASE_URL = "https://api.fastforex.io";  // No /v1 needed

    @FXML private TextField amountField;
    @FXML private ComboBox<String> fromCurrency;
    @FXML private ComboBox<String> toCurrency;
    @FXML private Label resultLabel;
    @FXML private Label errorLabel;
    @FXML private Label rateLabel;
    @FXML private Label changeLabel;
    @FXML private Label lastUpdateLabel;
    @FXML private LineChart<String, Number> historyChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    private double previousRate = 0;
    private Timeline refreshTimeline;
    private Map<String, String> currencyNames = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadCurrencies();

        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(30), e -> {
            if (fromCurrency.getValue() != null && toCurrency.getValue() != null) {
                fetchLiveRate();
            }
        }));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();

        fromCurrency.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> {
                    if (newVal != null && toCurrency.getValue() != null) {
                        fetchLiveRate();
                        loadHistoricalRates();
                    }
                });

        toCurrency.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> {
                    if (newVal != null && fromCurrency.getValue() != null) {
                        fetchLiveRate();
                        loadHistoricalRates();
                    }
                });
    }

    private void loadCurrencies() {
        new Thread(() -> {
            try {
                // CORRECT endpoint for currencies
                String response = get(BASE_URL + "/currencies?api_key=" + API_KEY);
                System.out.println("Currencies response: " + response);

                ObjectMapper mapper = new ObjectMapper();
                JsonNode json = mapper.readTree(response);
                JsonNode currencies = json.get("currencies");

                List<String> codes = new ArrayList<>();
                Iterator<Map.Entry<String, JsonNode>> fields = currencies.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    String code = entry.getKey();
                    String name = entry.getValue().asText();
                    codes.add(code);
                    currencyNames.put(code, name);
                }
                Collections.sort(codes);

                Platform.runLater(() -> {
                    fromCurrency.setItems(FXCollections.observableArrayList(codes));
                    toCurrency.setItems(FXCollections.observableArrayList(codes));
                    fromCurrency.setValue("USD");
                    toCurrency.setValue("EUR");

                    // Set cell factory to show full names
                    fromCurrency.setCellFactory(lv -> new ListCell<String>() {
                        @Override
                        protected void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            setText(empty || item == null ? null :
                                    item + " - " + currencyNames.getOrDefault(item, ""));
                        }
                    });

                    toCurrency.setCellFactory(lv -> new ListCell<String>() {
                        @Override
                        protected void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            setText(empty || item == null ? null :
                                    item + " - " + currencyNames.getOrDefault(item, ""));
                        }
                    });
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> errorLabel.setText("Failed to load currencies: " + e.getMessage()));
            }
        }).start();
    }

    private void fetchLiveRate() {
        String from = fromCurrency.getValue();
        String to = toCurrency.getValue();

        if (from == null || to == null) return;

        new Thread(() -> {
            try {
                // CORRECT endpoint for live rate - using fetch-one
                String urlStr = BASE_URL + "/fetch-one?from=" + from + "&to=" + to + "&api_key=" + API_KEY;
                System.out.println("Calling: " + urlStr);

                String response = get(urlStr);
                System.out.println("Rate response: " + response);

                ObjectMapper mapper = new ObjectMapper();
                JsonNode json = mapper.readTree(response);

                double currentRate = json.get("result").get(to).asDouble();

                Platform.runLater(() -> {
                    rateLabel.setText(String.format("1 %s = %.6f %s", from, currentRate, to));
                    lastUpdateLabel.setText("Last updated: " + new Date().toString());

                    if (previousRate > 0) {
                        double change = currentRate - previousRate;
                        double changePercent = (change / previousRate) * 100;

                        String changeText = String.format("%.6f (%.4f%%)", change, changePercent);

                        if (change > 0) {
                            changeLabel.setText("▲ " + changeText);
                            changeLabel.setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;");
                        } else if (change < 0) {
                            changeLabel.setText("▼ " + changeText);
                            changeLabel.setStyle("-fx-text-fill: #f44336; -fx-font-weight: bold;");
                        } else {
                            changeLabel.setText("• " + changeText);
                            changeLabel.setStyle("-fx-text-fill: #888;");
                        }
                    }

                    previousRate = currentRate;
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> errorLabel.setText("Rate fetch failed: " + e.getMessage()));
            }
        }).start();
    }

    private void loadHistoricalRates() {
        String from = fromCurrency.getValue();
        String to = toCurrency.getValue();

        if (from == null || to == null) return;

        new Thread(() -> {
            try {
                // Get last 30 days of data (using multiple requests since free tier might have limits)
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName(from + " to " + to);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Calendar cal = Calendar.getInstance();

                // Get data for last 30 days (simplified - in production use historical endpoint)
                for (int i = 30; i >= 0; i--) {
                    cal.add(Calendar.DAY_OF_MONTH, -1);
                    String date = sdf.format(cal.getTime());

                    // For demo, we'll use approximate rates
                    // In production, use: /historical?date=2024-01-01&from=USD&to=EUR&api_key=KEY
                    double rate = getApproximateRate(from, to, i);
                    String shortDate = date.substring(5);
                    series.getData().add(new XYChart.Data<>(shortDate, rate));
                }

                Platform.runLater(() -> {
                    historyChart.getData().clear();
                    historyChart.getData().add(series);
                    historyChart.setTitle(from + " to " + to + " - Last 30 Days");
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Temporary method for demo - replace with actual historical API
    private double getApproximateRate(String from, String to, int daysAgo) {
        // This is just for demonstration - in production use real historical data
        Random rand = new Random();
        double baseRate = from.equals("USD") && to.equals("EUR") ? 0.92 : 1.0;
        return baseRate + (rand.nextDouble() - 0.5) * 0.05;
    }

    @FXML
    private void handleConvert() {
        errorLabel.setText("");
        resultLabel.setText("");

        String amountText = amountField.getText().trim();
        String from = fromCurrency.getValue();
        String to = toCurrency.getValue();

        if (amountText.isEmpty() || from == null || to == null) {
            errorLabel.setText("Please fill in all fields.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            errorLabel.setText("Invalid amount. Please enter a number.");
            return;
        }

        resultLabel.setText("Converting...");

        new Thread(() -> {
            try {
                // CORRECT endpoint for conversion
                String url = BASE_URL + "/convert?from=" + from + "&to=" + to
                        + "&amount=" + amount + "&api_key=" + API_KEY;
                System.out.println("Convert URL: " + url);

                String response = get(url);
                System.out.println("Convert response: " + response);

                ObjectMapper mapper = new ObjectMapper();
                JsonNode json = mapper.readTree(response);

                double result = json.get("result").get(to).asDouble();

                String resultText = String.format("%.2f %s = %.4f %s", amount, from, result, to);

                Platform.runLater(() -> {
                    resultLabel.setText(resultText);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    errorLabel.setText("Conversion failed: " + e.getMessage());
                    resultLabel.setText("");
                });
            }
        }).start();
    }

    @FXML
    private void showWelcome() {
        navigateTo("/fxml/WelcomeScreen.fxml");
    }

    @FXML
    private void showBudgets() {
        navigateTo("/fxml/BudgetManagement.fxml");
    }

    @FXML
    private void showExpenses() {
        navigateTo("/fxml/ExpenseManagement.fxml");
    }

    @FXML
    private void showConverter() {
        // Already here
    }

    @FXML
    private void showBills() {
        navigateTo("/fxml/BillManagement.fxml");
    }

    @FXML
    private void toggleNotifications() {}

    private void navigateTo(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) amountField.getScene().getWindow();
            stage.setScene(new Scene(root, 1000, 700));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String get(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestProperty("Accept", "application/json");

        int responseCode = conn.getResponseCode();
        System.out.println("Response Code: " + responseCode);

        if (responseCode != 200) {
            BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream())
            );
            StringBuilder errorSb = new StringBuilder();
            String errorLine;
            while ((errorLine = errorReader.readLine()) != null) {
                errorSb.append(errorLine);
            }
            errorReader.close();
            throw new Exception("HTTP " + responseCode + ": " + errorSb.toString());
        }

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream())
        );
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }

    public void shutdown() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
    }
}