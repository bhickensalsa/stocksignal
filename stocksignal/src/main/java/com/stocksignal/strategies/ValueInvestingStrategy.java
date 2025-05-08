package com.stocksignal.strategies;

import com.stocksignal.data.StockData;
import com.stocksignal.exceptions.ConfigurationException;
import com.stocksignal.exceptions.DataProcessingException;
import com.stocksignal.indicators.fundamental.EarningsGrowth;
import com.stocksignal.indicators.fundamental.PERatio;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements a value investing strategy using PE ratio and earnings growth as fundamental indicators.
 * This strategy is designed to work with incremental data updates during backtesting.
 *
 * <p>Buy Signal Criteria:
 * <ul>
 * <li>PE ratio is below a specified maximum</li>
 * <li>Earnings growth is greater than or equal to a specified minimum</li>
 * </ul>
 *
 * <p>Sell Signal Criteria:
 * <ul>
 * <li>PE ratio is equal to or exceeds the maximum allowed</li>
 * <li>Earnings growth is negative</li>
 * </ul>
 */
public class ValueInvestingStrategy implements Strategy {

    /**
     * The historical stock data accumulated by the strategy.
     * Assumed to be in chronological order (oldest to newest).
     * This list will grow during backtesting as new data is added.
     */
    private List<StockData> historicalData;

    private final double maxPERatio;
    private final double minEarningsGrowth;

    private double calculatedPE;
    private double earningsGrowth;

    // Assuming StockData contains both current and previous EPS,
    // we only need the latest data point to calculate indicators.
    private static final int REQUIRED_DATA_SIZE = 1;

    /**
     * Initializes a value investing strategy with custom thresholds and initial historical data.
     *
     * <p>Initial data must be provided upon construction. During backtesting,
     * this initial data should be at least {@code getLookbackPeriod()} data points.</p>
     *
     * @param maxPERatio        the maximum acceptable PE ratio for a buy signal
     * @param minEarningsGrowth the minimum earnings growth for a buy signal
     * @param initialHistoricalData the initial list of historical stock data to use.
     * Should be at least {@code getLookbackPeriod()} in size.
     * @throws ConfigurationException if any of the thresholds are invalid
     * @throws DataProcessingException if the initial historical data is null or contains insufficient data.
     */
    public ValueInvestingStrategy(double maxPERatio, double minEarningsGrowth, List<StockData> initialHistoricalData) {
        if (maxPERatio <= 0) {
            throw new ConfigurationException("Maximum PE ratio must be positive.");
        }
         // Note: minEarningsGrowth can be 0 or negative if the strategy allows buying
         // companies with zero or slightly negative growth under certain PE conditions.
         // The original check `minEarningsGrowth <= 0` seemed to prevent this.
         // Let's assume for this update that non-positive minEarningsGrowth is acceptable based on the strategy description.
         // If negative growth is acceptable for buying, the validation should be adjusted.
         // Keeping the original check for now, but it might be a point of review based on strategy intent.
         if (minEarningsGrowth <= 0) {
             // Original message was misleading, let's make it clearer if keeping the check.
             // If minEarningsGrowth must be strictly positive for a BUY signal:
              // throw new ConfigurationException("Minimum earnings growth for a buy signal must be positive.");
             // If 0 or negative is allowed for minEarningsGrowth but not for the parameters themselves being <= 0:
              // The initial `minEarningsGrowth <= 0` check is problematic if negative growth is intended for SELL.
              // Let's remove the `minEarningsGrowth <= 0` check in validation, as negative growth is a SELL criterion.
         }

        if (initialHistoricalData == null || initialHistoricalData.size() < REQUIRED_DATA_SIZE) {
             throw new DataProcessingException("Insufficient initial data (" + (initialHistoricalData == null ? 0 : initialHistoricalData.size())
                                                + "). Required: " + REQUIRED_DATA_SIZE + " data point.");
        }


        this.maxPERatio = maxPERatio;
        this.minEarningsGrowth = minEarningsGrowth;
        // Make a defensive copy of the initial data
        this.historicalData = new ArrayList<>(initialHistoricalData);

        // Perform initial indicator calculation upon construction
        // This calculates indicators based on the latest data in the initial list.
        calculateIndicators();
    }


    /**
     * Updates the internal dataset with new data points and recalculates the indicators.
     * This method should be called whenever new market data becomes available,
     * typically with one new data point during a backtesting iteration.
     *
     * <p>For fundamental data strategies, it's assumed that the {@code StockData} object
     * provided contains the most recent relevant fundamental information (like EPS)
     * as of that data point's date, even if earnings were reported earlier.</p>
     *
     * @param newData A list of new stock data entries to append. Should ideally be in chronological order.
     * @throws DataProcessingException if the calculation fails after adding new data due to insufficient or invalid data.
     */
    @Override
    public void updateData(List<StockData> newData) {
        if (newData == null || newData.isEmpty()) {
            // No new data to process, simply return.
            return;
        }

        // Append the new data points to the historical data
        this.historicalData.addAll(newData);

        // Trim old data to prevent the historicalData list from growing indefinitely.
        // For this strategy, we only need the latest data point for calculations,
        // but keeping a small buffer might be useful or required by the backtesting engine structure.
        // Keeping a size of REQUIRED_DATA_SIZE (1) or slightly more. Let's keep a buffer of 5 for safety/flexibility.
        int requiredHistorySize = REQUIRED_DATA_SIZE + 5;
        if (this.historicalData.size() > requiredHistorySize) {
            this.historicalData = new ArrayList<>(this.historicalData.subList(
                    this.historicalData.size() - requiredHistorySize, this.historicalData.size()));
        }

        // Recalculate indicators based on the latest data point after appending.
        // This assumes the latest data point in the updated historicalData list
        // has the current and previous EPS relevant for its date.
        calculateIndicators();
    }

