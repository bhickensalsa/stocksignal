package com.stocksignal.indicators.fundamental;

import com.stocksignal.exceptions.DataProcessingException;

/**
 * This class calculates the Earnings Growth of a company.
 */
public class EarningsGrowth {

    private final double currentEarnings;
    private final double previousEarnings;

    /**
     * Constructs an EarningsGrowth calculator.
     *
     * @param currentEarnings  the most recent earnings figure
     * @param previousEarnings the previous earnings figure
     */
    public EarningsGrowth(double currentEarnings, double previousEarnings) {
        if (currentEarnings < 0) {
            throw new IllegalArgumentException("Current earnings must not be negative.");
        }
        if (previousEarnings < 0) {
            throw new IllegalArgumentException("Previous earnings must not be negative.");
        }

        this.currentEarnings = currentEarnings;
        this.previousEarnings = previousEarnings;
    }

    /**
     * Calculates the earnings growth percentage.
     *
     * @return earnings growth percentage
     * @throws DataProcessingException if previous earnings are zero (undefined growth)
     */
    public double calculateGrowth() {
        if (previousEarnings == 0) {
            throw new DataProcessingException("Previous earnings cannot be zero when calculating growth.");
        }
        return ((currentEarnings - previousEarnings) / previousEarnings) * 100;
    }
}
