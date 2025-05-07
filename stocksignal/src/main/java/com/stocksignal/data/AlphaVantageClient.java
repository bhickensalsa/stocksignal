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
        this.baseUrl = "https://www.alphavantage.co/query";
        this.client = new OkHttpClient();
    }

    /**
     * Loads the API key from the config.properties file located in the classpath resources.
     *
     * @return the API key as a string
     * @throws ConfigurationException if the configuration file is missing or invalid
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
     * @param dataPoints the number of most recent daily data points to return
     * @return a list of StockData representing the requested number of daily time series entries
     * @throws IOException if a network error occurs during the HTTP request
     * @throws DataProcessingException if the API response is invalid or parsing fails
     */
    public List<StockData> fetchDailyStockData(String symbol, int dataPoints) throws IOException {
        return fetchDailyStockData(symbol, dataPoints, false); // Default: don't include EPS
    }

    /**
     * Fetches daily time series stock data for the given symbol, with optional EPS data.
     * Automatically chooses between 'compact' (100 data points) and 'full' (full historical)
     * output size based on the requested dataPoints value.
     *
     * @param symbol the stock ticker symbol (e.g., "AAPL", "GOOGL")
     * @param dataPoints the number of most recent daily data points to return
     * @param includeEPS flag indicating whether to enrich stock data with EPS information
     * @return a list of StockData representing the requested number of daily time series entries
     * @throws IOException if a network error occurs during the HTTP request
     * @throws DataProcessingException if the API response is invalid or parsing fails
     */
    public List<StockData> fetchDailyStockData(String symbol, int dataPoints, boolean includeEPS) throws IOException {
        // Use 'full' for over 100 data points, otherwise use 'compact'
        String outputSize = dataPoints > 100 ? "full" : "compact";

        // Prepare the daily adjusted stock data request URL
        String url = String.format(
            "%s?function=TIME_SERIES_DAILY&symbol=%s&outputsize=%s&apikey=%s",
            baseUrl, symbol, outputSize, apiKey
        );

        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new DataProcessingException("API request failed: " + response.message());
            }

            String json = response.body().string();
            List<StockData> stockDataList;

            if (includeEPS) {
                // If EPS data is requested, make an additional API call to fetch earnings
                String epsUrl = String.format(
                    "%s?function=EARNINGS&symbol=%s&apikey=%s", baseUrl, symbol, apiKey
                );
                Request epsRequest = new Request.Builder().url(epsUrl).build();
                try (Response epsResponse = client.newCall(epsRequest).execute()) {
                    if (!epsResponse.isSuccessful()) {
                        throw new DataProcessingException("EPS API request failed: " + epsResponse.message());
                    }

                    String epsJson = epsResponse.body().string();
                    JSONObject epsData = new JSONObject(epsJson);
                    JSONArray earnings = epsData.getJSONArray("annualEarnings");

                    if (earnings.length() < 2) {
                        throw new DataProcessingException("Insufficient annual earnings data for symbol: " + symbol);
                    }

                    double currentEPS = Double.parseDouble(earnings.getJSONObject(0).getString("reportedEPS"));
                    double previousEPS = Double.parseDouble(earnings.getJSONObject(1).getString("reportedEPS"));

                    stockDataList = StockDataParser.parse(json, symbol, currentEPS, previousEPS);
                } catch (Exception e) {
                    throw new DataProcessingException("Failed to fetch EPS data for symbol: " + symbol, e);
                }
            } else {
                // Parse only the stock data if EPS is not required
                stockDataList = StockDataParser.parse(json);
            }

            // Return only the number of data points requested, up to the available amount
            return stockDataList.subList(0, Math.min(dataPoints, stockDataList.size()));
        }
    }
}