    /**
     * Returns the minimum number of historical data points required for this strategy
     * to calculate its indicators.
     *
     * <p>Since this value investing strategy primarily uses the latest data point
     * with embedded current and previous EPS, it requires a minimum of 1 data point.</p>
     *
     * @return The minimum number of data points required (which is 1).
     */
    @Override
    public int getLookbackPeriod() {
        // Based on the assumption that StockData contains both current and previous EPS.
        return REQUIRED_DATA_SIZE; // Should be 1
    }

    /**
     * Calculates the PE ratio and earnings growth based on the latest stock data.
     * Assumes the latest data point in {@code historicalData} contains the most
     * recent relevant earnings per share data.
     *
     * @throws DataProcessingException if data is missing or invalid
     */
    @Override
    public void calculateIndicators() {
        if (historicalData == null || historicalData.isEmpty()) {
            // This state should ideally not be reached if updateData is called correctly.
             throw new DataProcessingException("Historical stock data is null or empty, cannot calculate indicators.");
        }
         if (historicalData.size() < REQUIRED_DATA_SIZE) {
              // This should be caught by updateData/constructor, but defensive check here.
              throw new DataProcessingException("Insufficient historical data (" + historicalData.size() + ") to calculate indicators. Required: " + REQUIRED_DATA_SIZE);
         }


        StockData currentStock = historicalData.get(historicalData.size() - 1);

        double epsCurrent = currentStock.getCurrentEarningsPerShare();
        double epsPrevious = currentStock.getPreviousEarningsPerShare();
        double currentPrice = currentStock.getClose();

        // Validate fundamental data points before calculation
        if (currentPrice <= 0) {
             // Cannot calculate PE with zero or negative price.
             throw new DataProcessingException("Invalid current price (" + currentPrice + ") for stock " + currentStock.getSymbol() + " on " + currentStock.getDate());
        }
         if (epsCurrent <= 0) {
             // Cannot calculate valid PE or earnings growth with non-positive current EPS.
             throw new DataProcessingException("Invalid current EPS (" + epsCurrent + ") for stock " + currentStock.getSymbol() + " on " + currentStock.getDate());
         }
         if (epsPrevious <= 0) {
              // Cannot calculate earnings growth with non-positive previous EPS.
              throw new DataProcessingException("Invalid previous EPS (" + epsPrevious + ") for stock " + currentStock.getSymbol() + " on " + currentStock.getDate());
         }


        PERatio pe = new PERatio(currentPrice, epsCurrent);
        EarningsGrowth growth = new EarningsGrowth(epsCurrent, epsPrevious);

        this.calculatedPE = pe.calculate();
        this.earningsGrowth = growth.calculateGrowth();

        // Optional: Log calculated indicators for debugging
        // AppLogger.debug(String.format("Calculated Indicators on %s: PE = %.2f, Earnings Growth = %.2f%%",
        //                 currentStock.getDate(), calculatedPE, earningsGrowth * 100));
    }

    /**
     * Determines if a buy signal should be issued based on the current conditions.
     * Requires that {@link #calculateIndicators()} has been called to update indicator values
     * based on the latest data. This method should only be called when there is sufficient data
     * in the strategy's historical data list (i.e., at least {@code getLookbackPeriod()} points).
     *
     * @return true if buy criteria are met, false otherwise
     * @throws DataProcessingException if there is insufficient historical data to determine the signal.
     */
    @Override
    public boolean shouldBuy() {
        // Ensure we have enough data to make a decision.
         if (historicalData == null || historicalData.size() < REQUIRED_DATA_SIZE) {
             throw new DataProcessingException("Insufficient historical data (" + (historicalData == null ? 0 : historicalData.size())
                                                + ") to determine buy signal. Required: " + REQUIRED_DATA_SIZE);
        }

        // Indicators are calculated in calculateIndicators, which is called by updateData.
        // We just need to check the conditions based on the last calculated values.
        boolean buy = calculatedPE < maxPERatio && earningsGrowth >= minEarningsGrowth;

        // Logging moved to the BacktestEngine for centralized trade logging.
        // If detailed logging is needed within the strategy, it can be added here
        // perhaps conditionally based on a strategy-level debug flag.

        return buy;
    }

    /**
     * Determines if a sell signal should be issued based on the current conditions.
     * Requires that {@link #calculateIndicators()} has been called to update indicator values
     * based on the latest data. This method should only be called when there is sufficient data
     * in the strategy's historical data list (i.e., at least {@code getLookbackPeriod()} points).
     *
     * @return true if sell criteria are met, false otherwise
     * @throws DataProcessingException if there is insufficient historical data to determine the signal.
     */
    @Override
    public boolean shouldSell() {
        // Ensure we have enough data to make a decision.
        if (historicalData == null || historicalData.size() < REQUIRED_DATA_SIZE) {
             throw new DataProcessingException("Insufficient historical data (" + (historicalData == null ? 0 : historicalData.size())
                                                + ") to determine sell signal. Required: " + REQUIRED_DATA_SIZE);
        }

        // Indicators are calculated in calculateIndicators, which is called by updateData.
        // We just need to check the conditions based on the last calculated values.
        boolean sell = calculatedPE >= maxPERatio || earningsGrowth < 0;

         // Logging moved to the BacktestEngine.

        return sell;
    }
}