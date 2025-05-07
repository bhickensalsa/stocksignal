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
 */
public class RSI implements Indicator {

    private final int period;

    /**
     * Constructs the RSI indicator with a specified period.
     * <p>
     * The period is the number of periods (typically 14) used to calculate the average gains and losses.
     * </p>
     *
     * @param period the number of days to calculate the RSI (typically 14)
     * @throws ConfigurationException if the period is not a positive integer
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
     * The RSI is calculated by comparing the average gains to the average losses over a specified period.
     * </p>
     *
     * @param data List of stock data in ascending date order (oldest to newest)
     * @return RSI value between 0 and 100
     * @throws DataProcessingException if there is insufficient data or the data is malformed
     */
    @Override
    public double calculate(List<StockData> data) {
        // Check if data is valid and has enough data points to perform the calculation
        if (data == null || data.size() <= period) {
            throw new DataProcessingException("Insufficient data to calculate RSI.");
        }

        double gainSum = 0;
        double lossSum = 0;

        // Loop over the data for the last 'period' number of days
        for (int i = data.size() - period; i < data.size(); i++) {
            double change = data.get(i).getClose() - data.get(i - 1).getClose();

            // Calculate gains and losses
            if (change >= 0) {
                gainSum += change;
            } else {
                lossSum += Math.abs(change);
            }
        }

        // Calculate the average gain and average loss
        double averageGain = gainSum / period;
        double averageLoss = lossSum / period;

        // If there are no losses, return an RSI of 100 (fully overbought)
        if (averageLoss == 0) {
            return 100; // Prevent division by zero (fully overbought)
        }

        // Calculate the relative strength (RS) and the RSI value
        double rs = averageGain / averageLoss;
        return 100 - (100 / (1 + rs));
    }
}
