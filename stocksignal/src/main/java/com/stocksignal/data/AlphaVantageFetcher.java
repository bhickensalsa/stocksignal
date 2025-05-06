package com.stocksignal.data;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.Properties;

import org.json.JSONObject;


public class AlphaVantageFetcher {
    
    private static final String BASE_URL = "https://www.alphavantage.co/query";
    private final String apiKey = loadApiKey();
    private final HttpClient client = HttpClient.newHttpClient();

    private String loadApiKey() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("Unable to find config.properties in resources.");
            }
            props.load(input);
            return props.getProperty("api.key");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load API key from config.properties", e);
        }
    }

    /**
     * Fetches the most recent daily stock data for the specified symbol.
     *
     * @param symbol stock symbol (e.g., AAPL, TSLA)
     * @return StockData for the most recent trading day
     */
    public StockData collectLatestDailyData(String symbol) {
        try {
            String url = String.format("%s?function=TIME_SERIES_DAILY&symbol=%s&apikey=%s", BASE_URL, symbol, apiKey);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject json = new JSONObject(response.body());
            JSONObject timeSeries = json.getJSONObject("Time Series (Daily)");

            String latestDate = timeSeries.keys().next(); // This gives the most recent date
            JSONObject dayData = timeSeries.getJSONObject(latestDate);

            double open = Double.parseDouble(dayData.getString("1. open"));
            double high = Double.parseDouble(dayData.getString("2. high"));
            double low = Double.parseDouble(dayData.getString("3. low"));
            double close = Double.parseDouble(dayData.getString("4. close"));
            long volume = Long.parseLong(dayData.getString("5. volume"));

            return new StockData(
                    LocalDate.parse(latestDate),
                    open,
                    close,
                    high,
                    low,
                    volume
            );

        } catch (Exception e) {
            System.err.println("Failed to fetch stock data: " + e.getMessage());
            return null;
        }
    }
}
