package com.stocksignal.indicators.technical;

import com.stocksignal.exceptions.DataProcessingException;

import java.util.List;

/**
 * SignalLineCalculator provides a static method to calculate the Signal Line
 * series from a list of values (typically MACD line values) using Exponential Moving Average (EMA).
 * <p>
 * This class is intended to be used internally by indicators such as MACD.
 * The Signal Line is commonly used in conjunction with the MACD line to generate trading signals
 * based on crossovers and provides a smoothed representation of the MACD line's movement.
 * </p>
 * <p>
 * This utility class calculates the entire Signal Line series for the provided input values.
 * </p>
 */
public class SignalLineCalculator {

    // Private constructor to prevent instantiation of this utility class
    private SignalLineCalculator() {}

    /**
     * Calculates the Signal Line series as the Exponential Moving Average (EMA) of the given list of values.
     * The series starts from the point where the first valid EMA can be calculated (index signalPeriod - 1
     * relative to the input values list).
     *
     * <p>The Signal Line is typically calculated as a 9-period EMA of the MACD values. The Signal Line
     * is used to smooth out the MACD and identify signals when the MACD crosses over or under the Signal Line.
     * This method returns the entire series of calculated Signal Line values.</p>
     *
     * @param values        A list of values (e.g., MACD line values) representing a time series.
     * Assumed to be in chronological order.
     * @param signalPeriod The number of periods to use for the EMA (typically 9). Must be positive.
     * @return A list of Signal Line values, starting from the index `signalPeriod - 1` of the input `values` list.
     * The size of the returned list will be `values.size() - signalPeriod + 1`.
     * Returns an empty list if input values size < signalPeriod (after throwing exception).
     * @throws DataProcessingException if values is null, empty, or smaller than signalPeriod, or if signalPeriod is non-positive.
     */
    public static List<Double> calculateSeries(List<Double> values, int signalPeriod) {
        if (signalPeriod <= 0) {
             throw new DataProcessingException("Signal period must be positive.");
        }
        // Use the EMACalculator.calculateSeries method which handles null/empty/insufficient data for its period check.
        // We re-throw as DataProcessingException for consistency in this package.
        try {
            // The new EMACalculator.calculateSeries handles validation and returns the EMA series.
            return EMACalculator.calculateSeries(values, signalPeriod);
        } catch (DataProcessingException e) {
            // Wrap EMACalculator's DataProcessingException if needed, or re-throw directly.
            // Re-throwing directly is fine as the context (insufficient data for EMA) is clear.
            throw new DataProcessingException("Error calculating Signal Line series: " + e.getMessage(), e);
        } catch (Exception e) {
             // Catch any other unexpected errors from EMACalculator
             throw new DataProcessingException("An unexpected error occurred during Signal Line series calculation: " + e.getMessage(), e);
        }
    }

    // Note: Removed the old 'calculate' method that returned a single double
    // because the core need for MACD calculation is the series.
    // If the last value specifically is needed, you would call calculateSeries
    // and then get the last element of the returned list.
}