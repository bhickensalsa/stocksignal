package com.stocksignal.indicators.technical;

import com.stocksignal.data.StockData;

import java.util.ArrayList;
import java.util.List;

/**
 * The MACD (Moving Average Convergence Divergence) is a trend-following momentum indicator.
 * It shows the relationship between two EMAs of a stock's price.
 * 
 * This implementation uses external static calculators for EMA and signal line computation.
 */
public class MACD {

    private final int shortPeriod;
    private final int longPeriod;
    private final int signalPeriod;

    /**
     * Constructor to initialize MACD with the periods for short, long, and signal.
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
     * Calculates just the MACD line (difference between short and long EMAs).
     *
     * @param data the list of historical stock data
     * @return the MACD line value
     */
    public double calculate(List<StockData> data) {
        double shortEMA = EMACalculator.calculateFromStockData(data, shortPeriod);
        double longEMA = EMACalculator.calculateFromStockData(data, longPeriod);
        return shortEMA - longEMA;
    }

    /**
     * Calculates the MACD line, signal line, and histogram from the stock data.
     *
     * @param data          the list of historical stock data
     * @param includeSignal whether to include signal line and histogram
     * @return a double array where:
     *         - index 0: MACD line
     *         - index 1: Signal line (if included)
     *         - index 2: Histogram (if included)
     */
    public double[] calculate(List<StockData> data, boolean includeSignal) {
        double shortEMA = EMACalculator.calculateFromStockData(data, shortPeriod);
        double longEMA = EMACalculator.calculateFromStockData(data, longPeriod);
        double macdLine = shortEMA - longEMA;

        if (!includeSignal) {
            return new double[]{macdLine};
        }

        // Build MACD line history for signal calculation
        List<Double> macdHistory = buildMacdHistory(data);
        double signalLine = SignalLineCalculator.calculate(macdHistory, signalPeriod);
        double histogram = macdLine - signalLine;

        return new double[]{macdLine, signalLine, histogram};
    }

    /**
     * Builds a historical list of MACD line values for signal line calculation.
     */
    private List<Double> buildMacdHistory(List<StockData> data) {
        List<Double> macdHistory = new ArrayList<>();

        for (int i = longPeriod - 1; i < data.size(); i++) {
            List<StockData> subset = data.subList(i - longPeriod + 1, i + 1);
            double shortEMA = EMACalculator.calculateFromStockData(subset, shortPeriod);
            double longEMA = EMACalculator.calculateFromStockData(subset, longPeriod);
            macdHistory.add(shortEMA - longEMA);
        }

        return macdHistory;
    }
}
