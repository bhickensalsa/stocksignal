package com.stocksignal.indicators.technical;

import com.stocksignal.data.StockData;

import java.util.ArrayList;
import java.util.List;

/**
 * The MACD (Moving Average Convergence Divergence) is a trend-following momentum indicator
 * that shows the relationship between two Exponential Moving Averages (EMAs) of a stock's price.
 * It helps traders identify potential buy or sell signals based on crossovers of the MACD line and the signal line.
 * <p>
 * This implementation relies on external static calculators for EMA and signal line computation.
 * </p>
 */
public class MACD {

    private final int shortPeriod;
    private final int longPeriod;
    private final int signalPeriod;

    /**
     * Constructs the MACD indicator with specified periods for the short, long, and signal EMAs.
     * <p>
     * The short period defines the number of periods for the short-term EMA (e.g., 12),
     * the long period defines the number of periods for the long-term EMA (e.g., 26),
     * and the signal period is used to calculate the signal line (e.g., 9).
     * </p>
     *
     * @param shortPeriod  the short period for the MACD (e.g., 12)
     * @param longPeriod   the long period for the MACD (e.g., 26)
     * @param signalPeriod the signal period for the MACD (e.g., 9)
     */
    public MACD(int shortPeriod, int longPeriod, int signalPeriod) {
        this.shortPeriod = shortPeriod;
        this.longPeriod = longPeriod;
        this.signalPeriod = signalPeriod;
    }

    /**
     * Calculates only the MACD line, which is the difference between the short-term EMA and long-term EMA.
     * <p>
     * This method provides the basic MACD line for trend-following analysis.
     * </p>
     *
     * @param data the list of historical stock data
     * @return the calculated MACD line value
     */
    public double calculate(List<StockData> data) {
        // Calculate the short-term and long-term EMAs
        double shortEMA = EMACalculator.calculateFromStockData(data, shortPeriod);
        double longEMA = EMACalculator.calculateFromStockData(data, longPeriod);
        
        // Return the MACD line as the difference between the short-term and long-term EMAs
        return shortEMA - longEMA;
    }

    /**
     * Calculates the MACD line, signal line, and histogram from the provided stock data.
     * <p>
     * The MACD line is the difference between the short-term and long-term EMAs.
     * The signal line is the EMA of the MACD line over a specific signal period (typically 9).
     * The histogram represents the difference between the MACD line and the signal line.
     * </p>
     *
     * @param data          the list of historical stock data
     * @param includeSignal whether to include the signal line and histogram in the result
     * @return a double array where:
     *         - index 0: the MACD line
     *         - index 1: the signal line (if included)
     *         - index 2: the histogram (if included)
     */
    public double[] calculate(List<StockData> data, boolean includeSignal) {
        // Calculate the MACD line
        double shortEMA = EMACalculator.calculateFromStockData(data, shortPeriod);
        double longEMA = EMACalculator.calculateFromStockData(data, longPeriod);
        double macdLine = shortEMA - longEMA;

        // If signal line and histogram are not required, return only the MACD line
        if (!includeSignal) {
            return new double[]{macdLine};
        }

        // Build the historical MACD values for signal line calculation
        List<Double> macdHistory = buildMacdHistory(data);

        // Calculate the signal line using the MACD history and the signal period
        double signalLine = SignalLineCalculator.calculate(macdHistory, signalPeriod);

        // Calculate the histogram (MACD line - Signal line)
        double histogram = macdLine - signalLine;

        // Return an array with MACD line, signal line, and histogram
        return new double[]{macdLine, signalLine, histogram};
    }

    /**
     * Builds a historical list of MACD line values for the purpose of calculating the signal line.
     * <p>
     * This method computes the MACD line for each subset of the data based on the short and long periods
     * and builds a history of those MACD values, which is used to calculate the signal line.
     * </p>
     *
     * @param data the list of historical stock data
     * @return a list of MACD line values
     */
    private List<Double> buildMacdHistory(List<StockData> data) {
        List<Double> macdHistory = new ArrayList<>();

        // Loop through the historical data, starting from the point where we have enough data for the long period
        for (int i = longPeriod - 1; i < data.size(); i++) {
            // Create a subset of data for the current period
            List<StockData> subset = data.subList(i - longPeriod + 1, i + 1);
            
            // Calculate the short-term and long-term EMAs for the current subset of data
            double shortEMA = EMACalculator.calculateFromStockData(subset, shortPeriod);
            double longEMA = EMACalculator.calculateFromStockData(subset, longPeriod);

            // Add the MACD line (difference between short and long EMAs) to the history
            macdHistory.add(shortEMA - longEMA);
        }

        return macdHistory;
    }
}
