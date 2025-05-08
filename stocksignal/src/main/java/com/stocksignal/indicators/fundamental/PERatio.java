package com.stocksignal.indicators.fundamental;

import com.stocksignal.exceptions.ConfigurationException;

/**
 * This class represents and calculates the Price-to-Earnings (PE) Ratio
 * for a stock based on its current price and earnings per share.
 *
 * <p>The PE Ratio is a fundamental valuation metric calculated as:
 * {@code Current Stock Price / Earnings Per Share (EPS)}.</p>
 *
 * <p>A high PE ratio can indicate an expensive stock or high growth expectations,
 * while a low PE ratio might suggest an undervalued stock or low growth prospects.
 * A negative PE ratio occurs when a company has negative earnings (is losing money).</p>
 */
public class PERatio { // Renamed class for standard convention

    private final double currentPrice;
    private final double earningsPerShare;

    /**
     * Constructs a PeRatio calculation instance with the specified current price and earnings per share.
     *
     * @param currentPrice       The current stock price. Must be a positive value (greater than zero).
     * @param earningsPerShare   The earnings per share (can be positive, negative, or zero).
     * @throws ConfigurationException If the current price is not greater than zero.
     */
    public PERatio(double currentPrice, double earningsPerShare) {
        // Validate that price is positive
        if (currentPrice <= 0) {
            // Price must be a positive value for a meaningful PE calculation
            throw new ConfigurationException("Current price must be greater than zero for PE Ratio calculation.");
        }
        // Earnings per share can be zero or negative, resulting in an infinite or negative PE ratio.
        // We do not validate EPS value here, allowing standard financial results.

        this.currentPrice = currentPrice;
        this.earningsPerShare = earningsPerShare;
    }

    /**
     * Calculates the Price-to-Earnings (PE) Ratio based on the current stock price
     * and earnings per share provided during construction.
     *
     * <p>The calculation is: {@code currentPrice / earningsPerShare}.</p>
     *
     * @return The calculated PE ratio:
     * <ul>
     * <li>A positive double if EPS > 0.</li>
     * <li>A negative double if EPS < 0.</li>
     * <li>{@code Double.POSITIVE_INFINITY} if EPS = 0 (since price > 0 is validated in constructor).</li>
     * </ul>
     * <p>Note: This method relies on standard double floating-point arithmetic,
     * which may have minor precision limitations compared to BigDecimal for very sensitive calculations,
     * but is typically sufficient for technical/fundamental indicators.</p>
     */
    public double calculate() {
        // Handle the case where EPS is zero explicitly for clarity.
        // Standard double division would also result in Double.POSITIVE_INFINITY when price > 0 and EPS == 0.
        if (earningsPerShare == 0.0) {
            return Double.POSITIVE_INFINITY;
        }

        // Perform the division. This correctly handles positive and negative EPS.
        return currentPrice / earningsPerShare;
    }

    // Optional: Add getters for the fields if needed
    // public double getCurrentPrice() { return currentPrice; }
    // public double getEarningsPerShare() { return earningsPerShare; }
}