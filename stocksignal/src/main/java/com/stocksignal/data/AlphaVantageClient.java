package com.stocksignal.data;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import com.stocksignal.exceptions.DataProcessingException;

/**
 * AlphaVantageClient interacts with the Alpha Vantage API to retrieve stock data,
 * including daily time series and optional EPS (Earnings Per Share) data.
 * <p>
 * It uses OkHttpClient to make HTTP requests and parses JSON responses into StockData objects.
 * </p>
 */
public class AlphaVantageClient {

    private static final Logger logger = Logger.getLogger(AlphaVantageClient.class.getName());

    private final String apiKey;
    private final String baseUrl;
    private final OkHttpClient client;

    /**
     * Constructs a new AlphaVantageClient and loads the API key from config.properties.
     *
     * @throws DataProcessingException if the configuration is invalid or API key is missing
     */
    public AlphaVantageClient() {
        this.apiKey = loadApiKey();
        this.baseUrl = "https://www.alphavantage.co/query";
        this.client = new OkHttpClient();
    }

    /**
     * Loads the Alpha Vantage API key from a config.properties file in the classpath.
     *
     * @return the API key as a String
     * @throws DataProcessingException if the configuration file is missing or the key is not found
     */
    private String loadApiKey() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new DataProcessingException("Unable to find config.properties in resources.");
            }
            props.load(input);
            String key = props.getProperty("api.key");
            if (key == null || key.isBlank()) {
                throw new DataProcessingException("API key is missing in config.properties.");
            }
            logger.info("API key successfully loaded.");
            return key;
        } catch (IOException e) {
            throw new DataProcessingException("Failed to load API key from config.properties", e);
        }
    }

    /**
     * Fetches daily stock data for the given symbol.
     *
     * @param symbol     the stock ticker symbol (e.g., "AAPL", "GOOGL")
     * @param dataPoints the number of recent daily entries to retrieve
     * @return a list of StockData objects
     * @throws IOException               if a network error occurs
     * @throws DataProcessingException  if parsing or API call fails
     */
    public List<StockData> fetchDailyStockData(String symbol, int dataPoints) throws IOException {
        return fetchDailyStockData(symbol, dataPoints, false);
    }

    /**
     * Fetches daily stock data and optionally includes EPS information.
     *
     * @param symbol      the stock ticker symbol
     * @param dataPoints  number of daily entries to fetch
     * @param includeEPS  whether to include EPS data in the results
     * @return a list of StockData entries
     * @throws IOException              if network errors occur
     * @throws DataProcessingException if parsing or API response is invalid
     */
    public List<StockData> fetchDailyStockData(String symbol, int dataPoints, boolean includeEPS) throws IOException {
        String outputSize = dataPoints > 100 ? "full" : "compact";
        String url = String.format(
            "%s?function=TIME_SERIES_DAILY&symbol=%s&outputsize=%s&apikey=%s",
            baseUrl, symbol, outputSize, apiKey
        );
    
        Request request = new Request.Builder().url(url).build();
    
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new DataProcessingException("API request failed with HTTP code " + response.code() +
                    ": " + response.message());
            }
    
            String json = response.body().string();
            JSONObject jsonObject = new JSONObject(json);
    
            // Check for common error fields in response
            if (jsonObject.has("Error Message")) {
                throw new DataProcessingException("Invalid API call for symbol: " + symbol +
                    ". Reason: " + jsonObject.getString("Error Message"));
            }
            if (jsonObject.has("Note")) {
                throw new DataProcessingException("API limit reached or service unavailable: " + jsonObject.getString("Note"));
            }
            if (!jsonObject.has("Time Series (Daily)")) {
                throw new DataProcessingException("Missing 'Time Series (Daily)' data in API response for symbol: " + symbol);
            }
    
            List<StockData> stockDataList;
    
            if (includeEPS) {
                String epsUrl = String.format(
                    "%s?function=EARNINGS&symbol=%s&apikey=%s", baseUrl, symbol, apiKey
                );
                Request epsRequest = new Request.Builder().url(epsUrl).build();
                try (Response epsResponse = client.newCall(epsRequest).execute()) {
                    if (!epsResponse.isSuccessful()) {
                        throw new DataProcessingException("EPS API request failed with HTTP code " +
                            epsResponse.code() + ": " + epsResponse.message());
                    }
    
                    String epsJson = epsResponse.body().string();
                    JSONObject epsData = new JSONObject(epsJson);
    
                    if (!epsData.has("annualEarnings")) {
                        throw new DataProcessingException("Missing 'annualEarnings' in EPS API response for symbol: " + symbol);
                    }
    
                    JSONArray earnings = epsData.getJSONArray("annualEarnings");
                    if (earnings.length() < 2) {
                        throw new DataProcessingException("Insufficient EPS data (need at least 2 entries) for symbol: " + symbol);
                    }
    
                    double currentEPS = Double.parseDouble(earnings.getJSONObject(0).getString("reportedEPS"));
                    double previousEPS = Double.parseDouble(earnings.getJSONObject(1).getString("reportedEPS"));
    
                    stockDataList = StockDataParser.parse(json, symbol, currentEPS, previousEPS);
                } catch (Exception e) {
                    throw new DataProcessingException("Failed to fetch or parse EPS data for symbol: " + symbol, e);
                }
            } else {
                stockDataList = StockDataParser.parse(json);
            }
    
            if (stockDataList == null || stockDataList.isEmpty()) {
                throw new DataProcessingException("Parsed stock data is empty for symbol: " + symbol +
                    ". This may indicate malformed API response or unsupported symbol.");
            }

            // Process data before sending out
            DataPreprocessor preprocessor = new DataPreprocessor();
            return preprocessor.preprocess(stockDataList.subList(0, Math.min(dataPoints, stockDataList.size())));
        } catch (IOException e) {
            throw new IOException("Network error while fetching stock data for symbol: " + symbol, e);
        }
    }
    
}
