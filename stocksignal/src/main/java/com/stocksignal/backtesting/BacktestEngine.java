package com.stocksignal.backtesting;

import com.stocksignal.data.StockData;
import com.stocksignal.strategies.Strategy;
import com.stocksignal.utils.AppLogger;  // Import the logger class

import java.util.ArrayList;
import java.util.List;

/**
 * The BacktestEngine class is responsible for simulating trades over historical stock data
 * using a given trading strategy and tracking portfolio performance.
 */
public class BacktestEngine {

    private final Strategy strategy;
    private final List<StockData> historicalData;
    private final double initialCapital;
    private final double transactionFee;
    private final boolean detailed;

    private double cash;
    private double sharesHeld;
    private final List<String> tradeLog;

    /**
     * Constructs a BacktestEngine.
     *
     * @param strategy        the strategy to be tested
     * @param historicalData  the stock data to simulate trades on
     * @param initialCapital  starting amount of capital
     * @param transactionFee  flat fee per trade (can be 0)
     * @param detailed        whether to print detailed logs
     */
    public BacktestEngine(Strategy strategy, List<StockData> historicalData,
                          double initialCapital, double transactionFee, boolean detailed) {
        this.strategy = strategy;
        this.historicalData = historicalData;
        this.initialCapital = initialCapital;
        this.transactionFee = transactionFee;
        this.detailed = detailed;

        this.cash = initialCapital;
        this.sharesHeld = 0;
        this.tradeLog = new ArrayList<>();
    }

    /**
     * Executes the backtest using the provided strategy and stock data.
     */
    public void runBacktest() {
        // Ensure initial data is sufficient for the strategy's lookback
        int requiredInitialData = strategy.getLookbackPeriod();
        if (historicalData.size() < requiredInitialData) {
             AppLogger.error("Insufficient historical data for the strategy's lookback period.");
             return;
        }
    
        // Initialize the strategy with the minimum required historical data
        strategy.updateData(new ArrayList<>(historicalData.subList(0, requiredInitialData)));
    
        // Iterate through the rest of the data, providing one day at a time
        for (int i = requiredInitialData; i < historicalData.size(); i++) {
            StockData currentDayData = historicalData.get(i);
    
            // Provide the new data point to the strategy
            List<StockData> newDataForDay = new ArrayList<>();
            newDataForDay.add(currentDayData);
            strategy.updateData(newDataForDay); // Strategy appends and recalculates
    
            double price = currentDayData.getClose();
    
            // Now, the strategy's shouldBuy/shouldSell methods will use indicators
            // calculated based on data up to and including currentDayData.
    
            AppLogger.debug(String.format("Day %s: shouldBuy=%s, shouldSell=%s", currentDayData.getDate(), strategy.shouldBuy(), strategy.shouldSell()));
            AppLogger.debug(String.format("Current cash: $%.2f, Shares held: %.2f", cash, sharesHeld));
    
    
            // Buy decision
            // Add checks to ensure strategy has enough data points after updateData
            // before making a decision, although updateData should handle this.
            if (strategy.shouldBuy() && cash > 0) {
                 if (price > 0) { // Avoid division by zero
                    sharesHeld = (cash - transactionFee) / price;
                    cash = 0;
                    logTrade(String.format("BUY on %s @ %.2f. Shares acquired: %.2f", currentDayData.getDate(), price, sharesHeld));
                 } else {
                     AppLogger.warn("Attempted to buy with zero or negative price on " + currentDayData.getDate());
                 }
            }
            // Sell decision
            else if (strategy.shouldSell() && sharesHeld > 0) {
                cash = sharesHeld * price - transactionFee;
                sharesHeld = 0;
                logTrade(String.format("SELL on %s @ %.2f. Cash received: $%.2f", currentDayData.getDate(), price, cash));
            }
        }
    
        // Calculate final value of the portfolio using the *last* closing price
        double lastClosingPrice = historicalData.get(historicalData.size() - 1).getClose();
        double finalValue = cash + sharesHeld * lastClosingPrice;
        logTrade(String.format("Final portfolio value: $%.2f", finalValue));
        evaluateStrategy(finalValue);
    }

    /**
     * Outputs summary statistics about the strategy performance.
     */
    public void evaluateStrategy(double finalValue) {
        double returnPct = ((finalValue - initialCapital) / initialCapital) * 100.0;

        // Log all the formatted strings directly
        AppLogger.info(String.format("---------- Backtest Summary ----------"));
        AppLogger.info(String.format("Initial Capital: $%.2f", initialCapital));
        AppLogger.info(String.format("Final Value:     $%.2f", finalValue));
        AppLogger.info(String.format("Return:          %.2f%%", returnPct));
        AppLogger.info(String.format("Trades executed: %d", tradeLog.size()));
        AppLogger.info(String.format("--------------------------------------"));

        // If detailed is true, log each trade from the trade log
        if (detailed) {
            AppLogger.info("Trade Log:");
            tradeLog.forEach(AppLogger::info); // Log each trade using AppLogger
        }
    }

    private void logTrade(String entry) {
        tradeLog.add(entry);
        if (detailed) AppLogger.info(entry); // Log the trade info using AppLogger
    }
}
