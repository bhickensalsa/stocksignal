package com.stocksignal.strategies;

import com.stocksignal.data.StockData;
import com.stocksignal.exceptions.ConfigurationException;
import com.stocksignal.exceptions.DataProcessingException;
import com.stocksignal.indicators.technical.MACD;
import com.stocksignal.indicators.technical.SMA;

import java.util.List;

/**
 * A trend-following trading strategy combining a Simple Moving Average (SMA)
 * for trend identification and the Moving Average Convergence Divergence (MACD)
 * indicator for momentum and signal generation.
 *
 * <p>This strategy generates:</p>
 * <ul>
 * <li>A buy signal when the stock price is above the SMA (confirming an uptrend)
 * AND the MACD line crosses above the MACD signal line (indicating bullish momentum).</li>
 * <li>A sell signal when the stock price is below the SMA (confirming a downtrend)
 * AND the MACD line crosses below the MACD signal line (indicating bearish momentum).</li>
 * </ul>
 *
 * <p>The strategy requires sufficient historical data to calculate both indicators accurately
 * for the current period and the immediately preceding period to detect crossovers.</p>
 */
public class TrendFollowingStrategy implements Strategy {

    /**
     * The complete historical stock data used to calculate indicators.
     * Assumed to be in chronological order (oldest to newest).
     */
    private List<StockData> historicalData;

    /** Period for the Simple Moving Average (SMA). */
    private final int smaPeriod;

    /** Fast period for the MACD's EMA calculation. */
    private final int macdFastPeriod;

    /** Slow period for the MACD's EMA calculation. */
    private final int macdSlowPeriod;

    /** Signal period for the MACD's signal line EMA calculation. */
    private final int macdSignalPeriod;

    /** MACD indicator instance. */
    private final MACD macd;

    /** SMA indicator instance. */
    private final SMA sma;

    /** The minimum number of historical data points required for this strategy. */
    private final int minimumRequiredData;

    /** The most recently calculated value of the Simple Moving Average (SMA). */
    private double currentSMA;

    /** The most recently calculated value of the MACD line. */
    private double currentMacdLine;

    /** The most recently calculated value of the MACD signal line. */
    private double currentMacdSignal;

    /** The value of the MACD line for the period immediately preceding the current. */
    private double previousMacdLine;

    /** The value of the MACD signal line for the period immediately preceding the current. */
    private double previousMacdSignal;

    /**
     * Constructs a TrendFollowingStrategy with specified parameters and initial historical data.
     *
     * <p>Initial data must be provided upon construction. The strategy requires enough
     * data points to calculate all indicators for both the current and previous periods
     * to enable crossover detection.</p>
     *
     * @param historicalData     The initial list of stock data (must be in ascending chronological order).
     * @param smaPeriod          The period for the SMA calculation (e.g., 200 for long-term trend analysis). Must be positive.
     * @param macdFastPeriod     The fast period for the MACD (e.g., 12). Must be positive and less than macdSlowPeriod.
     * @param macdSlowPeriod     The slow period for the MACD (e.g., 26). Must be positive and greater than macdFastPeriod.
     * @param macdSignalPeriod   The signal period for the MACD (e.g., 9). Must be positive.
     * @throws ConfigurationException If any of the periods are non-positive or if MACD fast period >= slow period.
     * @throws DataProcessingException If the initial historical data is null, empty, contains insufficient data,
     * or is not in chronological order.
     */
    public TrendFollowingStrategy(List<StockData> historicalData, int smaPeriod,
                                  int macdFastPeriod, int macdSlowPeriod, int macdSignalPeriod) {

        // --- Validate Indicator Periods ---
        if (smaPeriod <= 0) {
            throw new ConfigurationException("SMA period must be positive.");
        }
        if (macdFastPeriod <= 0) {
            throw new ConfigurationException("MACD fast period must be positive.");
        }
        if (macdSlowPeriod <= 0) {
            throw new ConfigurationException("MACD slow period must be positive.");
        }
        if (macdSignalPeriod <= 0) {
            throw new ConfigurationException("MACD signal period must be positive.");
        }
        if (macdFastPeriod >= macdSlowPeriod) {
            throw new ConfigurationException("MACD fast period (" + macdFastPeriod + ") must be less than MACD slow period (" + macdSlowPeriod + ").");
        }

        this.smaPeriod = smaPeriod;
        this.macdFastPeriod = macdFastPeriod;
        this.macdSlowPeriod = macdSlowPeriod;
        this.macdSignalPeriod = macdSignalPeriod;

        // --- Determine Minimum Required Data ---
        // The minimum data needed for MACD current + previous values is slowPeriod + signalPeriod.
        // We need enough data for the longest lookback of any indicator.
        this.minimumRequiredData = Math.max(smaPeriod, macdSlowPeriod + macdSignalPeriod);

        // --- Validate Historical Data ---
        if (historicalData == null || historicalData.size() < minimumRequiredData) {
            throw new DataProcessingException("Insufficient initial data (" + (historicalData == null ? 0 : historicalData.size())
                                               + "). Required: " + minimumRequiredData + " data points.");
        }


        // --- Initialize Indicators and Data ---
        // Consider a defensive copy if you want to ensure the original list isn't modified externally
        this.historicalData = historicalData; // Using the provided list directly
        this.sma = new SMA(smaPeriod);
        this.macd = new MACD(macdFastPeriod, macdSlowPeriod, macdSignalPeriod);

        // Perform initial indicator calculation upon construction
        calculateIndicators();
    }

