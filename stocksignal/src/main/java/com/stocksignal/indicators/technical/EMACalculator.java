package com.stocksignal.indicators.technical;

import com.stocksignal.data.StockData;
import com.stocksignal.exceptions.DataProcessingException; // Assuming this exception is used

import java.util.ArrayList;
import java.util.List;

/**
 * EMACalculator provides static utility methods to calculate the Exponential Moving Average (EMA) series.
 * <p>
 * This class implements the standard EMA calculation including the initial SMA period
 * and the recursive formula, providing the EMA value for each relevant data point in a series.
 * </p>
 * <p>
 * Note: This is a stateless utility class—no instance should be created.
 * </p>
 */
public class EMACalculator {

    private EMACalculator() {
        // Private constructor to prevent instantiation
    }

    /**
     * Calculates the Exponential Moving Average (EMA) series for a generic list of double values.
     * The series starts from the point where the first valid EMA can be calculated (index period - 1).
     *
     * <p>The calculation follows the standard method: initial EMA is the SMA of the first 'period' values,
     * subsequent EMAs use the recursive formula.</p>
     *
     * @param values a list of numeric values (e.g., prices or indicator values)
     * @param period the number of periods to use in the EMA calculation. Must be positive.
     * @return a list of EMA values, starting from the index `period - 1` of the input list.
     * The size of the returned list will be `values.size() - period + 1`.
     * @throws DataProcessingException if the input list is null, empty, or smaller than the period, or if period is non-positive.
     */
    public static List<Double> calculateSeries(List<Double> values, int period) {
        if (period <= 0) {
             throw new DataProcessingException("EMA period must be positive.");
        }
        if (values == null || values.size() < period) {
            throw new DataProcessingException("Not enough data to calculate EMA series. Required: " + period + ", Available: " + (values == null ? 0 : values.size()));
        }

        List<Double> emaSeries = new ArrayList<>(values.size() - period + 1);
        double multiplier = 2.0 / (period + 1);
        double currentEMA = 0;

        // Calculate the initial SMA for the first 'period' values
        double sumForSMA = 0;
        for (int i = 0; i < period; i++) {
            sumForSMA += values.get(i);
        }
        currentEMA = sumForSMA / period;
        emaSeries.add(currentEMA); // This EMA corresponds to index period - 1 in the original list

        // Calculate subsequent EMAs using the recursive formula
        for (int i = period; i < values.size(); i++) {
            double currentValue = values.get(i);
            currentEMA = ((currentValue - currentEMA) * multiplier) + currentEMA;
            emaSeries.add(currentEMA); // This EMA corresponds to index i in the original list
        }

        return emaSeries;
    }

    /**
     * Calculates the Exponential Moving Average (EMA) series based on closing prices
     * from a list of {@link StockData}. The series starts from the point where
     * the first valid EMA can be calculated (index period - 1).
     *
     * @param data   a list of stock data, each containing a closing price (must be in chronological order).
     * @param period the number of periods to use in the EMA calculation. Must be positive.
     * @return a list of EMA values based on closing prices, starting from the index `period - 1` of the input list.
     * The size of the returned list will be `data.size() - period + 1`.
     * @throws DataProcessingException if the stock data list is null, empty, or smaller than the period, or if period is non-positive.
     */
    public static List<Double> calculateSeriesFromStockData(List<StockData> data, int period) {
         if (period <= 0) {
             throw new DataProcessingException("EMA period must be positive.");
        }
        if (data == null || data.size() < period) {
            throw new DataProcessingException("Not enough stock data to calculate EMA series. Required: " + period + ", Available: " + (data == null ? 0 : data.size()));
        }

        List<Double> emaSeries = new ArrayList<>(data.size() - period + 1);
        double multiplier = 2.0 / (period + 1);
        double currentEMA = 0;

        // Calculate the initial SMA for the first 'period' closing prices
        double sumForSMA = 0;
        for (int i = 0; i < period; i++) {
            sumForSMA += data.get(i).getClose();
        }
        currentEMA = sumForSMA / period;
        emaSeries.add(currentEMA); // This EMA corresponds to index period - 1 in the original data list

        // Calculate subsequent EMAs using the recursive formula
        for (int i = period; i < data.size(); i++) {
            double currentClose = data.get(i).getClose();
            currentEMA = ((currentClose - currentEMA) * multiplier) + currentEMA;
            emaSeries.add(currentEMA); // This EMA corresponds to index i in the original data list
        }

        return emaSeries;
    }

    // Could also add methods to calculate a single EMA value for the *last* data point
    // if needed, potentially calling the series method and returning the last element.
    // However, providing the series calculation is the key improvement for MACD etc.
}