package de.Linus122.TimeIsMoney.tools;

import org.bukkit.entity.Player;

/**
 * Interface that allows sending of actionbar messages for different versions of Spigot.
 *
 * @author Linus122
 * @since 1.9.6.1
 */
@FunctionalInterface
public interface ActionBarUtils {
	/**
	 * Sends an actionbar message to the specified player.
	 *
	 * @param p The player to send the actionbar message to.
	 * @param message The message the actionbar should give to the player.
	 */
	void sendActionBarMessage(Player p, String message);
}