    /**
     * Updates the historical stock data used for recalculating indicators.
     * Validates the new data before replacing the existing dataset.
     *
     * <p>Note: This implementation *replaces* the entire historical dataset with the new data.
     * For real-time or streaming applications, you would typically append new data and manage
     * the list size (e.g., by trimming old data) while ensuring the history is sufficient
     * for the required lookback period.</p>
     *
     * @param newHistoricalData The new list of stock data to replace the current dataset.
     * Must be in ascending chronological order and contain sufficient data.
     * @throws DataProcessingException if the new data is null, empty, contains insufficient data,
     * or is not in chronological order.
     */
    private void setHistoricalData(List<StockData> newHistoricalData) {
         if (newHistoricalData == null || newHistoricalData.size() < minimumRequiredData) {
             throw new DataProcessingException("Insufficient new historical data (" + (newHistoricalData == null ? 0 : newHistoricalData.size())
                                                + "). Required: " + minimumRequiredData + " data points.");
         }
        this.historicalData = newHistoricalData;
    }


    /**
     * Updates the internal dataset with new data and recalculates the indicators.
     * This method should be called whenever new market data is available to ensure
     * the strategy uses the latest data for decision making.
     *
     * @param newData The latest list of stock data entries. This replaces the existing data.
     * Must contain sufficient data to recalculate indicators.
     * @throws DataProcessingException if the new data is insufficient or calculation fails.
     */
    @Override
    public void updateData(List<StockData> newData) {
        // In a real application with streaming data, you'd typically append newData to historicalData
        // and possibly trim historicalData to a maximum size to manage memory,
        // while ensuring the size remains >= minimumRequiredData.
        // For simplicity here, we replace the data entirely.
        setHistoricalData(newData);
        calculateIndicators();
    }

    /**
     * Returns the minimum number of historical data points required for this strategy
     * to calculate all indicators reliably for both the current and previous periods.
     * This is determined by the indicator with the longest lookback requirement.
     *
     * @return The minimum number of data points required.
     */
    @Override
    public int getLookbackPeriod() {
        return minimumRequiredData;
    }

    /**
     * Calculates the current and previous SMA and MACD indicators using the historical data.
     * This method updates the internal state variables (`currentSMA`, `currentMacdLine`, etc.).
     *
     * <p>Two sets of MACD values (line and signal) are calculated: one for the most recent period
     * (ending at the latest data point), and one for the period just before it (ending at
     * the second-to-latest data point). These are used to detect MACD crossovers.</p>
     *
     * @throws DataProcessingException if there is an issue with the data processing or if there is insufficient data.
     */
    @Override
    public void calculateIndicators() {
        try {
            int dataSize = historicalData.size();

            // Data validation - primarily done in constructor/setHistoricalData,
            // but defensive check here is also good.
            if (dataSize < minimumRequiredData) {
                 throw new DataProcessingException("Insufficient data (" + dataSize + ") to calculate indicators. Required: " + minimumRequiredData);
            }

            // --- Calculate Current SMA (ending at dataSize - 1) ---
            // SMA needs the last 'smaPeriod' data points
            List<StockData> currentDataForSma = historicalData.subList(dataSize - smaPeriod, dataSize);
            this.currentSMA = sma.calculate(currentDataForSma);

            // --- Calculate MACD for the Current Period (ending at dataSize - 1) ---
            // MACD needs enough data for its EMAs and signal line EMA to calculate the *last* value.
            // The list passed to MACD.calculate should be at least macdSlowPeriod + macdSignalPeriod long
            // to get reliable last values. We'll pass the full data tail required by the strategy.
            List<StockData> currentDataForMacd = historicalData.subList(dataSize - minimumRequiredData, dataSize);
            double[] currentMacdValues = macd.calculate(currentDataForMacd, true);
            this.currentMacdLine = currentMacdValues[0];
            this.currentMacdSignal = currentMacdValues[1];

            // --- Calculate MACD for the Previous Period (ending at dataSize - 2) ---
            // We need a data window ending one step earlier.
            // Ensure the list has at least minimumRequiredData + 1 points to get the window ending at size - 2
             if (dataSize < minimumRequiredData + 1) {
                 // This case should ideally be caught by the minimumRequiredData check in constructor/setHistoricalData,
                 // which should be minimumRequiredData = max(smaPeriod, slowPeriod + signalPeriod) + 1 for previous calc.
                 // Let's adjust minimumRequiredData logic slightly in constructor to account for previous step explicitly.
                 // Re-evaluating: Minimum data for current is max(smaPeriod, slow+signal-1). Minimum for previous is max(smaPeriod, slow+signal-1) shifted back 1.
                 // So, to calculate current AND previous MACD, need data down to index (size - 2) - (slow + signal - 1) + 1 = size - slow - signal.
                 // Need data from size - slow - signal to size - 1. Number of points = (size-1) - (size - slow - signal) + 1 = slow + signal.
                 // So, min data for MACD current+previous is slow + signal. My original reasoning for minimumRequiredData as max(sma, slow+signal) is correct for having enough points total.
                 // The subList indices need to be correct though.
             }

             // The sublist for the previous MACD calculation needs to end at dataSize - 1 (exclusive)
             // and go back minimumRequiredData points from that endpoint.
            List<StockData> previousDataForMacd = historicalData.subList(dataSize - minimumRequiredData -1, dataSize - 1);
            double[] previousMacdValues = macd.calculate(previousDataForMacd, true);
            this.previousMacdLine = previousMacdValues[0];
            this.previousMacdSignal = previousMacdValues[1];

        } catch (DataProcessingException e) {
            // Re-throw DataProcessingException with added context
            throw new DataProcessingException("Error occurred during indicator calculation: " + e.getMessage(), e);
        } catch (IndexOutOfBoundsException e) {
             // Catch potential index errors from subList, which should ideally be prevented by data validation
             throw new DataProcessingException("Index out of bounds while calculating indicators. Data size: " + historicalData.size() +
                                                ", SMA Period: " + smaPeriod + ", MACD Periods: (" + macdFastPeriod + ", " + macdSlowPeriod + ", " + macdSignalPeriod + ")" +
                                                ", Required: " + minimumRequiredData, e);
        } catch (Exception e) {
            // Catch any other unexpected exceptions
             throw new DataProcessingException("An unexpected error occurred during indicator calculation: " + e.getMessage(), e);
        }
    }

