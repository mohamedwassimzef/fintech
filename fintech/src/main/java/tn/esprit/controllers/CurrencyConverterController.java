package tn.esprit.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class CurrencyConverterController implements Initializable {

    private static final String API_KEY = "0259e85880-814428cf3c-tavrz0";
    private static final String BASE_URL = "https://api.fastforex.io";

    @FXML private TextField amountField;
    @FXML private ComboBox<String> fromCurrency;
    @FXML private ComboBox<String> toCurrency;
    @FXML private Label resultLabel;
    @FXML private Label errorLabel;
    @FXML private Label rateLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadCurrencies();
    }

    private void loadCurrencies() {
        new Thread(() -> {
            try {
                String response = get(BASE_URL + "/currencies?api_key=" + API_KEY);
                ObjectMapper mapper = new ObjectMapper();
                JsonNode json = mapper.readTree(response);
                JsonNode currencies = json.get("currencies");

                List<String> codes = new ArrayList<>();
                currencies.fieldNames().forEachRemaining(codes::add);
                Collections.sort(codes);

                Platform.runLater(() -> {
                    fromCurrency.setItems(FXCollections.observableArrayList(codes));
                    toCurrency.setItems(FXCollections.observableArrayList(codes));
                    fromCurrency.setValue("USD");
                    toCurrency.setValue("EUR");
                });

            } catch (Exception e) {
                Platform.runLater(() -> errorLabel.setText("Failed to load currencies: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleConvert() {
        errorLabel.setText("");
        resultLabel.setText("");
        rateLabel.setText("");

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
                String url = BASE_URL + "/convert?from=" + from + "&to=" + to
                        + "&amount=" + amount + "&api_key=" + API_KEY;
                String response = get(url);
                ObjectMapper mapper = new ObjectMapper();
                JsonNode json = mapper.readTree(response);

                double result = json.get("result").get(to).asDouble();
                double rate = result / amount;

                String resultText = String.format("%.2f %s = %.4f %s", amount, from, result, to);
                String rateText = String.format("1 %s = %.6f %s", from, rate, to);

                Platform.runLater(() -> {
                    resultLabel.setText(resultText);
                    rateLabel.setText(rateText);
                });

            } catch (Exception e) {
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

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream())
        );
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        return sb.toString();
    }
    @FXML
    private void showBills() {
        navigateTo("/fxml/BillManagement.fxml");
    }

    @FXML
    private void toggleNotifications() {}
}