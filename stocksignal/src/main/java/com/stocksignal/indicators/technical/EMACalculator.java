package com.stocksignal.indicators.technical;

import com.stocksignal.data.StockData;
import java.util.List;

/**
 * EMACalculator provides static utility methods to calculate the Exponential Moving Average (EMA).
 * <p>
 * This class is designed for use within other indicators (such as MACD) and should not be instantiated.
 * It supports calculating EMA from either a generic list of numerical values or from closing prices
 * in a list of {@link StockData}.
 * </p>
 * <p>
 * Note: This is a stateless utility class—no instance should be created.
 * </p>
 */
public class EMACalculator {

    /**
     * Calculates the Exponential Moving Average (EMA) for a generic list of double values.
     * <p>
     * The formula for EMA is:
     * <pre>
     * EMA = (Current Value - Previous EMA) * multiplier + Previous EMA
     * </pre>
     * where multiplier = 2 / (period + 1).
     * </p>
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

        // Calculate the multiplier (smoothing factor)
        double multiplier = 2.0 / (period + 1);

        // Initialize EMA with the first value in the list
        double ema = values.get(0); 

        // Loop through the list, updating the EMA with each new value
        for (int i = 1; i < values.size(); i++) {
            ema = (values.get(i) - ema) * multiplier + ema;
        }

        return ema;
    }

    /**
     * Calculates the Exponential Moving Average (EMA) based on closing prices from a list of {@link StockData}.
     * <p>
     * This method is specifically designed for use with stock data, where each {@link StockData} object contains
     * the closing price of a stock for a given period.
     * </p>
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

        // Calculate the multiplier (smoothing factor)
        double multiplier = 2.0 / (period + 1);

        // Initialize EMA with the first stock's closing price
        double ema = data.get(0).getClose();

        // Loop through the list of stock data, updating the EMA with each closing price
        for (int i = 1; i < data.size(); i++) {
            ema = (data.get(i).getClose() - ema) * multiplier + ema;
        }

        return ema;
    }
}
