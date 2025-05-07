package com.stocksignal.indicators.technical;

import java.util.List;
import com.stocksignal.data.StockData;
import com.stocksignal.exceptions.ConfigurationException;
import com.stocksignal.exceptions.DataProcessingException;
import com.stocksignal.indicators.Indicator;

/**
 * Calculates the Simple Moving Average (SMA) based on the closing prices of the stock data.
 * <p>
 * The Simple Moving Average (SMA) is the unweighted mean of the previous `windowSize` closing prices of a stock.
 * It is commonly used to smooth price data to identify trends over a specified period of time.
 * </p>
 */
public class SMA implements Indicator {

    /** The number of data points (days) over which the moving average is calculated. */
    private final int windowSize;

    /**
     * Constructs a new SMA calculator with the specified window size.
     * <p>
     * The window size determines the period over which the moving average is calculated. It should be a positive number
     * to ensure valid calculations.
     * </p>
     *
     * @param windowSize the number of days over which to calculate the moving average (e.g., 50 for a 50-day SMA)
     * @throws IllegalArgumentException if windowSize is not positive.
     */
    public SMA(int windowSize) {
        if (windowSize <= 0) {
            throw new ConfigurationException("Window size must be more than 0");
        }        
        this.windowSize = windowSize;
    }

    /**
     * Calculates the SMA over the last `windowSize` entries in the provided stock data list.
     * <p>
     * This method calculates the average of the closing prices over the most recent `windowSize` days from the stock data.
     * </p>
     *
     * @param data the list of {@link StockData} objects (assumed to be sorted in ascending date order)
     * @return the calculated simple moving average.
     * @throws DataProcessingException if the data list is null, too small, or if the calculation fails.
     */
    @Override
    public double calculate(List<StockData> data) {
        // Ensure that the data list is not null and contains enough data points to calculate the SMA
        if (data == null || data.isEmpty()) {
            throw new DataProcessingException("Stock data is null or empty.");
        }

        // Check if there are enough data points to calculate the SMA
        if (data.size() < windowSize) {
            throw new DataProcessingException("Not enough data to calculate SMA. Required: " + windowSize + ", Available: " + data.size());
        }

        try {
            // Calculate the average of the closing prices over the last `windowSize` data points
            return data.subList(data.size() - windowSize, data.size()) // Get the last `windowSize` entries
                    .stream() // Stream the data
                    .mapToDouble(StockData::getClose) // Extract the closing prices
                    .average() // Calculate the average
                    .orElseThrow(() -> new DataProcessingException("Unable to calculate SMA: no values in window.")); // Handle empty window scenario
        } catch (Exception e) {
            // Catch any other errors during the calculation process and provide additional context
            throw new DataProcessingException("Error occurred while calculating SMA: " + e.getMessage(), e);
        }
    }
}
