package com.stocksignal.data;

import java.time.LocalDate;

/**
 * {@code StockData} represents a single day's trading information for a specific stock.
 * This includes open, close, high, low prices, trading volume, and earnings per share.
 */
public class StockData {

    private String symbol;
    private LocalDate date;
    private double open;
    private double close;
    private double high;
    private double low;
    private long volume;
    private double currentEarningsPerShare;  // Current EPS
    private double previousEarningsPerShare; // Previous EPS

    // Constructor
    public StockData(String symbol, LocalDate date, double open, double close, double high, double low, 
                     long volume, double currentEarningsPerShare, double previousEarningsPerShare) {
        this.symbol = symbol;
        this.date = date;
        this.open = open;
        this.close = close;
        this.high = high;
        this.low = low;
        this.volume = volume;
        this.currentEarningsPerShare = currentEarningsPerShare;  // Store current EPS
        this.previousEarningsPerShare = previousEarningsPerShare; // Store previous EPS
    }

    // Getters for all fields
    public String getSymbol() {
        return symbol;
    }

    public LocalDate getDate() {
        return date;
    }

    public double getOpen() {
        return open;
    }

    public double getClose() {
        return close;
    }

    public double getHigh() {
        return high;
    }

    public double getLow() {
        return low;
    }

    public long getVolume() {
        return volume;
    }

    public double getCurrentEarningsPerShare() {
        return currentEarningsPerShare;
    }

    public double getPreviousEarningsPerShare() {
        return previousEarningsPerShare;
    }
}
