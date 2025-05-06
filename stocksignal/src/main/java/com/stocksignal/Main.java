package com.stocksignal;

import com.stocksignal.data.AlphaVantageFetcher;
import com.stocksignal.data.StockData;

/**
 * The Main class is the entry point for running the Stock Signal application.
 * It initializes the application, fetches stock data, and prints relevant information to the console.
 */
public class Main {

    /**
     * The main method serves as the entry point to start the Stock Signal application.
     * It initializes the application by printing a start message, then collects stock data for a given symbol,
     * including current and previous earnings per share (EPS). After data collection, it prints out the stock details.
     * 
     * @param args command-line arguments (if any)
     */
    public static void main(String[] args) {
        // Initialize the fetcher to collect stock data from Alpha Vantage
        AlphaVantageFetcher fetcher = new AlphaVantageFetcher();
        
        // Collect the latest stock data for a specific symbol (e.g., GOOGL)
        StockData stockData = fetcher.collectLatestDailyData("GOOGL");

        // Check if the stock data was fetched successfully
        if (stockData != null) {
            // Print out the stock information to the console
            System.out.println("Stock Symbol: " + stockData.getSymbol());
            System.out.println("Date: " + stockData.getDate());
            System.out.println("Open: " + stockData.getOpen());
            System.out.println("Close: " + stockData.getClose());
            System.out.println("EPS (Current): " + stockData.getCurrentEarningsPerShare());
            System.out.println("EPS (Previous): " + stockData.getPreviousEarningsPerShare());
        } else {
            System.out.println("Failed to fetch stock data.");
        }
    }
}
