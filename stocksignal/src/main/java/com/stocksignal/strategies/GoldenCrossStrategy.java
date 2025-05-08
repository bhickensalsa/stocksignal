package com.stocksignal.strategies;

import com.stocksignal.data.StockData;
import com.stocksignal.exceptions.ConfigurationException;
import com.stocksignal.exceptions.DataProcessingException;
import com.stocksignal.indicators.technical.SMA;
import java.util.List;

/**
 * A trading strategy based on the Golden Cross and Death Cross signals using Simple Moving Averages (SMA).
 *
 * <p>A Golden Cross occurs when a shorter-period SMA crosses above a longer-period SMA,
 * typically signaling a potential upward trend and a potential buy opportunity.
 * Conversely, a Death Cross occurs when the shorter-period SMA crosses below the longer-period SMA,
 * often indicating a potential downward trend and a potential sell opportunity.</p>
 *
 * <p>This strategy requires historical data sufficient to calculate the longest SMA for
 * both the current period and the immediately preceding period to detect crossovers.</p>
 */
public class GoldenCrossStrategy implements Strategy {

    /**
     * The complete historical stock data used to calculate indicators.
     * Assumed to be in chronological order (oldest to newest).
     */
    private List<StockData> historicalData;

    /**
     * The period (number of data points) for the short-term Simple Moving Average.
     * Must be less than the longPeriod.
     */
    private final int shortPeriod;

    /**
     * The period (number of data points) for the long-term Simple Moving Average.
     * Must be greater than the shortPeriod.
     */
    private final int longPeriod;

    /** The most recently calculated value of the short-term SMA. */
    private double currentShortSMA;

    /** The most recently calculated value of the long-term SMA. */
    private double currentLongSMA;

    /** The value of the short-term SMA for the period immediately preceding the current. */
    private double previousShortSMA;

    /** The value of the long-term SMA for the period immediately preceding the current. */
    private double previousLongSMA;

    /**
     * Constructs a GoldenCrossStrategy with specified SMA periods and initial historical data.
     *
     * <p>Initial data must be provided upon construction. The strategy requires enough
     * data points to calculate both the current and previous values for the longest SMA.</p>
     *
     * @param historicalData The initial list of stock data (must be in ascending chronological order).
     * @param shortPeriod    The period for the short-term SMA. Must be positive and less than longPeriod.
     * @param longPeriod     The period for the long-term SMA. Must be positive and greater than shortPeriod.
     * @throws ConfigurationException If the shortPeriod is not less than longPeriod, or if either period is not positive.
     * @throws DataProcessingException If the initial historical data is null, empty, or contains insufficient data
     * to perform the initial indicator calculation for both current and previous periods.
     */
    public GoldenCrossStrategy(List<StockData> historicalData, int shortPeriod, int longPeriod) {
        if (shortPeriod <= 0) {
            throw new ConfigurationException("Short period must be positive.");
        }
        if (longPeriod <= 0) {
            throw new ConfigurationException("Long period must be positive.");
        }
        if (shortPeriod >= longPeriod) {
            throw new ConfigurationException("Short period (" + shortPeriod + ") must be less than long period (" + longPeriod + ").");
        }

        // Minimum data required to calculate the previous longest SMA: longPeriod + 1 points.
        // This is needed to get the window ending at index size-2.
        int minimumRequiredData = longPeriod + 1;
        if (historicalData == null || historicalData.size() < minimumRequiredData) {
            throw new DataProcessingException("Insufficient initial data (" + (historicalData == null ? 0 : historicalData.size())
                                               + "). Required for previous " + longPeriod + "-day SMA: " + minimumRequiredData + " data points.");
        }

        // Make a defensive copy if you want to prevent external modification of the list
        // For this example, we'll use the provided list directly but it's a consideration.
        this.historicalData = historicalData;
        this.shortPeriod = shortPeriod;
        this.longPeriod = longPeriod;

        // Perform initial indicator calculation upon construction
        calculateIndicators();
    }

    /**
     * Updates the historical stock data used for indicator calculations.
     *
     * <p>Note: This implementation *replaces* the entire historical dataset with the new data.
     * For real-time or streaming applications, you might want to append new data and manage
     * the list size (e.g., by trimming old data).</p>
     *
     * @param newHistoricalData The new list of stock data to replace the current dataset.
     * Must be in ascending chronological order and contain sufficient data.
     * @throws DataProcessingException if the new data is null, empty, or contains insufficient data.
     */
    // Renamed and slightly modified from refreshHistoricalData for clarity on its role in updateData
    private void setHistoricalData(List<StockData> newHistoricalData) {
         int minimumRequiredData = longPeriod + 1;
         if (newHistoricalData == null || newHistoricalData.size() < minimumRequiredData) {
             throw new DataProcessingException("Insufficient new historical data (" + (newHistoricalData == null ? 0 : newHistoricalData.size())
                                                + "). Required for previous " + longPeriod + "-day SMA: " + minimumRequiredData + " data points.");
         }
        this.historicalData = newHistoricalData;
    }


