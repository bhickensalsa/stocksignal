package com.stocksignal.indicators.technical;

import java.util.List;
import com.stocksignal.exceptions.DataProcessingException;

/**
 * SignalLineCalculator provides a static method to calculate the Signal Line
 * from a list of MACD or similar values using Exponential Moving Average (EMA).
 * <p>
 * This class is intended to be used internally by indicators such as MACD
 * and follows a stateless, static utility pattern. The Signal Line is commonly
 * used in conjunction with MACD to generate trading signals based on crossovers.
 * </p>
 */
public class SignalLineCalculator {

    /**
     * Calculates the Signal Line as the Exponential Moving Average (EMA) of the given list of values.
     * <p>
     * The Signal Line is typically calculated as a 9-period EMA of the MACD values. The Signal Line
     * is used to smooth out the MACD and identify signals when the MACD crosses over or under the Signal Line.
     * </p>
     *
     * @param values a list of values (e.g., MACD values) over time
     * @param signalPeriod the number of periods to use for the EMA (typically 9)
     * @return the current signal line value
     * @throws IllegalArgumentException if values is null or smaller than signalPeriod
     */
    public static double calculate(List<Double> values, int signalPeriod) {
        // Check if the list of values is valid and has enough data points for the calculation
        if (values == null || values.size() < signalPeriod) {
            throw new DataProcessingException("Not enough values to calculate Signal Line.");
        }

        // Use the EMACalculator to calculate the Signal Line as an EMA of the given values
        return EMACalculator.calculate(values, signalPeriod);
    }

    // Private constructor to prevent instantiation of this utility class
    private SignalLineCalculator() {}
}
