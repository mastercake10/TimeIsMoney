package de.Linus122.TimeIsMoney;

import org.bukkit.ChatColor;

public class Utils {

    public Utils() {
        throw new RuntimeException("Utils class should not be instantiated!");
    }

    /**
     * Utility method which converts &<color> bukkit colors to real bukkit colors which correct symbol
     * @param s string to convert
     * @return converted string
     */
    public static String CC(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
