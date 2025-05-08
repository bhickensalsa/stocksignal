package com.stocksignal.indicators.technical;

import java.util.List;
import com.stocksignal.data.StockData;
import com.stocksignal.exceptions.DataProcessingException;
import com.stocksignal.exceptions.ConfigurationException;
import com.stocksignal.indicators.Indicator;

/**
 * Calculates the Relative Strength Index (RSI) for a given stock based on closing prices.
 * <p>
 * The RSI is a momentum oscillator that measures the speed and change of price movements.
 * It oscillates between 0 and 100, and values typically indicate overbought (>70) or oversold (<30) conditions.
 * </p>
 * <p>
 * This implementation follows the standard calculation method, including the initial
 * Simple Moving Average for the first period and the subsequent smoothed average calculation.
 * </p>
 */
public class RSI implements Indicator {

    private final int period;

    /**
     * Constructs the RSI indicator with a specified period.
     * <p>
     * The period is the number of periods (typically 14) used to calculate the average gains and losses.
     * </p>
     *
     * @param period the number of days to calculate the RSI (typically 14). Must be a positive integer.
     * @throws ConfigurationException if the period is not a positive integer.
     */
    public RSI(int period) {
        if (period <= 0) {
            throw new ConfigurationException("RSI period must be a positive integer.");
        }
        this.period = period;
    }

    /**
     * Calculates the latest RSI value based on the closing prices of the provided stock data.
     * <p>
     * The RSI is calculated by:
     * 1. Determining price changes between consecutive periods.
     * 2. Separating gains (positive changes) and losses (absolute value of negative changes).
     * 3. Calculating the initial average gain and loss as the Simple Moving Average (SMA)
     * of the first 'period' gains and losses.
     * 4. For subsequent periods, calculating the smoothed average gains and losses recursively
     * using the standard smoothing formula.
     * 5. Calculating Relative Strength (RS) and the final RSI value using the latest smoothed averages.
     * </p>
     * <p>
     * Requires at least `period + 1` data points to calculate the first 'period' price changes.
     * The data must be in ascending chronological order.
     * </p>
     *
     * @param data List of stock data in ascending date order (oldest to newest).
     * @return RSI value between 0 and 100. Returns 100 if there are only gains across the calculation history,
     * 0 if there are only losses across the history. Returns NaN if sufficient data is present but
     * no price change occurred across the entire history (both total gain and total loss are zero).
     * @throws DataProcessingException if the data list is null, empty, contains insufficient data,
     * or is not in chronological order.
     */
    @Override
    public double calculate(List<StockData> data) {
        // Check if data is valid and has enough data points to perform the calculation.
        // Need 'period' changes, which requires 'period + 1' data points (data[i] - data[i-1]).
        if (data == null || data.size() < period + 1) {
            throw new DataProcessingException("Insufficient data to calculate RSI. Required: " + (period + 1) + ", Available: " + (data == null ? 0 : data.size()));
        }

        double[] gains = new double[data.size()];
        double[] losses = new double[data.size()];

        // Calculate gains and losses for each period (from the 2nd data point onwards, index 1)
        // The change at index i is data[i].close - data[i-1].close
        // The relevant changes for the first RSI calculation are at indices 1 to 'period'.
        for (int i = 1; i < data.size(); i++) {
            double change = data.get(i).getClose() - data.get(i - 1).getClose();
            if (change > 0) {
                gains[i] = change;
                losses[i] = 0; // Ensure losses are zero when there's a gain
            } else { // change <= 0
                gains[i] = 0; // Ensure gains are zero when there's a loss or no change
                losses[i] = Math.abs(change); // Store positive value for loss
            }
        }

        // --- Step 1: Calculate the initial average gain and average loss ---
        // This is the SMA of the first 'period' gains and losses (changes at indices 1 to 'period').
        double initialAvgGain = 0;
        double initialAvgLoss = 0;

        for (int i = 1; i <= period; i++) {
             initialAvgGain += gains[i];
             initialAvgLoss += losses[i];
        }

        initialAvgGain /= period;
        initialAvgLoss /= period;

        // Handle the case where the data set is exactly 'period + 1' size - this is the first valid RSI value
        if (data.size() == period + 1) {
            // Calculate RS and RSI based on the initial averages
            if (initialAvgLoss == 0) {
                 // If initial average loss is 0, RSI is 100 unless initial average gain is also 0
                 return initialAvgGain == 0 ? 0 : 100; // 0 if no change in the first period, 100 if only gains
             }
             double rs = initialAvgGain / initialAvgLoss;
             // Handle potential Infinity from division if initialAvgLoss is very small positive number
             if (Double.isInfinite(rs)) {
                 return 100;
             }
             return 100.0 - (100.0 / (1.0 + rs));
        }


        // --- Step 2: Calculate smoothed averages recursively for subsequent periods ---
        double currentAvgGain = initialAvgGain;
        double currentAvgLoss = initialAvgLoss;

        // Start smoothing from the data point after the initial period (index period + 1 in original data)
        // The changes used for smoothing are at indices from period + 1 up to data.size() - 1
        for (int i = period + 1; i < data.size(); i++) {
            double currentGain = gains[i]; // Gain for the current day (index i)
            double currentLoss = losses[i]; // Loss for the current day (index i)

            // Apply the smoothed average formula: NewAvg = ((OldAvg * (period - 1)) + Current) / period
            currentAvgGain = ((currentAvgGain * (period - 1)) + currentGain) / period;
            currentAvgLoss = ((currentAvgLoss * (period - 1)) + currentLoss) / period;
        }

        // After the loop, currentAvgGain and currentAvgLoss hold the smoothed averages for the last data point (index data.size() - 1).

        // --- Step 3: Calculate the final RS and RSI ---
        if (currentAvgLoss == 0) {
            // If smoothed average loss is 0, RSI is 100 unless smoothed average gain is also 0.
            // If both are 0 across the *entire* history used for smoothing, RSI is typically considered 0 or NaN.
            // Let's return 0 if both are zero, 100 if only smoothed gains > 0.
            return currentAvgGain == 0 ? 0 : 100;
        }

        double rs = currentAvgGain / currentAvgLoss;

        // Handle potential Infinity from division if currentAvgLoss is very small positive number
        if (Double.isInfinite(rs)) {
             return 100;
        }
        // Handle potential NaN if both average gain and loss were initially 0 and remained 0 (e.g., flat price)
         if (Double.isNaN(rs)) {
              return 0; // Or handle as NaN depending on desired behavior
         }

        return 100.0 - (100.0 / (1.0 + rs));
    }
}