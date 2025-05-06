package com.stocksignal.data;

import java.util.Date;

/**
 * The StockData class represents stock information for a specific ticker, 
 * including its price, volume, and the date of the data.
 */
public class StockData {
    
    private String ticker;
    private double price;
    private double volume;
    private Date date;

    /**
     * Gets the ticker symbol for the stock.
     * 
     * @return the ticker symbol of the stock
     */
    public String getTicker() {
        return ticker;
    }

    /**
     * Sets the ticker symbol for the stock.
     * 
     * @param ticker the ticker symbol of the stock
     */
    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    /**
     * Gets the price of the stock.
     * 
     * @return the price of the stock
     */
    public double getPrice() {
        return price;
    }

    /**
     * Sets the price of the stock.
     * 
     * @param price the price of the stock
     */
    public void setPrice(double price) {
        this.price = price;
    }

    /**
     * Gets the trading volume of the stock.
     * 
     * @return the volume of the stock traded
     */
    public double getVolume() {
        return volume;
    }

    /**
     * Sets the trading volume of the stock.
     * 
     * @param volume the volume of the stock traded
     */
    public void setVolume(double volume) {
        this.volume = volume;
    }

    /**
     * Gets the date the stock data was recorded.
     * 
     * @return the date of the stock data
     */
    public Date getDate() {
        return date;
    }

    /**
     * Sets the date for the stock data.
     * 
     * @param date the date the stock data was recorded
     */
    public void setDate(Date date) {
        this.date = date;
    }
}
