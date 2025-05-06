package com.stocksignal.strategies;

import com.stocksignal.data.StockData;
import com.stocksignal.exceptions.DataProcessingException;
import com.stocksignal.indicators.technical.MACD;
import com.stocksignal.indicators.technical.SMA;

import java.util.List;

/**
 * The TrendFollowingStrategy combines a long-term SMA with MACD to identify trend-based buy/sell signals.
 */
public class TrendFollowingStrategy implements Strategy {

    private final List<StockData> historicalData;
    private final int smaPeriod;
    private final MACD macd;
    private final SMA sma;

    private double currentSMA;
    private double macdLine;
    private double macdSignal;

    /**
     * Constructs a TrendFollowingStrategy with historical data and initializes indicators.
     *
     * @param historicalData The list of historical stock data.
     * @param smaPeriod      The period for the SMA (e.g., 200).
     */
    public TrendFollowingStrategy(List<StockData> historicalData, int smaPeriod) {
        if (historicalData == null || historicalData.size() < smaPeriod) {
            throw new DataProcessingException("Insufficient data for TrendFollowingStrategy.");
        }
        this.historicalData = historicalData;
        this.smaPeriod = smaPeriod;
        this.sma = new SMA(smaPeriod);
        this.macd = new MACD(12, 26, 9);
    }

    /**
     * Precomputes the indicators (SMA, MACD) to be used in the decision methods.
     */
    @Override
    public void calculateIndicators() {
        try {
            List<StockData> recentData = historicalData.subList(historicalData.size() - smaPeriod, historicalData.size());

            this.currentSMA = sma.calculate(recentData);
            double[] macdValues = macd.calculate(recentData, true);
            this.macdLine = macdValues[0];
            this.macdSignal = macdValues[1];

        } catch (DataProcessingException e) {
            throw new DataProcessingException("Failed to calculate indicators: " + e.getMessage());
        }
    }

    /**
     * Determines a buy signal based on the current price being above the SMA and MACD crossover.
     */
    @Override
    public boolean shouldBuy(StockData stock) {
        return stock.getClose() > currentSMA && macdLine > macdSignal;
    }

    /**
     * Determines a sell signal based on the current price being below the SMA and MACD crossover.
     */
    @Override
    public boolean shouldSell(StockData stock) {
        return stock.getClose() < currentSMA && macdLine < macdSignal;
    }
}
