package com.stocksignal;

import java.io.IOException;
import java.util.List;

import com.stocksignal.data.AlphaVantageClient;
import com.stocksignal.data.StockData;
import com.stocksignal.exceptions.ConfigurationException;
import com.stocksignal.exceptions.DataProcessingException;
import com.stocksignal.strategies.TrendFollowingStrategy;

/**
 * The Main class serves as the entry point for running the Stock Signal application.
 * It initializes the application, fetches stock data, and evaluates whether to buy, sell, or hold based on the 
 * Trend Following Strategy.
 * <p>
 * This class demonstrates how to use the AlphaVantageClient to fetch stock data and then apply a trading strategy 
 * based on simple moving averages (SMA) and trend-following techniques.
 * </p>
 */
public class Main {

    /**
     * The main method serves as the entry point to start the Stock Signal application.
     * It initializes the application by fetching stock data for a given symbol, applies a Trend Following Strategy,
     * and prints out whether to buy, sell, or hold.
     * 
     * @param args command-line arguments (if any)
     */
    public static void main(String[] args) {
        String symbol = "GOOGL";  // The stock symbol to analyze (e.g., "GOOGL" for Alphabet Inc.)
        int smaPeriod = 200;      // The period for Simple Moving Average (SMA) to be used for trend-following
        List<StockData> historicalData;

        // Initialize AlphaVantageClient to fetch stock data
        AlphaVantageClient fetcher = new AlphaVantageClient();

        try {
            // Fetch historical stock data (e.g., the past 201 days for a 200-day SMA)
            historicalData = fetcher.fetchDailyStockData(symbol, smaPeriod + 1, false);
        } catch (IOException e) {
            // If there's an issue with network or the API, throw a ConfigurationException
            throw new ConfigurationException("Couldn't fetch historical data. " + e.getMessage());
        }

        // Check if the historical data is null or has fewer data points than required
        if (historicalData == null) {
            throw new DataProcessingException("Parsed stock data list is null.");
        }
        if (historicalData.size() < smaPeriod) {
            throw new DataProcessingException(String.format("Warning: Only %d data points available, but %d requested.\n", historicalData.size(), smaPeriod));
        }

        // Initialize the TrendFollowingStrategy with the historical data and the SMA period
        TrendFollowingStrategy TFStrategy = new TrendFollowingStrategy(historicalData, smaPeriod);

        // Check if the strategy signals a buy or sell
        boolean buy = TFStrategy.shouldBuy();
        boolean sell = TFStrategy.shouldSell();

        // Print the decision to the console
        if (buy) {
            System.out.println("Should buy: " + buy);
        }
        if (sell) {
            System.out.println("Should sell: " + sell);
        } else {
            System.out.println("Hold");
        }
    }
}
