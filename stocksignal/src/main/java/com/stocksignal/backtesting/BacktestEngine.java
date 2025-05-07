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
        // No lookback period is needed, data is assumed to be clean and trimmed

        // Iterate from the earliest available data to the most recent data
        for (int i = 0; i < historicalData.size(); i++) {
            // Sublist includes all data from the start to the current day (i)
            List<StockData> dataSlice = historicalData.subList(i, historicalData.size());
            strategy.updateData(dataSlice); // Update internal indicators or calculations

            StockData currentDay = historicalData.get(i);
            double price = currentDay.getClose();

            AppLogger.debug(String.format("Day %s: shouldBuy=%s, shouldSell=%s", currentDay.getDate(), strategy.shouldBuy(), strategy.shouldSell()));

            // Debugging: Output current portfolio status
            AppLogger.debug(String.format("Current cash: $%.2f, Shares held: %.2f", cash, sharesHeld));

            // Buy decision
            if (strategy.shouldBuy() && cash > 0) {
                sharesHeld = (cash - transactionFee) / price;
                cash = 0;
                logTrade(String.format("BUY on %s @ %.2f", currentDay.getDate(), price));
            }
            // Sell decision
            else if (strategy.shouldSell() && sharesHeld > 0) {
                cash = sharesHeld * price - transactionFee;
                sharesHeld = 0;
                logTrade(String.format("SELL on %s @ %.2f", currentDay.getDate(), price));
            }
        }

        // Calculate final value of the portfolio
        double finalValue = cash + sharesHeld * historicalData.get(0).getClose(); // Use the most recent closing price
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
