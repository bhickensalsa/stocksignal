package com.stocksignal;

import java.io.IOException;
import java.util.List;

import com.stocksignal.data.AlphaVantageClient;
import com.stocksignal.data.StockData;
import com.stocksignal.exceptions.ConfigurationException;
import com.stocksignal.exceptions.DataProcessingException;
import com.stocksignal.strategies.TrendFollowingStrategy;

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
        String symbol = "GOOGL"; // Symbol
        int smaPeriod = 200;
        List<StockData> historicalData;

        AlphaVantageClient fetcher = new AlphaVantageClient();

        try {
            historicalData = fetcher.fetchDailyStockData(symbol, smaPeriod + 1, false);
        } catch (IOException e) {
            throw new ConfigurationException("Couldnt fetch historical data. " + e.getMessage());
        }


        if (historicalData == null) {
            throw new DataProcessingException("Parsed stock data list is null");
        }
        if (historicalData.size() < smaPeriod) {
            throw new DataProcessingException(String.format("Warning: Only %d data points available, but %d requested.\n", historicalData.size(), smaPeriod));
        }

        TrendFollowingStrategy TFStrategy = new TrendFollowingStrategy(historicalData, smaPeriod);

        boolean buy = TFStrategy.shouldBuy();
        boolean sell = TFStrategy.shouldSell();
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
