package com.stocksignal.data;

import com.stocksignal.exceptions.ConfigurationException;
import com.stocksignal.exceptions.DataProcessingException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.Properties;

/**
 * Fetches stock data from Alpha Vantage, including daily price and earnings per share.
 */
public class AlphaVantageFetcher {

    private static final String BASE_URL = "https://www.alphavantage.co/query";
    private final String apiKey = loadApiKey();
    private final HttpClient client = HttpClient.newHttpClient();

    private String loadApiKey() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new ConfigurationException("Unable to find config.properties in resources.");
            }
            props.load(input);
            String key = props.getProperty("api.key");
            if (key == null || key.isBlank()) {
                throw new ConfigurationException("API key is missing in config.properties.");
            }
            return key;
        } catch (IOException e) {
            throw new ConfigurationException("Failed to load API key from config.properties", e);
        }
    }

    public StockData collectLatestDailyData(String symbol) {
        try {
            // Fetch daily price data
            String url = String.format("%s?function=TIME_SERIES_DAILY&symbol=%s&apikey=%s", BASE_URL, symbol, apiKey);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject json = new JSONObject(response.body());
            JSONObject timeSeries = json.getJSONObject("Time Series (Daily)");
            String latestDate = timeSeries.keys().next();
            JSONObject dayData = timeSeries.getJSONObject(latestDate);

            double open = Double.parseDouble(dayData.getString("1. open"));
            double high = Double.parseDouble(dayData.getString("2. high"));
            double low = Double.parseDouble(dayData.getString("3. low"));
            double close = Double.parseDouble(dayData.getString("4. close"));
            long volume = Long.parseLong(dayData.getString("5. volume"));

            // Fetch EPS values
            double[] epsValues = fetchAnnualEPS(symbol);
            double currentEPS = epsValues[0];
            double previousEPS = epsValues[1];

            return new StockData(
                    symbol,
                    LocalDate.parse(latestDate),
                    open,
                    close,
                    high,
                    low,
                    volume,
                    currentEPS,
                    previousEPS
            );

        } catch (Exception e) {
            throw new DataProcessingException("Failed to fetch or parse stock data for symbol: " + symbol, e);
        }
    }

    private double[] fetchAnnualEPS(String symbol) {
        try {
            String epsUrl = String.format("%s?function=EARNINGS&symbol=%s&apikey=%s", BASE_URL, symbol, apiKey);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(epsUrl))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject json = new JSONObject(response.body());
            JSONArray earnings = json.getJSONArray("annualEarnings");

            if (earnings.length() < 2) {
                throw new DataProcessingException("Insufficient annual earnings data for symbol: " + symbol);
            }

            double currentEPS = Double.parseDouble(earnings.getJSONObject(0).getString("reportedEPS"));
            double previousEPS = Double.parseDouble(earnings.getJSONObject(1).getString("reportedEPS"));

            return new double[]{currentEPS, previousEPS};
        } catch (Exception e) {
            throw new DataProcessingException("Failed to fetch EPS data for symbol: " + symbol, e);
        }
    }
}
