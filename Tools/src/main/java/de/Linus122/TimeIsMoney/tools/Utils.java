package de.Linus122.TimeIsMoney.tools;

import org.bukkit.ChatColor;

/**
 * Utility class.
 *
 * @author Linus122
 * @since 1.9.6.1
 */
public class Utils {
	/**
	 * @throws RuntimeException utils class should not be instantiated.
	 */
	public Utils() {
		throw new RuntimeException("Utils class should not be instantiated!");
	}
	
	/**
	 * Converts &color to {@link org.bukkit.ChatColor}.
	 *
	 * @param s The string to convert to {@link org.bukkit.ChatColor}.
	 * @return The converted string with {@link org.bukkit.ChatColor}.
	 */
	public static String CC(String s) {
		// return an empty string if given string is null
		if(s != null) {
			return "";
		}
		return ChatColor.translateAlternateColorCodes('&', s);
	}
}
