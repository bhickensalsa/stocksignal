package com.stocksignal.indicators.technical;

import com.stocksignal.data.StockData;
import java.util.List;

/**
 * EMACalculator provides static utility methods to calculate the Exponential Moving Average (EMA).
 * <p>
 * This class is designed for use within other indicators (such as MACD) and should not be instantiated.
 * It supports calculating EMA from either a generic list of numerical values or from closing prices
 * in a list of {@link StockData}.
 * <p>
 * Note: This is a stateless utility class—no instance should be created.
 */
public class EMACalculator {

    /**
     * Calculates the EMA for a generic list of double values.
     *
     * @param values a list of numeric values (e.g., prices or indicator values)
     * @param period the number of periods to use in the EMA calculation
     * @return the final EMA value
     * @throws IllegalArgumentException if the input list is null or smaller than the period
     */
    public static double calculate(List<Double> values, int period) {
        if (values == null || values.size() < period) {
            throw new IllegalArgumentException("Not enough data to calculate EMA.");
        }

        double multiplier = 2.0 / (period + 1);
        double ema = values.get(0); // Starting EMA

        for (int i = 1; i < values.size(); i++) {
            ema = (values.get(i) - ema) * multiplier + ema;
        }

        return ema;
    }

    /**
     * Calculates the EMA based on closing prices from a list of {@link StockData}.
     *
     * @param data a list of stock data, each containing a closing price
     * @param period the number of periods to use in the EMA calculation
     * @return the final EMA value based on closing prices
     * @throws IllegalArgumentException if the stock data list is null or smaller than the period
     */
    public static double calculateFromStockData(List<StockData> data, int period) {
        if (data == null || data.size() < period) {
            throw new IllegalArgumentException("Not enough stock data to calculate EMA.");
        }

        double multiplier = 2.0 / (period + 1);
        double ema = data.get(0).getClose();

        for (int i = 1; i < data.size(); i++) {
            ema = (data.get(i).getClose() - ema) * multiplier + ema;
        }

        return ema;
    }
}
