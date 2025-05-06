package com.stocksignal.execution;

/**
 * The DiscordNotifier class implements the TradeNotifier interface and is responsible for sending 
 * trade-related notifications to Discord.
 */
public class DiscordNotifier implements TradeNotifier {

    /**
     * Sends a notification message to Discord.
     * Example logic to simulate sending a trade notification to Discord.
     * 
     * @param message the message to be sent as a notification
     */
    public void notify(String message) {
        System.out.println("Sending Discord notification: " + message);
    }
}
