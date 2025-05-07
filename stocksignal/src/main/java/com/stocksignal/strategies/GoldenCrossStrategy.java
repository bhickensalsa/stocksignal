package com.stocksignal.strategies;

import com.stocksignal.data.StockData;
import com.stocksignal.exceptions.ConfigurationException;
import com.stocksignal.exceptions.DataProcessingException;
import com.stocksignal.indicators.technical.SMA;

import java.util.List;

/**
 * A strategy that uses the Golden Cross signal to determine buy and sell opportunities.
 * <p>
 * A Golden Cross occurs when a short-term moving average crosses above a long-term moving average.
 * A Death Cross (used for selling) occurs when the short-term moving average crosses below the long-term moving average.
 * This strategy signals a buy when a Golden Cross occurs and a sell when a Death Cross occurs.
 * </p>
 */
public class GoldenCrossStrategy implements Strategy {

    /** The list of historical stock data used to calculate the indicators. */
    private final List<StockData> historicalData;

    /** The short-term period for the Simple Moving Average (SMA), typically set to 50 days. */
    private final int shortPeriod;

    /** The long-term period for the Simple Moving Average (SMA), typically set to 200 days. */
    private final int longPeriod;

    /** The current short-term SMA value calculated from the most recent data. */
    private double currentShortSMA;

    /** The current long-term SMA value calculated from the most recent data. */
    private double currentLongSMA;

    /** The previous short-term SMA value calculated from the data prior to the most recent. */
    private double previousShortSMA;

    /** The previous long-term SMA value calculated from the data prior to the most recent. */
    private double previousLongSMA;

    /**
     * Constructs a GoldenCrossStrategy with the specified historical data and moving average periods.
     * <p>
     * The short-period SMA must be less than the long-period SMA. The historical data must be large enough
     * to support both the short and long period calculations.
     * </p>
     *
     * @param historicalData the list of historical stock data (must be sorted by date in ascending order).
     * @param shortPeriod    the short-term period for the SMA (e.g., 50).
     * @param longPeriod     the long-term period for the SMA (e.g., 200).
     * @throws ConfigurationException if the short period is not less than the long period.
     * @throws DataProcessingException if there is not enough data to evaluate the strategy.
     */
    public GoldenCrossStrategy(List<StockData> historicalData, int shortPeriod, int longPeriod) {
        if (shortPeriod >= longPeriod) {
            throw new ConfigurationException("Short period must be less than long period.");
        }
        if (historicalData == null || historicalData.size() < longPeriod + 1) {
            throw new DataProcessingException("Not enough data to evaluate strategy.");
        }
        this.historicalData = historicalData;
        this.shortPeriod = shortPeriod;
        this.longPeriod = longPeriod;
    }

    /**
     * Calculates the short-term and long-term SMAs (Simple Moving Averages) for the most recent data and the previous data.
     * <p>
     * This method calculates the SMAs using the most recent `longPeriod` stock data for the current values,
     * and the previous `longPeriod` stock data for the previous values.
     * </p>
     * 
     * @throws DataProcessingException if there is an error during the SMA calculation.
     */
    @Override
    public void calculateIndicators() {
        try {
            SMA shortSMA = new SMA(shortPeriod);
            SMA longSMA = new SMA(longPeriod);

            // Use the most recent data for the current SMA calculations
            List<StockData> recentData = historicalData.subList(historicalData.size() - longPeriod, historicalData.size());
            currentShortSMA = shortSMA.calculate(recentData);
            currentLongSMA = longSMA.calculate(recentData);

            // Use the previous data for the previous SMA calculations
            List<StockData> previousData = historicalData.subList(historicalData.size() - longPeriod - 1, historicalData.size() - 1);
            previousShortSMA = shortSMA.calculate(previousData);
            previousLongSMA = longSMA.calculate(previousData);
        } catch (DataProcessingException e) {
            // Re-throw with additional context to indicate the error occurred during indicator calculation
            throw new DataProcessingException("Error calculating indicators: " + e.getMessage());
        }
    }

    /**
     * Determines whether the strategy signals a buy opportunity based on the Golden Cross.
     * <p>
     * A buy signal is generated when the short-term SMA crosses above the long-term SMA, 
     * indicating a potential upward trend (Golden Cross).
     * </p>
     *
     * @return true if a buy signal is generated, false otherwise.
     */
    @Override
    public boolean shouldBuy() {
        // Ensure indicators are up to date before making a decision
        calculateIndicators();
        
        // A Golden Cross occurs when the current short-term SMA crosses above the current long-term SMA
        return previousShortSMA <= previousLongSMA && currentShortSMA > currentLongSMA;
    }

    /**
     * Determines whether the strategy signals a sell opportunity based on the Death Cross.
     * <p>
     * A sell signal is generated when the short-term SMA crosses below the long-term SMA,
     * indicating a potential downward trend (Death Cross).
     * </p>
     *
     * @return true if a sell signal is generated, false otherwise.
     */
    @Override
    public boolean shouldSell() {
        // Ensure indicators are up to date before making a decision
        calculateIndicators();

        // A Death Cross occurs when the current short-term SMA crosses below the current long-term SMA
        return previousShortSMA >= previousLongSMA && currentShortSMA < currentLongSMA;
    }
}
