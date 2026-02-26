package tn.esprit.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class CurrencyService {

    private static final String API_KEY = "3f3326198a1b887a19dbe273";
    private static final String BASE_URL = "https://v6.exchangerate-api.com/v6/";

    /**
     * Convert amount from one currency to another
     */
    public BigDecimal convertCurrency(BigDecimal amount, String fromCurrency, String toCurrency) {
        try {
            if (fromCurrency.equals(toCurrency)) {
                return amount;
            }

            BigDecimal rate = getExchangeRate(fromCurrency, toCurrency);
            return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);

        } catch (Exception e) {
            System.out.println("Error converting currency: " + e.getMessage());
            e.printStackTrace();
            return amount;
        }
    }

    /**
     * Get exchange rate from one currency to another
     */
    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) throws IOException {
        String urlString = BASE_URL + API_KEY + "/latest/" + fromCurrency;

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.connect();

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("API returned error code: " + responseCode);
        }

        Scanner scanner = new Scanner(url.openStream());
        StringBuilder response = new StringBuilder();
        while (scanner.hasNext()) {
            response.append(scanner.nextLine());
        }
        scanner.close();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.toString());
        JsonNode rates = root.get("conversion_rates");
        double rate = rates.get(toCurrency).asDouble();

        return BigDecimal.valueOf(rate);
    }

    /**
     * Get all exchange rates for a base currency
     */
    public Map<String, BigDecimal> getAllRates(String baseCurrency) throws IOException {
        String urlString = BASE_URL + API_KEY + "/latest/" + baseCurrency;

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.connect();

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("API returned error code: " + responseCode);
        }

        Scanner scanner = new Scanner(url.openStream());
        StringBuilder response = new StringBuilder();
        while (scanner.hasNext()) {
            response.append(scanner.nextLine());
        }
        scanner.close();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.toString());
        JsonNode rates = root.get("conversion_rates");

        Map<String, BigDecimal> ratesMap = new HashMap<>();
        rates.fields().forEachRemaining(entry -> {
            ratesMap.put(entry.getKey(), BigDecimal.valueOf(entry.getValue().asDouble()));
        });

        return ratesMap;
    }
}