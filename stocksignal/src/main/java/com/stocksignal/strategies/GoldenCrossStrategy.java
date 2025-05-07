package com.stocksignal.strategies;

import com.stocksignal.data.StockData;
import com.stocksignal.exceptions.ConfigurationException;
import com.stocksignal.exceptions.DataProcessingException;
import com.stocksignal.indicators.technical.SMA;

import java.util.List;

/**
 * A trading strategy based on the Golden Cross signal using simple moving averages (SMA).
 *
 * <p>A Golden Cross occurs when a short-term SMA crosses above a long-term SMA,
 * indicating a potential upward trend. Conversely, a Death Cross occurs when the
 * short-term SMA crosses below the long-term SMA, signaling a potential downward trend.
 * This strategy generates buy signals on a Golden Cross and sell signals on a Death Cross.</p>
 */
public class GoldenCrossStrategy implements Strategy {

    /** Historical stock data used to calculate indicators. */
    private List<StockData> historicalData;

    /** Short-term SMA period (e.g., 50 days). */
    private final int shortPeriod;

    /** Long-term SMA period (e.g., 200 days). */
    private final int longPeriod;

    /** Most recent short-term SMA. */
    private double currentShortSMA;

    /** Most recent long-term SMA. */
    private double currentLongSMA;

    /** Previous short-term SMA (used for detecting crossovers). */
    private double previousShortSMA;

    /** Previous long-term SMA (used for detecting crossovers). */
    private double previousLongSMA;

    /**
     * Constructs a GoldenCrossStrategy using the specified SMA periods and historical stock data.
     *
     * @param historicalData the stock data to use (must be in chronological order).
     * @param shortPeriod the short-term SMA period.
     * @param longPeriod the long-term SMA period.
     * @throws ConfigurationException if shortPeriod is not less than longPeriod.
     * @throws DataProcessingException if there is insufficient data for the longest SMA calculation.
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
     * Updates the historical stock data used for indicator calculations.
     *
     * @param newHistoricalData new stock data to replace the current dataset.
     */
    public void refreshHistoricalData(List<StockData> newHistoricalData) {
        this.historicalData = newHistoricalData;
    }

    /**
     * Updates the internal dataset and recalculates the technical indicators.
     *
     * <p>This should be called whenever new market data becomes available.</p>
     *
     * @param newData latest stock data entries.
     */
    @Override
    public void updateData(List<StockData> newData) {
        refreshHistoricalData(newData);
        calculateIndicators();
    }

    /**
     * Returns the number of data points required to perform reliable indicator calculations.
     *
     * @return the greater of the short-term and long-term SMA periods.
     */
    @Override
    public int getLookbackPeriod() {
        return Math.max(shortPeriod, longPeriod);
    }

    /**
     * Calculates current and previous SMAs based on the historical stock data.
     *
     * <p>Two sets of SMAs are calculated: one for the most recent period, and one for the period just before it.
     * These values are used to detect Golden or Death Cross signals.</p>
     *
     * @throws DataProcessingException if any error occurs during SMA calculation.
     */
    @Override
    public void calculateIndicators() {
        try {
            SMA shortSMA = new SMA(shortPeriod);
            SMA longSMA = new SMA(longPeriod);

            List<StockData> recentData = historicalData.subList(historicalData.size() - longPeriod - 1, historicalData.size() - 1);
            List<StockData> previousData = historicalData.subList(historicalData.size() - longPeriod - 1, historicalData.size() - 1);

            currentShortSMA = shortSMA.calculate(recentData);
            currentLongSMA = longSMA.calculate(recentData);
            previousShortSMA = shortSMA.calculate(previousData);
            previousLongSMA = longSMA.calculate(previousData);
        } catch (DataProcessingException e) {
            throw new DataProcessingException("Error calculating indicators: " + e.getMessage());
        }
    }

    /**
     * Determines if the strategy generates a buy signal based on the Golden Cross.
     *
     * <p>A buy signal is generated when the short-term SMA crosses above the long-term SMA.</p>
     *
     * @return {@code true} if a Golden Cross occurred; {@code false} otherwise.
     */
    @Override
    public boolean shouldBuy() {
        calculateIndicators();
        return previousShortSMA <= previousLongSMA && currentShortSMA > currentLongSMA;
    }

    /**
     * Determines if the strategy generates a sell signal based on the Death Cross.
     *
     * <p>A sell signal is generated when the short-term SMA crosses below the long-term SMA.</p>
     *
     * @return {@code true} if a Death Cross occurred; {@code false} otherwise.
     */
    @Override
    public boolean shouldSell() {
        calculateIndicators();
        return previousShortSMA >= previousLongSMA && currentShortSMA < currentLongSMA;
    }
}
