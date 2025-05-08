package com.stocksignal.indicators.technical;

import com.stocksignal.data.StockData;
import com.stocksignal.exceptions.ConfigurationException;
import com.stocksignal.exceptions.DataProcessingException;

import java.util.ArrayList;
import java.util.List;

/**
 * The MACD (Moving Average Convergence Divergence) is a trend-following momentum indicator
 * that shows the relationship between two Exponential Moving Averages (EMAs) of a stock's price.
 * It helps traders identify potential buy or sell signals based on crossovers of the MACD line and the signal line.
 * <p>
 * This improved implementation calculates the necessary EMA and Signal Line series correctly
 * using the updated {@link EMACalculator} and {@link SignalLineCalculator}. It then returns
 * the final value(s) of these series corresponding to the latest data point.
 * </p>
 */
public class MACD {

    private final int shortPeriod;
    private final int longPeriod;
    private final int signalPeriod;

    // Constants for the indices in the double[] returned by calculate(List<StockData>, boolean)
    public static final int MACD_LINE_INDEX = 0;
    public static final int SIGNAL_LINE_INDEX = 1;
    public static final int HISTOGRAM_INDEX = 2;

    /**
     * Constructs the MACD indicator with specified periods for the short, long, and signal EMAs.
     *
     * <p>The short period defines the number of periods for the short-term EMA (e.g., 12).
     * The long period defines the number of periods for the long-term EMA (e.g., 26).
     * The signal period is used to calculate the signal line (e.g., 9).</p>
     *
     * @param shortPeriod the short period for the MACD (e.g., 12). Must be positive and less than longPeriod.
     * @param longPeriod the long period for the MACD (e.g., 26). Must be positive and greater than shortPeriod.
     * @param signalPeriod the signal period for the MACD (e.g., 9). Must be positive.
     * @throws ConfigurationException if any period is non-positive or if shortPeriod >= longPeriod.
     */
    public MACD(int shortPeriod, int longPeriod, int signalPeriod) {
        // --- Validate Indicator Periods ---
        if (shortPeriod <= 0) {
            throw new ConfigurationException("Short period must be positive.");
        }
         if (longPeriod <= 0) {
            throw new ConfigurationException("Long period must be positive.");
        }
         if (signalPeriod <= 0) {
            throw new ConfigurationException("Signal period must be positive.");
        }
        if (shortPeriod >= longPeriod) {
            throw new ConfigurationException("Short period (" + shortPeriod + ") must be less than long period (" + longPeriod + ").");
        }

        this.shortPeriod = shortPeriod;
        this.longPeriod = longPeriod;
        this.signalPeriod = signalPeriod;
    }

    /**
     * Calculates only the most recent MACD line value from the provided stock data.
     * The MACD line is the difference between the short-term EMA and long-term EMA.
     *
     * @param data The list of historical stock data (must be in chronological order).
     * Requires sufficient data to calculate the MACD line (at least `longPeriod` data points).
     * @return The calculated MACD line value for the latest data point.
     * @throws DataProcessingException if the data list is null, empty, or contains insufficient data,
     * or if an error occurs during calculation.
     */
    public double calculate(List<StockData> data) {
        // Minimum data needed for the first valid MACD line value is longPeriod.
         if (data == null || data.size() < longPeriod) {
             throw new DataProcessingException("Insufficient data (" + (data == null ? 0 : data.size()) + ") to calculate MACD line. Required: " + longPeriod);
         }

        try {
            // Calculate the short and long EMA series using the improved calculator.
            // These series start from the first valid EMA point (index period - 1 in original data).
            List<Double> shortEmaSeries = EMACalculator.calculateSeriesFromStockData(data, shortPeriod);
            List<Double> longEmaSeries = EMACalculator.calculateSeriesFromStockData(data, longPeriod);

            // The last element of each series corresponds to the last data point in the input 'data'.
            // Need to ensure both series are valid up to the last data point.
            // Since data.size() >= longPeriod > shortPeriod, both series will have at least one point.
            // The last index in the short series corresponds to data.size() - 1.
            // The last index in the long series corresponds to data.size() - 1.

            double latestShortEMA = shortEmaSeries.get(shortEmaSeries.size() - 1);
            double latestLongEMA = longEmaSeries.get(longEmaSeries.size() - 1);

            // The latest MACD line is the difference of the latest EMAs
            return latestShortEMA - latestLongEMA;

        } catch (DataProcessingException e) {
             // Re-throw with context
             throw new DataProcessingException("Error calculating MACD line: " + e.getMessage(), e);
        } catch (IndexOutOfBoundsException e) {
             // This should ideally not happen with correct validation and series calculation,
             // but provides a safeguard.
             throw new DataProcessingException("Index out of bounds while accessing latest EMA values for MACD line. Data size: " + data.size() +
                                                ", Short Period: " + shortPeriod + ", Long Period: " + longPeriod, e);
        } catch (Exception e) {
             // Catch any other unexpected errors
             throw new DataProcessingException("An unexpected error occurred during MACD line calculation: " + e.getMessage(), e);
        }
    }

