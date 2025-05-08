package com.stocksignal.strategies;

import com.stocksignal.data.StockData;
import com.stocksignal.exceptions.ConfigurationException;
import com.stocksignal.exceptions.DataProcessingException;
import com.stocksignal.indicators.technical.EMACalculator; // Import the EMA Calculator
import com.stocksignal.indicators.technical.RSI;

import java.util.ArrayList;
import java.util.List;

/**
 * A trading strategy combining Golden Cross/Death Cross signals using Exponential Moving Averages (EMA)
 * with the Relative Strength Index (RSI) for signal confirmation.
 *
 * <p>A buy signal is generated when a Golden Cross occurs (shorter-period EMA crosses above longer-period EMA)
 * AND the RSI is above a specified threshold (e.g., 50).
 * A sell signal is generated when a Death Cross occurs (shorter-period EMA crosses below longer-period EMA)
 * AND the RSI is below a specified threshold (e.g., 50).</p>
 *
 * <p>This strategy requires historical data sufficient to calculate the longest EMA and the RSI
 * for both the current period and potentially the immediately preceding period to detect crossovers.</p>
 */
public class GoldenCrossStrategy implements Strategy {

    /**
     * The historical stock data accumulated by the strategy.
     * Assumed to be in chronological order (oldest to newest).
     * This list will grow during backtesting as new data is added.
     */
    private List<StockData> historicalData;

    /**
     * The period (number of data points) for the shorter-term Exponential Moving Average.
     * Must be less than the longPeriod.
     */
    private final int shortPeriod;

    /**
     * The period (number of data points) for the longer-term Exponential Moving Average.
     * Must be greater than the shortPeriod.
     */
    private final int longPeriod;

    /** The period (number of data points) for the Relative Strength Index. */
    private final int rsiPeriod;

    /** The threshold for the RSI to confirm a buy signal. */
    private final double rsiBuyThreshold;

    /** The threshold for the RSI to confirm a sell signal. */
    private final double rsiSellThreshold;

    /** RSI indicator instance. */
    private final RSI rsiIndicator;

    /** The minimum number of historical data points required for this strategy to calculate indicators for the current *and* previous period. */
    private final int minimumRequiredData;

    /** The most recently calculated value of the shorter-term EMA. */
    private double currentShortEMA;

    /** The most recently calculated value of the longer-term EMA. */
    private double currentLongEMA;

    /** The most recently calculated value of the RSI. */
    private double currentRSI;

    /** The value of the shorter-term EMA for the period immediately preceding the current. */
    private double previousShortEMA;

    /** The value of the longer-term EMA for the period immediately preceding the current. */
    private double previousLongEMA;


    /**
     * Constructs a GoldenCrossStrategy with specified EMA and RSI periods and initial historical data.
     *
     * <p>Initial data must be provided upon construction. The strategy requires enough
     * data points to calculate all indicators for both the current and previous periods.</p>
     *
     * @param initialHistoricalData The initial list of stock data (must be in ascending chronological order).
     * Should be at least {@code getLookbackPeriod()} in size.
     * @param shortPeriod    The period for the shorter-term EMA. Must be positive and less than longPeriod.
     * @param longPeriod     The period for the longer-term EMA. Must be positive and greater than shortPeriod.
     * @param rsiPeriod      The period for the RSI. Must be positive.
     * @param rsiBuyThreshold The RSI threshold for confirming a buy signal (e.g., 50).
     * @param rsiSellThreshold The RSI threshold for confirming a sell signal (e.g., 50).
     * @throws ConfigurationException If any of the periods are non-positive or if shortPeriod >= longPeriod.
     * @throws DataProcessingException If the initial historical data is null, empty, or contains insufficient data
     * to perform the initial indicator calculations.
     */
    public GoldenCrossStrategy(List<StockData> initialHistoricalData, int shortPeriod, int longPeriod,
                               int rsiPeriod, double rsiBuyThreshold, double rsiSellThreshold) {
        // --- Validate Periods and Thresholds ---
        if (shortPeriod <= 0) {
            throw new ConfigurationException("Short period must be positive.");
        }
        if (longPeriod <= 0) {
            throw new ConfigurationException("Long period must be positive.");
        }
        if (shortPeriod >= longPeriod) {
            throw new ConfigurationException("Short period (" + shortPeriod + ") must be less than long period (" + longPeriod + ").");
        }
        if (rsiPeriod <= 0) {
            throw new ConfigurationException("RSI period must be positive.");
        }

        this.shortPeriod = shortPeriod;
        this.longPeriod = longPeriod;
        this.rsiPeriod = rsiPeriod;
        this.rsiBuyThreshold = rsiBuyThreshold;
        this.rsiSellThreshold = rsiSellThreshold;

        // --- Determine Minimum Required Data ---
        // To calculate the current EMA, need `period` points for the first EMA value,
        // and `period + 1` points to get the second EMA value (needed for previous EMA).
        // So, to get the current and previous EMA values from a series calculation,
        // the input data needs to be at least `period + 1` in size.
        // Minimum data required for previous longest EMA: longPeriod + 1.
        // Minimum data required for current RSI: rsiPeriod + 1.
        // The overall minimum data required is the maximum of these individual requirements.
        this.minimumRequiredData = Math.max(longPeriod + 1, rsiPeriod + 1);


        // --- Validate Initial Historical Data ---
        if (initialHistoricalData == null || initialHistoricalData.size() < minimumRequiredData) {
            throw new DataProcessingException("Insufficient initial data (" + (initialHistoricalData == null ? 0 : initialHistoricalData.size())
                                               + "). Required: " + minimumRequiredData + " data points for initial indicator calculations.");
        }

        // --- Initialize Indicators and Data ---
        this.historicalData = new ArrayList<>(initialHistoricalData); // Defensive copy
        // No need to instantiate SMA indicators anymore, we use EMACalculator directly
        this.rsiIndicator = new RSI(rsiPeriod);

        // Perform initial indicator calculation upon construction
        calculateIndicators();
    }

