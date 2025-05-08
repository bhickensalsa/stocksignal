package com.stocksignal;

import java.io.IOException;
import java.util.List;

import com.stocksignal.backtesting.BacktestEngine;
import com.stocksignal.data.AlphaVantageClient;
import com.stocksignal.data.DataPreprocessor;
import com.stocksignal.data.StockData;
import com.stocksignal.exceptions.ConfigurationException;
import com.stocksignal.exceptions.DataProcessingException;
import com.stocksignal.strategies.GoldenCrossStrategy;
//import com.stocksignal.utils.AppLogger;

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
        String symbol = "AAPL";  // The stock symbol to analyze (e.g., "GOOGL" for Alphabet Inc.)
        int dataListSize = 200;
        
        int shortPeriod = 50;
        int longPeriod = 200;
        List<StockData> historicalData;

        // Initialize AlphaVantageClient to fetch stock data
        AlphaVantageClient fetcher = new AlphaVantageClient();

        try {
            // Fetch historical stock data (e.g., the past 201 days for a 200-day SMA)
            historicalData = fetcher.fetchDailyStockData(symbol, dataListSize, false);
        } catch (IOException e) {
            // If there's an issue with network or the API, throw a ConfigurationException
            throw new ConfigurationException("Couldn't fetch historical data. " + e.getMessage());
        }

        // Clean the data
        DataPreprocessor preprocessor = new DataPreprocessor();
        List<StockData> cleanData = preprocessor.preprocess(historicalData, longPeriod);

        // Check if the historical data is null or has fewer data points than required
        if (cleanData == null) {
            throw new DataProcessingException("Parsed stock data list is null.");
        }
        if (cleanData.size() < longPeriod) {
            throw new DataProcessingException(String.format("Warning: Only %d data points available, but %d requested.\n", cleanData.size(), longPeriod));
        }

        // Initialize the Strategy
        GoldenCrossStrategy GCStrat = new GoldenCrossStrategy(cleanData, shortPeriod, longPeriod);

        /* GCStrat.calculateIndicators();
        AppLogger.addContext(symbol, symbol);
        if (GCStrat.shouldBuy()) {
            AppLogger.info("Buy" + symbol);
        } else if (GCStrat.shouldSell()) {
            AppLogger.info("Sell" + symbol);
        } else {
            AppLogger.info("Hold" + symbol);
        }
        AppLogger.clearContext(); */

        // Backtest
        BacktestEngine backtest = new BacktestEngine(GCStrat, cleanData, 2000, 2, true);
        
        // Run backtest
        backtest.runBacktest();
    }
}