    /**
     * Calculates the most recent MACD line, signal line, and histogram values
     * from the provided stock data by calculating the full indicator series and
     * extracting the final values.
     *
     * <p>The MACD line is the difference between the short-term and long-term EMAs.
     * The signal line is the EMA of the MACD line over the signal period.
     * The histogram is the difference between the MACD line and the signal line.</p>
     *
     * @param data          The list of historical stock data (must be in chronological order).
     * Requires sufficient data to calculate the first valid signal line value
     * (at least `longPeriod + signalPeriod - 1` data points).
     * @param includeSignal Whether to include the signal line and histogram in the result array.
     * @return A double array containing the calculated values for the *latest* data point:
     * index {@code MACD_LINE_INDEX} (0): MACD line
     * index {@code SIGNAL_LINE_INDEX} (1): Signal line (if included)
     * index {@code HISTOGRAM_INDEX} (2): Histogram (if included)
     * The array size is 1 if includeSignal is false, and 3 if true.
     * @throws DataProcessingException if the data list is null, empty, or contains insufficient data,
     * or if an error occurs during calculation.
     */
    public double[] calculate(List<StockData> data, boolean includeSignal) {
        // Minimum data needed to calculate the first valid Signal Line value
        // is longPeriod + signalPeriod - 1.
        int minDataRequired = longPeriod + signalPeriod - 1;

        if (data == null || data.size() < minDataRequired) {
            throw new DataProcessingException("Insufficient data (" + (data == null ? 0 : data.size()) + ") to calculate MACD and Signal Line. Required: " + minDataRequired);
        }

        try {
            // --- Step 1: Calculate Short and Long EMA series ---
            // These series start from index shortPeriod - 1 and longPeriod - 1 respectively
            // of the original data. Their lengths are data.size() - period + 1.
            List<Double> shortEmaSeries = EMACalculator.calculateSeriesFromStockData(data, shortPeriod);
            List<Double> longEmaSeries = EMACalculator.calculateSeriesFromStockData(data, longPeriod);

            // --- Step 2: Calculate MACD Line series ---
            // The MACD line is the difference between the short and long EMA series.
            // The MACD line becomes valid from original data index longPeriod - 1.
            // The MACD line series will have a length of data.size() - longPeriod + 1.
            List<Double> macdLineSeries = new ArrayList<>(data.size() - longPeriod + 1);
            // The 'i'-th element of macdLineSeries corresponds to original data index 'i + longPeriod - 1'.
            // We need short and long EMA values at this same original index.
            // Short EMA at original index k is at index k - (shortPeriod - 1) in shortEmaSeries.
            // Long EMA at original index k is at index k - (longPeriod - 1) in longEmaSeries.
            // So, for macdLineSeries index 'i' (original index 'i + longPeriod - 1'):
            // Short EMA index: (i + longPeriod - 1) - (shortPeriod - 1) = i + longPeriod - shortPeriod
            // Long EMA index: (i + longPeriod - 1) - (longPeriod - 1) = i
            int shortEmaOffset = longPeriod - shortPeriod; // Offset to align short EMA index with long EMA index

            for (int i = 0; i < longEmaSeries.size(); i++) {
                 double shortEma = shortEmaSeries.get(i + shortEmaOffset);
                 double longEma = longEmaSeries.get(i);
                 macdLineSeries.add(shortEma - longEma);
            }

            // --- Step 3: Calculate Signal Line series ---
            // This is the EMA of the MACD line series.
            // The Signal Line series starts from original data index (longPeriod - 1) + (signalPeriod - 1) = longPeriod + signalPeriod - 2.
            // It starts at index signalPeriod - 1 within the macdLineSeries list.
            List<Double> signalLineSeries = SignalLineCalculator.calculateSeries(macdLineSeries, signalPeriod);

            // --- Step 4: Calculate Histogram series ---
            // Histogram = MACD Line - Signal Line
            // The Histogram series starts where both MACD line and Signal line series are valid.
            // This is from original data index longPeriod + signalPeriod - 2.
            // In the macdLineSeries, this corresponds to index signalPeriod - 1.
            // In the signalLineSeries, this corresponds to index 0.
            List<Double> histogramSeries = new ArrayList<>(signalLineSeries.size());
            int macdLineStartIndexForHistogram = signalPeriod - 1;

            for (int i = 0; i < signalLineSeries.size(); i++) {
                double macdVal = macdLineSeries.get(macdLineStartIndexForHistogram + i);
                double signalVal = signalLineSeries.get(i);
                histogramSeries.add(macdVal - signalVal);
            }

            // --- Step 5: Extract the latest values ---
            // The last values of all calculated series correspond to the last data point in the input 'data'.
            double latestMacdLine = macdLineSeries.get(macdLineSeries.size() - 1);

            if (!includeSignal) {
                return new double[]{latestMacdLine};
            }

            double latestSignalLine = signalLineSeries.get(signalLineSeries.size() - 1);
            double latestHistogram = histogramSeries.get(histogramSeries.size() - 1);

            return new double[]{latestMacdLine, latestSignalLine, latestHistogram};

        } catch (DataProcessingException e) {
            // Re-throw DataProcessingException from calculators with added context
            throw new DataProcessingException("Error during MACD calculation: " + e.getMessage(), e);
        } catch (IndexOutOfBoundsException e) {
             // Catch potential index errors during series processing. This could indicate
             // an issue with series alignment or insufficient data not caught by the initial check.
             throw new DataProcessingException("Index out of bounds during MACD series calculation. Data size: " + data.size() +
                                                ", Short: " + shortPeriod + ", Long: " + longPeriod + ", Signal: " + signalPeriod +
                                                ". This may indicate insufficient data or a calculation error.", e);
        } catch (Exception e) {
            // Catch any other unexpected exceptions
             throw new DataProcessingException("An unexpected error occurred during MACD calculation: " + e.getMessage(), e);
        }
    }
}