    /**
     * Updates the internal dataset with new data points and recalculates the technical indicators.
     * This method should be called whenever new market data becomes available,
     * typically with one new data point during a backtesting iteration.
     *
     * @param newData A list of new stock data entries to append. Should ideally be in chronological order.
     * @throws DataProcessingException if the calculation fails after adding new data.
     */
    @Override
    public void updateData(List<StockData> newData) {
        if (newData == null || newData.isEmpty()) {
            // Handle case with no new data, simply return.
            return;
        }

        // Append new data
        this.historicalData.addAll(newData);

        // Optional: Trim old data to manage memory, keeping enough history for the required lookback
        // plus an additional buffer.
        int requiredHistorySize = getLookbackPeriod() + 20; // Keep a buffer
        if (this.historicalData.size() > requiredHistorySize) {
            this.historicalData = new ArrayList<>(this.historicalData.subList(
                this.historicalData.size() - requiredHistorySize, this.historicalData.size()));
        }

        // Recalculate indicators based on the updated historical data.
        // This will use the latest data points added.
        calculateIndicators();
    }

    /**
     * Returns the minimum number of historical data points required to perform reliable indicator calculations
     * for all indicators (EMAs and RSI) for both the current and previous periods.
     *
     * @return The minimum number of data points required.
     */
    @Override
    public int getLookbackPeriod() {
         // The minimum required data is the maximum of the lookback periods for
        // the longest EMA needing current and previous values (longPeriod + 1)
        // and the RSI needing current value (rsiPeriod + 1).
        return Math.max(longPeriod + 1, rsiPeriod + 1);
    }

