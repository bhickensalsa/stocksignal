package com.stocksignal.execution;

/**
 * The TradeNotifier interface defines the contract for sending trade-related notifications.
 * Any class implementing this interface must provide a method to notify with a message.
 */
public interface TradeNotifier {

    /**
     * Sends a trade notification with the given message.
     * 
     * @param message the notification message to be sent
     */
    void notify(String message);
}
