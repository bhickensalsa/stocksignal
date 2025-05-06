package com.stocksignal.data;

import java.time.LocalDate;

/**
 * {@code StockData} represents a single day's trading information for a specific stock.
 *
 * <p>This includes open, close, high, low prices, trading volume, and the date of the data point.
 */
public class StockData {
    
    /** The date for which this stock data applies */
    private LocalDate date;

    /** The price at which the stock opened on this day */
    private double open;

    /** The price at which the stock closed on this day */
    private double close;

    /** The highest price the stock reached during the day */
    private double high;

    /** The lowest price the stock reached during the day */
    private double low;

    /** The total number of shares traded during the day */
    private long volume;

    /**
     * Constructs a new {@code StockData} object with the given parameters.
     *
     * @param date the trading date
     * @param open the opening price
     * @param close the closing price
     * @param high the highest price of the day
     * @param low the lowest price of the day
     * @param volume the total trading volume
     */
    public StockData(LocalDate date, double open, double close, double high, double low, long volume) {
        this.date = date;
        this.open = open;
        this.close = close;
        this.high = high;
        this.low = low;
        this.volume = volume;
    }

    /**
     * @return the date of the stock data
     */
    public LocalDate getDate() { return date; }

    /**
     * @return the opening price
     */
    public double getOpen() { return open; }

    /**
     * @return the closing price
     */
    public double getClose() { return close; }

    /**
     * @return the highest price during the day
     */
    public double getHigh() { return high; }

    /**
     * @return the lowest price during the day
     */
    public double getLow() { return low; }

    /**
     * @return the trading volume
     */
    public long getVolume() { return volume; }
}
