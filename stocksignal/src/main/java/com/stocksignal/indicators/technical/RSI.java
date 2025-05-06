package com.stocksignal.indicators.technical;

import com.stocksignal.data.StockData;
import com.stocksignal.exceptions.DataProcessingException;
import com.stocksignal.exceptions.ConfigurationException;
import com.stocksignal.indicators.Indicator;

import java.util.List;

/**
 * Calculates the Relative Strength Index (RSI) for a given stock.
 */
public class RSI implements Indicator {

    private final int period;

    /**
     * Constructs the RSI indicator with a specified period.
     *
     * @param period the number of days to calculate the RSI (typically 14)
     */
    public RSI(int period) {
        if (period <= 0) {
            throw new ConfigurationException("RSI period must be a positive integer.");
        }
        this.period = period;
    }

    /**
     * Calculates the latest RSI value based on closing prices.
     *
     * @param data List of stock data in ascending date order
     * @return RSI value between 0 and 100
     * @throws DataProcessingException if there's not enough data
     */
    @Override
    public double calculate(List<StockData> data) {
        if (data == null || data.size() <= period) {
            throw new DataProcessingException("Insufficient data to calculate RSI.");
        }

        double gainSum = 0;
        double lossSum = 0;

        for (int i = data.size() - period; i < data.size(); i++) {
            double change = data.get(i).getClose() - data.get(i - 1).getClose();

            if (change >= 0) {
                gainSum += change;
            } else {
                lossSum += Math.abs(change);
            }
        }

        double averageGain = gainSum / period;
        double averageLoss = lossSum / period;

        if (averageLoss == 0) {
            return 100; // Prevent division by zero (fully overbought)
        }

        double rs = averageGain / averageLoss;
        return 100 - (100 / (1 + rs));
    }
}