    /**
     * Determines if a buy signal is generated based on the current conditions.
     *
     * <p>A buy signal is generated when the latest closing price is above the SMA
     * AND a bullish MACD crossover occurred in the most recent period
     * (i.e., previous MACD line was <= previous signal line, and current MACD line is > current signal line).</p>
     *
     * <p>Requires that {@link #calculateIndicators()} has been called to update indicator values
     * based on the latest data.</p>
     *
     * @return {@code true} if a buy signal is generated, {@code false} otherwise.
     * @throws DataProcessingException if the historical data is empty (should be prevented by validation).
     */
    @Override
    public boolean shouldBuy() {
        // Data validation should prevent empty historicalData here, but defensive check is okay.
        if (historicalData == null || historicalData.isEmpty()) {
             // This state should ideally not be reachable if constructor/updateData validation is correct
             throw new DataProcessingException("Historical data is null or empty, cannot determine buy signal.");
        }

        // Get the most recent stock data
        StockData latestStockData = historicalData.get(historicalData.size() - 1);

        // Check Buy Conditions:
        // 1. Price is above the SMA (Trend filter)
        boolean priceAboveSMA = latestStockData.getClose() > currentSMA;

        // 2. Bullish MACD Crossover occurred in the most recent period (Momentum/Signal)
        boolean macdBullishCrossover = previousMacdLine <= previousMacdSignal && currentMacdLine > currentMacdSignal;

        return priceAboveSMA && macdBullishCrossover;
    }

    /**
     * Determines if a sell signal is generated based on the current conditions.
     *
     * <p>A sell signal is generated when the latest closing price is below the SMA
     * AND a bearish MACD crossover occurred in the most recent period
     * (i.e., previous MACD line was >= previous signal line, and current MACD line is < current signal line).</p>
     *
     * <p>Requires that {@link #calculateIndicators()} has been called to update indicator values
     * based on the latest data.</p>
     *
     * @return {@code true} if a sell signal is generated, {@code false} otherwise.
     * @throws DataProcessingException if the historical data is empty (should be prevented by validation).
     */
    @Override
    public boolean shouldSell() {
         // Data validation should prevent empty historicalData here, but defensive check is okay.
        if (historicalData == null || historicalData.isEmpty()) {
             // This state should ideally not be reachable if constructor/updateData validation is correct
             throw new DataProcessingException("Historical data is null or empty, cannot determine sell signal.");
        }

        // Get the most recent stock data
        StockData latestStockData = historicalData.get(historicalData.size() - 1);

        // Check Sell Conditions:
        // 1. Price is below the SMA (Trend filter)
        boolean priceBelowSMA = latestStockData.getClose() < currentSMA;

        // 2. Bearish MACD Crossover occurred in the most recent period (Momentum/Signal)
        boolean macdBearishCrossover = previousMacdLine >= previousMacdSignal && currentMacdLine < currentMacdSignal;

        return priceBelowSMA && macdBearishCrossover;
    }
}