    /**
     * Updates the internal dataset with new data and recalculates the technical indicators.
     * This method should be called whenever new market data becomes available.
     *
     * @param newData The latest list of stock data entries. This replaces the existing data.
     * Must contain sufficient data to recalculate indicators.
     * @throws DataProcessingException if the new data is insufficient or calculation fails.
     */
    @Override
    public void updateData(List<StockData> newData) {
        // In a real application with streaming data, you'd likely append newData to historicalData
        // and possibly trim historicalData to a maximum size.
        // For simplicity here, we replace the data entirely.
        setHistoricalData(newData);
        calculateIndicators();
    }

    /**
     * Returns the number of data points required to perform reliable indicator calculations
     * for both the current and previous periods.
     *
     * @return The minimum number of data points required, which is the long period plus one.
     */
    @Override
    public int getLookbackPeriod() {
        // Need longPeriod + 1 data points to calculate the longest SMA for both current (ending at size-1)
        // and previous (ending at size-2) periods.
        return longPeriod + 1;
    }

    /**
     * Calculates the current and previous SMAs based on the historical stock data.
     * This method updates the internal state variables (`currentShortSMA`, etc.).
     *
     * <p>Two sets of SMAs are calculated: one for the most recent period (ending at the latest data point),
     * and one for the period just before it (ending at the second-to-latest data point).
     * These values are used to detect Golden or Death Cross signals.</p>
     *
     * @throws DataProcessingException if any error occurs during SMA calculation, typically due to insufficient data.
     */
    @Override
    public void calculateIndicators() {
        try {
            // Instantiate SMA calculators
            SMA shortSMA = new SMA(shortPeriod);
            SMA longSMA = new SMA(longPeriod);

            int dataSize = historicalData.size();

            // Data validation - should ideally be caught by constructor or updateData,
            // but defensive check here is also good.
             int minimumRequiredData = longPeriod + 1;
            if (dataSize < minimumRequiredData) {
                 throw new DataProcessingException("Insufficient data (" + dataSize + ") to calculate indicators for current and previous periods. Required: " + minimumRequiredData);
            }

            // --- Calculate Current SMAs (ending at dataSize - 1) ---
            // Get the data window for the current short SMA
            List<StockData> currentDataShort = historicalData.subList(dataSize - shortPeriod, dataSize);
            // Get the data window for the current long SMA
            List<StockData> currentDataLong = historicalData.subList(dataSize - longPeriod, dataSize);

            currentShortSMA = shortSMA.calculate(currentDataShort);
            currentLongSMA = longSMA.calculate(currentDataLong);

            // --- Calculate Previous SMAs (ending at dataSize - 2) ---
            // Get the data window for the previous short SMA (ends at index dataSize - 2)
            List<StockData> previousDataShort = historicalData.subList(dataSize - shortPeriod - 1, dataSize - 1);
            // Get the data window for the previous long SMA (ends at index dataSize - 2)
            List<StockData> previousDataLong = historicalData.subList(dataSize - longPeriod - 1, dataSize - 1);

            previousShortSMA = shortSMA.calculate(previousDataShort);
            previousLongSMA = longSMA.calculate(previousDataLong);

        } catch (DataProcessingException e) {
            // Re-throw DataProcessingException with added context
            throw new DataProcessingException("Error calculating indicators: " + e.getMessage(), e);
        } catch (IndexOutOfBoundsException e) {
             // Catch potential index errors from subList, which should be prevented by data validation
             throw new DataProcessingException("Index out of bounds while calculating indicators. Data size: " + historicalData.size() +
                                                ", Short Period: " + shortPeriod + ", Long Period: " + longPeriod, e);
        } catch (Exception e) {
            // Catch any other unexpected exceptions
             throw new DataProcessingException("An unexpected error occurred during indicator calculation: " + e.getMessage(), e);
        }
    }

    /**
     * Determines if the strategy generates a buy signal based on the Golden Cross.
     *
     * <p>A buy signal is generated when the short-term SMA crosses above the long-term SMA.
     * This is detected by checking if the previous short SMA was less than or equal to the
     * previous long SMA, and the current short SMA is strictly greater than the current long SMA.</p>
     *
     * <p>Requires that {@link #calculateIndicators()} has been called to update SMA values
     * based on the latest data.</p>
     *
     * @return {@code true} if a Golden Cross occurred in the most recent period; {@code false} otherwise.
     */
    @Override
    public boolean shouldBuy() {
        // Assuming calculateIndicators() has been called by updateData()
        // Logic: Previous short SMA was below or equal to previous long SMA, AND
        //        Current short SMA is now above current long SMA.
        return previousShortSMA <= previousLongSMA && currentShortSMA > currentLongSMA;
    }

    /**
     * Determines if the strategy generates a sell signal based on the Death Cross.
     *
     * <p>A sell signal is generated when the short-term SMA crosses below the long-term SMA.
     * This is detected by checking if the previous short SMA was greater than or equal to the
     * previous long SMA, and the current short SMA is strictly less than the current long SMA.</p>
     *
     * <p>Requires that {@link #calculateIndicators()} has been called to update SMA values
     * based on the latest data.</p>
     *
     * @return {@code true} if a Death Cross occurred in the most recent period; {@code false} otherwise.
     */
    @Override
    public boolean shouldSell() {
        // Assuming calculateIndicators() has been called by updateData()
        // Logic: Previous short SMA was above or equal to previous long SMA, AND
        //        Current short SMA is now below current long SMA.
        return previousShortSMA >= previousLongSMA && currentShortSMA < currentLongSMA;
    }
}