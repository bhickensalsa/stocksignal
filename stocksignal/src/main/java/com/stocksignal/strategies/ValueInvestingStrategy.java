package com.stocksignal.strategies;

import com.stocksignal.data.StockData;
import com.stocksignal.exceptions.ConfigurationException;
import com.stocksignal.exceptions.DataProcessingException;
import com.stocksignal.indicators.fundamental.EarningsGrowth;
import com.stocksignal.indicators.fundamental.PE_Ratio;
import com.stocksignal.utils.AppLogger;

import java.util.List;

/**
 * Implements a value investing strategy using PE ratio and earnings growth as fundamental indicators.
 *
 * <p>Buy Signal Criteria:
 * <ul>
 *   <li>PE ratio is below a specified maximum</li>
 *   <li>Earnings growth is greater than or equal to a specified minimum</li>
 * </ul>
 *
 * <p>Sell Signal Criteria:
 * <ul>
 *   <li>PE ratio is equal to or exceeds the maximum allowed</li>
 *   <li>Earnings growth is negative</li>
 * </ul>
 */
public class ValueInvestingStrategy implements Strategy {

    private List<StockData> historicalData;

    private final double maxPERatio;
    private final double minEarningsGrowth;

    private double calculatedPE;
    private double earningsGrowth;

    /**
     * Initializes a value investing strategy with custom thresholds and historical data.
     *
     * @param maxPERatio        the maximum acceptable PE ratio for a buy signal
     * @param minEarningsGrowth the minimum earnings growth for a buy signal
     * @param historicalData    the list of historical stock data to use
     * @throws ConfigurationException if any of the thresholds are invalid
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
     * Updates internal historical data reference.
     *
     * @param newHistoricalData list of new historical stock data
     */
    public void refreshHistoricalData(List<StockData> newHistoricalData) {
        this.historicalData = newHistoricalData;
    }

    /**
     * Updates strategy data with fresh market info and recalculates indicators.
     *
     * @param newData the updated market data
     */
    @Override
    public void updateData(List<StockData> newData) {
        refreshHistoricalData(newData);
        calculateIndicators();
    }

    /**
     * Returns the lookback period required for this strategy.
     *
     * Since the value investing strategy only uses the latest data point with its previous EPS,
     * it requires a minimum of 1 entry to function. Optionally, return 2 if you strictly require both
     * current and previous EPS to be in the list.
     *
     * @return required minimum number of historical data points
     */
    @Override
    public int getLookbackPeriod() {
        return 1; // or 2 if previous EPS is not part of the same object
    }

    /**
     * Calculates the PE ratio and earnings growth based on the latest stock data.
     *
     * @throws DataProcessingException if data is missing or invalid
     */
    @Override
    public void calculateIndicators() {
        if (historicalData == null || historicalData.isEmpty()) {
            throw new DataProcessingException("Historical stock data must be set before calculating indicators.");
        }

        StockData currentStock = historicalData.get(historicalData.size() - 1);

        double epsCurrent = currentStock.getCurrentEarningsPerShare();
        double epsPrevious = currentStock.getPreviousEarningsPerShare();

        if (epsCurrent <= 0 || epsPrevious <= 0) {
            throw new DataProcessingException(
                "Invalid earnings data for stock " + currentStock.getSymbol()
                + ": current EPS = " + epsCurrent + ", previous EPS = " + epsPrevious
            );
        }

        PE_Ratio pe = new PE_Ratio(currentStock.getClose(), epsCurrent);
        EarningsGrowth growth = new EarningsGrowth(epsCurrent, epsPrevious);

        this.calculatedPE = pe.calculate();
        this.earningsGrowth = growth.calculateGrowth();
    }

    /**
     * Determines if a buy signal should be issued.
     *
     * @return true if buy criteria are met, false otherwise
     */
    @Override
    public boolean shouldBuy() {
        if (historicalData == null || historicalData.isEmpty()) {
            throw new DataProcessingException("Historical stock data is required to determine buy signal.");
        }

        StockData currentStock = historicalData.get(historicalData.size() - 1);
        calculateIndicators();

        boolean buy = calculatedPE < maxPERatio && earningsGrowth >= minEarningsGrowth;

        if (buy) {
            AppLogger.info("Buy signal: Stock {} | PE = {} | Earnings Growth = {}",
                currentStock.getSymbol(), calculatedPE, earningsGrowth);
        }

        return buy;
    }

    /**
     * Determines if a sell signal should be issued.
     *
     * @return true if sell criteria are met, false otherwise
     */
    @Override
    public boolean shouldSell() {
        if (historicalData == null || historicalData.isEmpty()) {
            throw new DataProcessingException("Historical stock data is required to determine sell signal.");
        }

        StockData currentStock = historicalData.get(historicalData.size() - 1);
        calculateIndicators();

        boolean sell = calculatedPE >= maxPERatio || earningsGrowth < 0;

        if (sell) {
            AppLogger.info("Sell signal: Stock {} | PE = {} | Earnings Growth = {}",
                currentStock.getSymbol(), calculatedPE, earningsGrowth);
        }

        return sell;
    }
}
