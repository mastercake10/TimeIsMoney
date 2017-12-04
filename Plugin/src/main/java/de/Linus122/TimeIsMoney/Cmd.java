package de.Linus122.TimeIsMoney;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import static de.Linus122.TimeIsMoney.tools.Utils.CC;

/**
 * TimeIsMoney command listener.
 *
 * @author Linus122
 * @since 1.9.6.1
 */
class Cmd implements CommandExecutor {
	private final Main main;
	
	/**
	 * Creates a new cmd instance with the {@link de.Linus122.TimeIsMoney.Main} class.
	 * @param main The {@link de.Linus122.TimeIsMoney.Main} class.
	 */
	Cmd(Main main) {
		this.main = main;
	}
	
	@Override
	public boolean onCommand(CommandSender cs, Command arg1, String arg2, String[] arg3) {
		if (cs.hasPermission("tim.reload")) {
			main.reload();
			cs.sendMessage(CC("&aTime is Money &cv" + Main.PL_VERSION + " &areloaded!"));
		}
		return true;
	}
}