    /**
     * Calculates the current and previous EMAs and the current RSI based on the historical stock data.
     * This method updates the internal state variables (`currentShortEMA`, etc.).
     * It uses the *end* of the accumulated historical data list.
     *
     * @throws DataProcessingException if any error occurs during indicator calculation, typically due to insufficient data.
     */
    @Override
    public void calculateIndicators() {
        try {
            int dataSize = historicalData.size();

            // Data validation - ensure we have enough data after appending new points
            if (dataSize < minimumRequiredData) {
                 throw new DataProcessingException("Insufficient data (" + dataSize + ") to calculate indicators. Required: " + minimumRequiredData);
            }

            // --- Calculate EMA Series ---
            // Need enough data to calculate at least two EMA values for each period
            // to get the current and previous values.
            // The EMACalculator.calculateSeriesFromStockData requires at least `period` data points
            // to calculate the first EMA value. To get the second value (for the previous period),
            // it needs `period + 1` data points.

            // Calculate Short EMA Series
            if (dataSize < shortPeriod + 1) {
                throw new DataProcessingException("Insufficient data (" + dataSize + ") for short EMA series calculation. Required: " + (shortPeriod + 1));
            }
            List<Double> shortEmaSeries = EMACalculator.calculateSeriesFromStockData(historicalData.subList(dataSize - (shortPeriod + 1), dataSize), shortPeriod);
            this.currentShortEMA = shortEmaSeries.get(shortEmaSeries.size() - 1); // Last value is current EMA
            this.previousShortEMA = shortEmaSeries.get(shortEmaSeries.size() - 2); // Second to last is previous EMA


            // Calculate Long EMA Series
             if (dataSize < longPeriod + 1) {
                throw new DataProcessingException("Insufficient data (" + dataSize + ") for long EMA series calculation. Required: " + (longPeriod + 1));
            }
            List<Double> longEmaSeries = EMACalculator.calculateSeriesFromStockData(historicalData.subList(dataSize - (longPeriod + 1), dataSize), longPeriod);
            this.currentLongEMA = longEmaSeries.get(longEmaSeries.size() - 1); // Last value is current EMA
            this.previousLongEMA = longEmaSeries.get(longEmaSeries.size() - 2); // Second to last is previous EMA

            // --- Calculate Current RSI (ending at dataSize - 1) ---
            // RSI calculation requires rsiPeriod + 1 data points.
            if (dataSize < rsiPeriod + 1) {
                 throw new DataProcessingException("Insufficient data (" + dataSize + ") for RSI calculation. Required: " + (rsiPeriod + 1));
            }
            List<StockData> currentDataRsi = historicalData.subList(dataSize - (rsiPeriod + 1), dataSize);
            this.currentRSI = rsiIndicator.calculate(currentDataRsi);


        } catch (DataProcessingException e) {
            // Re-throw DataProcessingException with added context
            throw new DataProcessingException("Error calculating indicators: " + e.getMessage(), e);
        } catch (IndexOutOfBoundsException e) {
             // Catch potential index errors from subList or EMA series access. This indicates an issue with
            // the data size checks or sublist logic.
             throw new DataProcessingException("Index out of bounds while calculating indicators. Data size: " + historicalData.size() +
                                                ", Short Period: " + shortPeriod + ", Long Period: " + longPeriod + ", RSI Period: " + rsiPeriod +
                                                ", Required Minimum: " + minimumRequiredData, e);
        } catch (Exception e) {
            // Catch any other unexpected exceptions
             throw new DataProcessingException("An unexpected error occurred during indicator calculation: " + e.getMessage(), e);
        }
    }

    /**
     * Determines if the strategy generates a buy signal based on the Golden Cross (EMA crossover) confirmed by RSI.
     *
     * <p>A buy signal is generated when the shorter-term EMA crosses above the longer-term EMA
     * AND the current RSI is above the specified buy threshold.</p>
     *
     * <p>Requires that {@link #calculateIndicators()} has been called to update indicator values
     * based on the latest data. This method should only be called when there is sufficient data
     * in the strategy's historical data list (i.e., at least {@code getLookbackPeriod()} points).</p>
     *
     * @return {@code true} if a buy signal is generated; {@code false} otherwise.
     * @throws DataProcessingException if there is insufficient historical data to determine the signal.
     */
    @Override
    public boolean shouldBuy() {
         // Ensure we have enough data to make a decision.
         if (historicalData == null || historicalData.size() < minimumRequiredData) {
             throw new DataProcessingException("Insufficient historical data (" + (historicalData == null ? 0 : historicalData.size())
                                                + ") to determine buy signal. Required: " + minimumRequiredData);
        }

        // Check Buy Conditions:
        // 1. Golden Cross (EMA crossover) occurred in the most recent period
        boolean goldenCross = previousShortEMA <= previousLongEMA && currentShortEMA > currentLongEMA;

        // 2. Current RSI is above the buy threshold
        boolean rsiConfirms = currentRSI > rsiBuyThreshold;

        return goldenCross && rsiConfirms;
    }

    /**
     * Determines if the strategy generates a sell signal based on the Death Cross (EMA crossover) confirmed by RSI.
     *
     * <p>A sell signal is generated when the shorter-term EMA crosses below the longer-term EMA
     * AND the current RSI is below the specified sell threshold.</p>
     *
     * <p>Requires that {@link #calculateIndicators()} has been called to update indicator values
     * based on the latest data. This method should only be called when there is sufficient data
     * in the strategy's historical data list (i.e., at least {@code getLookbackPeriod()} points).</p>
     *
     * @return {@code true} if a sell signal is generated; {@code false} otherwise.
     * @throws DataProcessingException if there is insufficient historical data to determine the signal.
     */
    @Override
    public boolean shouldSell() {
         // Ensure we have enough data to make a decision.
         if (historicalData == null || historicalData.size() < minimumRequiredData) {
             throw new DataProcessingException("Insufficient historical data (" + (historicalData == null ? 0 : historicalData.size())
                                                + ") to determine sell signal. Required: " + minimumRequiredData);
        }

        // Check Sell Conditions:
        // 1. Death Cross (EMA crossover) occurred in the most recent period
        boolean deathCross = previousShortEMA >= previousLongEMA && currentShortEMA < currentLongEMA;

        // 2. Current RSI is below the sell threshold
        boolean rsiConfirms = currentRSI < rsiSellThreshold;

        return deathCross && rsiConfirms;
    }
}