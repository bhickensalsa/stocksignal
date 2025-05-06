package com.stocksignal.data;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONObject;

import com.stocksignal.exceptions.ConfigurationException;
import com.stocksignal.exceptions.DataProcessingException;

/**
 * AlphaVantageClient is responsible for interacting with the Alpha Vantage API
 * to retrieve stock market data, including daily time series and EPS data.
 */
public class AlphaVantageClient {

    private final String apiKey;
    private final String baseUrl;
    private final OkHttpClient client;

    public AlphaVantageClient() {
        this.apiKey = loadApiKey();
        this.baseUrl = "https://www.alphavantage.co/query";  // Assuming a fixed base URL
        this.client = new OkHttpClient();
    }

    /**
     * Loads the API key from a configuration file.
     */
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

    /**
     * Fetches daily time series stock data for the given symbol, without EPS data.
     *
     * @param symbol the stock ticker symbol (e.g., "AAPL", "GOOGL")
     * @return a list of StockData representing the daily time series data
     * @throws IOException if a network error occurs during the HTTP request
     * @throws DataProcessingException if the response is not successful or fails to be parsed
     */
    public List<StockData> fetchDailyStockData(String symbol) throws IOException {
        return fetchDailyStockData(symbol, false);  // Default: Don't include EPS
    }

    /**
     * Fetches daily time series stock data for the given symbol, with optional EPS data.
     *
     * @param symbol the stock ticker symbol (e.g., "AAPL", "GOOGL")
     * @param includeEPS flag indicating whether to include EPS data
     * @return a list of StockData representing the daily time series data
     * @throws IOException if a network error occurs during the HTTP request
     * @throws DataProcessingException if the response is not successful or fails to be parsed
     */
    public List<StockData> fetchDailyStockData(String symbol, boolean includeEPS) throws IOException {
        String url = String.format("%s?function=TIME_SERIES_DAILY&symbol=%s&apikey=%s", baseUrl, symbol, apiKey);

        // Create the HTTP request using OkHttpClient
        Request request = new Request.Builder().url(url).build();

        // Execute the request and handle the response
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new DataProcessingException("API request failed: " + response.message());
            }

            // Parse the JSON response body into a list of StockData objects
            String json = response.body().string();

            List<StockData> stockDataList;

            if (includeEPS) {
                // Fetch EPS data
                String epsUrl = String.format("%s?function=EARNINGS&symbol=%s&apikey=%s", baseUrl, symbol, apiKey);
                Request epsRequest = new Request.Builder().url(epsUrl).build();
                try (Response epsResponse = client.newCall(epsRequest).execute()) {
                    if (!epsResponse.isSuccessful()) {
                        throw new DataProcessingException("EPS API request failed: " + epsResponse.message());
                    }

                    // Parse EPS response JSON
                    String epsJson = epsResponse.body().string();
                    JSONObject epsData = new JSONObject(epsJson);
                    JSONArray earnings = epsData.getJSONArray("annualEarnings");

                    if (earnings.length() < 2) {
                        throw new DataProcessingException("Insufficient annual earnings data for symbol: " + symbol);
                    }

                    double currentEPS = Double.parseDouble(earnings.getJSONObject(0).getString("reportedEPS"));
                    double previousEPS = Double.parseDouble(earnings.getJSONObject(1).getString("reportedEPS"));

                    // Parse stock data with EPS values included
                    stockDataList = StockDataParser.parse(json, symbol, currentEPS, previousEPS);
                } catch (Exception e) {
                    throw new DataProcessingException("Failed to fetch EPS data for symbol: " + symbol, e);
                }
            } else {
                // Parse stock data without EPS
                stockDataList = StockDataParser.parse(json);
            }

            return stockDataList;
        }
    }
}
