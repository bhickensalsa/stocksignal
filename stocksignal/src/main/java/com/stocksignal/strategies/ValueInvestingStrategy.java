package com.stocksignal.strategies;

import com.stocksignal.data.StockData;
import com.stocksignal.exceptions.ConfigurationException;
import com.stocksignal.exceptions.DataProcessingException;
import com.stocksignal.indicators.fundamental.EarningsGrowth;
import com.stocksignal.indicators.fundamental.PE_Ratio;
import com.stocksignal.utils.AppLogger;

import java.util.List;

/**
 * This class implements a value investing strategy based on the Price-to-Earnings (PE) ratio and earnings growth.
 * It determines buy or sell signals based on specified thresholds for PE ratio and earnings growth.
 * 
 * The strategy considers:
 * - Buy signal: When the PE ratio is below the maximum allowed and earnings growth is above or equal to the minimum threshold.
 * - Sell signal: When the PE ratio exceeds or is equal to the maximum allowed or earnings growth is negative.
 */
public class ValueInvestingStrategy implements Strategy {

    /** The maximum allowed PE ratio for the strategy. */
    private final double maxPERatio;

    /** The minimum allowed earnings growth for the strategy. */
    private final double minEarningsGrowth;

    /** Historical stock data used for calculating indicators. */
    private List<StockData> historicalData;

    /** Calculated PE ratio for the most recent stock data. */
    private double calculatedPE;

    /** Calculated earnings growth for the most recent stock data. */
    private double earningsGrowth;

    /**
     * Constructor to initialize the ValueInvestingStrategy with the specified parameters.
     *
     * @param maxPERatio The maximum PE ratio allowed for a buy signal.
     * @param minEarningsGrowth The minimum earnings growth required for a buy signal.
     * @param historicalData A list of historical stock data used for calculating indicators.
     * @throws ConfigurationException If maxPERatio or minEarningsGrowth are non-positive.
     */
    public ValueInvestingStrategy(double maxPERatio, double minEarningsGrowth, List<StockData> historicalData) {
        if (maxPERatio <= 0 || minEarningsGrowth <= 0) {
            throw new ConfigurationException("PE ratio and earnings growth must be positive values.");
        }
        this.maxPERatio = maxPERatio;
        this.minEarningsGrowth = minEarningsGrowth;
        this.historicalData = historicalData;
    }

    /**
     * Calculates the PE ratio and earnings growth using the most recent stock data.
     * This method fetches the latest earnings per share (EPS) data from the stock's historical data and computes the indicators.
     * 
     * @throws DataProcessingException If historical data is not set, or EPS data is invalid (non-positive).
     */
    @Override
    public void calculateIndicators() {
        if (historicalData == null || historicalData.isEmpty()) {
            throw new DataProcessingException("Historical stock data must be set before calculating indicators.");
        }

        // Use the most recent stock data from the historical data
        StockData currentStock = historicalData.get(historicalData.size() - 1);

        double epsCurrent = currentStock.getCurrentEarningsPerShare();
        double epsPrevious = currentStock.getPreviousEarningsPerShare();

        // Validate EPS data
        if (epsCurrent <= 0 || epsPrevious <= 0) {
            throw new DataProcessingException(
                "Invalid earnings data for stock " + currentStock.getSymbol()
                + ": current EPS = " + epsCurrent + ", previous EPS = " + epsPrevious
            );
        }

        // Calculate PE ratio and earnings growth using the provided indicators
        PE_Ratio pe = new PE_Ratio(currentStock.getClose(), epsCurrent);
        EarningsGrowth growth = new EarningsGrowth(epsCurrent, epsPrevious);

        this.calculatedPE = pe.calculate();
        this.earningsGrowth = growth.calculateGrowth();
    }

    /**
     * Determines if a buy signal is generated based on the calculated indicators.
     * A buy signal is generated if the PE ratio is less than the maximum allowed, 
     * and the earnings growth is greater than or equal to the minimum required.
     *
     * @return true if the stock should be bought, false otherwise.
     * @throws DataProcessingException If historical stock data is not available or there are calculation issues.
     */
    @Override
    public boolean shouldBuy() {
        if (historicalData == null || historicalData.isEmpty()) {
            throw new DataProcessingException("Historical stock data is required to determine buy signal.");
        }

        // Use the most recent stock data from the historical data
        StockData currentStock = historicalData.get(historicalData.size() - 1);
        calculateIndicators();

        // Evaluate if the stock meets the buy criteria
        boolean buy = calculatedPE < maxPERatio && earningsGrowth >= minEarningsGrowth;
        
        // Log the buy signal if applicable
        if (buy) {
            AppLogger.info("Buy signal: Stock {} | PE = {} | Earnings Growth = {}", 
                currentStock.getSymbol(), calculatedPE, earningsGrowth);
        }

        return buy;
    }

    /**
     * Determines if a sell signal is generated based on the calculated indicators.
     * A sell signal is generated if the PE ratio is greater than or equal to the maximum allowed,
     * or if the earnings growth is negative.
     *
     * @return true if the stock should be sold, false otherwise.
     * @throws DataProcessingException If historical stock data is not available or there are calculation issues.
     */
    @Override
    public boolean shouldSell() {
        if (historicalData == null || historicalData.isEmpty()) {
            throw new DataProcessingException("Historical stock data is required to determine sell signal.");
        }

        // Use the most recent stock data from the historical data
        StockData currentStock = historicalData.get(historicalData.size() - 1);
        calculateIndicators();

        // Evaluate if the stock meets the sell criteria
        boolean sell = calculatedPE >= maxPERatio || earningsGrowth < 0;
        
        // Log the sell signal if applicable
        if (sell) {
            AppLogger.info("Sell signal: Stock {} | PE = {} | Earnings Growth = {}", 
                currentStock.getSymbol(), calculatedPE, earningsGrowth);
        }

        return sell;
    }
}
