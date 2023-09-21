package de.Linus122.TimeIsMoney.tools;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		if(s == null) {
			return "";
		}
		return ChatColor.translateAlternateColorCodes('&', parseRGB(s));
	}
	
	public static String applyPlaceholders(Player player, String s) {
        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
        	s = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, s);
        }
        return s;
	}

	private static final Pattern rgbColor = Pattern.compile("(?<!\\\\)(&#[a-fA-F0-9]{6})");

	public static String parseRGB(String msg) {
		Matcher matcher = rgbColor.matcher(msg);
		while (matcher.find()) {
			String color = msg.substring(matcher.start(), matcher.end());
			String hex = color.replace("&", "");
			try {
				msg = msg.replace(color, String.valueOf(net.md_5.bungee.api.ChatColor.of(hex)));
			} catch(NoSuchMethodError e) {
				// older spigot versions

				msg = msg.replace(color, "");
			}
			matcher = rgbColor.matcher(msg);
		}
		return msg;
	}

	/**
	 * Return the seconds of a time format, valid suffixes are s (seconds), m (minutes) and h (hours)
	 * @param format the formatted time, examples: 10m, 8h, 30s.
	 * @return converted seconds, invalid return -1
	 */
	public static int parseTimeFormat(String format) {
		Pattern pattern = Pattern.compile("^(?:(\\d+)s|(\\d+)m|(\\d+)h)$");
		Matcher matcher = pattern.matcher(format);

		if (matcher.matches()) {
			String seconds = matcher.group(1);
			String minutes = matcher.group(2);
			String hours = matcher.group(3);

			if (seconds != null) {
				return Integer.parseInt(seconds);
			}
			if (minutes != null) {
				return Integer.parseInt(minutes) * 60;
			}
			if (hours != null) {
				return Integer.parseInt(hours) * 60 * 60;
			}
		}
		return -1;
	}
}
