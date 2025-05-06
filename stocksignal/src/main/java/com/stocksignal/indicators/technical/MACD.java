package com.stocksignal.indicators.technical;

import com.stocksignal.data.StockData;
import java.util.List;

/**
 * The MACD (Moving Average Convergence Divergence) is a trend-following momentum indicator.
 * It shows the relationship between two EMAs of a stock's price.
 */
public class MACD {

    private final int shortPeriod;
    private final int longPeriod;
    private final int signalPeriod;

    /**
     * Constructor to initialize MACD with the periods for short, long, and signal.
     * 
     * @param shortPeriod the short period for the MACD (e.g., 12)
     * @param longPeriod  the long period for the MACD (e.g., 26)
     * @param signalPeriod the signal period for the MACD (e.g., 9)
     */
    public MACD(int shortPeriod, int longPeriod, int signalPeriod) {
        this.shortPeriod = shortPeriod;
        this.longPeriod = longPeriod;
        this.signalPeriod = signalPeriod;
    }

    /**
     * Calculates just the MACD line (difference between short and long EMAs).
     * This method will return the MACD line as a single double value.
     * 
     * @param data the list of historical stock data
     * @return the MACD line
     */
    public double calculate(List<StockData> data) {
        double shortEMA = calculateEMA(data, shortPeriod);
        double longEMA = calculateEMA(data, longPeriod);
        return shortEMA - longEMA;  // MACD line is the difference
    }

    /**
     * Calculates the MACD, Signal Line, and Histogram for the given list of stock data.
     * This method will return all three components in a double array.
     * 
     * @param data the list of historical stock data
     * @return a double array where:
     *   - index 0: MACD line
     *   - index 1: Signal line
     *   - index 2: MACD Histogram
     */
    public double[] calculate(List<StockData> data, boolean includeSignal) {
        double shortEMA = calculateEMA(data, shortPeriod);
        double longEMA = calculateEMA(data, longPeriod);

        // MACD line: difference between short and long EMAs
        double macdLine = shortEMA - longEMA;

        // Calculate the Signal line (9-period EMA of MACD line)
        double signalLine = calculateSignalLine(macdLine);

        // Optionally, calculate the MACD Histogram: difference between MACD line and Signal line
        double histogram = macdLine - signalLine;

        // If we need the signal line and histogram, return all three values
        if (includeSignal) {
            return new double[]{macdLine, signalLine, histogram};
        }

        // Otherwise, return just the MACD line
        return new double[]{macdLine};  // Only MACD line is needed
    }

    /**
     * Helper method to calculate the Exponential Moving Average (EMA) for a given period.
     */
    private double calculateEMA(List<StockData> data, int period) {
        double multiplier = 2.0 / (period + 1);
        double ema = data.get(0).getClose(); // Start with the first data point (could be adjusted)

        for (int i = 1; i < data.size(); i++) {
            ema = (data.get(i).getClose() - ema) * multiplier + ema;
        }

        return ema;
    }

    /**
     * Helper method to calculate the Signal Line (9-period EMA of the MACD line).
     */
    private double calculateSignalLine(double macdLine) {
        // In practice, we would keep track of the previous Signal line value
        // and calculate the next EMA based on that.
        return macdLine; // This is a simplified version
    }
}
