package com.stocksignal.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocksignal.exceptions.DataProcessingException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * {@code StockDataParser} is responsible for parsing JSON-formatted stock data
 * retrieved from an external API (e.g., Alpha Vantage) into a list of {@link StockData} objects.
 */
public class StockDataParser {

    /**
     * Parses the given JSON string into a list of {@link StockData} objects.
     *
     * @param json the raw JSON string returned by the stock data API
     * @return a list of {@link StockData} containing parsed daily trading information;
     *         returns an empty list if the JSON is invalid or data is missing
     */
    public static List<StockData> parse(String json) {
        // Call the overloaded method with default values for symbol and EPS
        return parse(json, null, 0.0, 0.0);
    }

    /**
     * Parses the given JSON string into a list of {@link StockData} objects, including earnings per share.
     *
     * @param json the raw JSON string returned by the stock data API
     * @param symbol the stock symbol to associate with each stock data object
     * @param currentEPS the current earnings per share for the stock
     * @param previousEPS the previous earnings per share for the stock
     * @return a list of {@link StockData} containing parsed daily trading information;
     *         returns an empty list if the JSON is invalid or data is missing
     */
    public static List<StockData> parse(String json, String symbol, double currentEPS, double previousEPS) {
        List<StockData> dataList = new ArrayList<>();

        try {
            // Create a new ObjectMapper instance to map the JSON data
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            // Access the "Time Series (Daily)" section of the response
            JsonNode timeSeries = root.get("Time Series (Daily)");

            if (timeSeries != null) {
                Iterator<String> dates = timeSeries.fieldNames();

                // Iterate over each date and extract stock data for that day
                while (dates.hasNext()) {
                    String date = dates.next();
                    JsonNode node = timeSeries.get(date);

                    // Extract stock data fields safely, using Optional for missing fields
                    Optional<Double> open = Optional.ofNullable(node.get("1. open")).map(JsonNode::asDouble);
                    Optional<Double> close = Optional.ofNullable(node.get("4. close")).map(JsonNode::asDouble);
                    Optional<Double> high = Optional.ofNullable(node.get("2. high")).map(JsonNode::asDouble);
                    Optional<Double> low = Optional.ofNullable(node.get("3. low")).map(JsonNode::asDouble);
                    Optional<Long> volume = Optional.ofNullable(node.get("5. volume")).map(JsonNode::asLong);

                    // Ensure all fields are present and valid
                    if (open.isPresent() && close.isPresent() && high.isPresent() && low.isPresent() && volume.isPresent()) {
                        // If no EPS values are passed, default them to 0.0
                        StockData data = new StockData(
                                (symbol != null) ? symbol : "",        // Use symbol if provided
                                LocalDate.parse(date),                 // Date parsed from the string
                                open.get(),                            // Stock opening price
                                close.get(),                           // Stock closing price
                                high.get(),                            // Highest price of the day
                                low.get(),                             // Lowest price of the day
                                volume.get(),                          // Volume of stocks traded
                                (symbol != null) ? currentEPS : 0.0,   // Current EPS, default if symbol is null
                                (symbol != null) ? previousEPS : 0.0   // Previous EPS, default if symbol is null
                        );

                        // Add the created StockData to the list
                        dataList.add(data);
                    }
                }
            }

        } catch (Exception e) {
            throw new DataProcessingException("Error parsing stock data: " + e.getMessage());
        }

        return dataList;
    }
}
