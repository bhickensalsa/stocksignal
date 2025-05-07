package com.stocksignal.indicators.technical;

import java.util.List;

import com.stocksignal.exceptions.DataProcessingException;

/**
 * SignalLineCalculator provides a static method to calculate the Signal Line
 * from a list of MACD or similar values using Exponential Moving Average (EMA).
 * <p>
 * This class is intended to be used internally by indicators such as MACD
 * and follows a stateless, static utility pattern.
 */
public class SignalLineCalculator {

    /**
     * Calculates the Signal Line as the EMA of the given list of values.
     *
     * @param values a list of values (e.g., MACD values) over time
     * @param signalPeriod the number of periods to use for the EMA (typically 9)
     * @return the current signal line value
     * @throws IllegalArgumentException if values is null or smaller than signalPeriod
     */
    public static double calculate(List<Double> values, int signalPeriod) {
        if (values == null || values.size() < signalPeriod) {
            throw new DataProcessingException("Not enough values to calculate Signal Line.");
        }

        return EMACalculator.calculate(values, signalPeriod);
    }

    // Prevent instantiation
    private SignalLineCalculator() {}
}
