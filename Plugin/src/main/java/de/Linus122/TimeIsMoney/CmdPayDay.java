package de.Linus122.TimeIsMoney;

import de.Linus122.TimeIsMoney.data.MySQLPluginData;
import de.Linus122.TimeIsMoney.data.PayoutData;
import de.Linus122.TimeIsMoney.data.PlayerData;
import de.Linus122.TimeIsMoney.tools.Utils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static de.Linus122.TimeIsMoney.tools.Utils.CC;

/**
 * TimeIsMoney command listener.
 *
 * @author Linus122
 * @since 1.9.6.1
 */
class CmdPayDay implements CommandExecutor {
	private final Main main;

	/**
	 * Creates a new cmd instance with the {@link Main} class.
	 * @param main The {@link Main} class.
	 */
	CmdPayDay(Main main) {
		this.main = main;
	}
	
	@Override
	public boolean onCommand(CommandSender cs, Command arg1, String arg2, String[] arg3) {
		if (!(cs instanceof Player))
			return true;

		Player player = (Player) cs;

		if (cs.hasPermission("tim.payday")) {
			PlayerData playerData = Main.plugin.pluginData.getPlayerData(player);
			String intervalString = Main.plugin.getConfig().getString("global_interval", Main.plugin.getConfig().getInt("give_money_every_second") + "s");
			int globalTimerSeconds = Utils.parseTimeFormat(intervalString);

			for(Payout payout : Main.plugin.getApplicablePayoutsForPlayer(player)) {
				PayoutData playerPayoutData = playerData.getPayoutData(payout.id);
				playerPayoutData.setSecondsSinceLastPayout(playerPayoutData.getSecondsSinceLastPayout() + 1);

				int intervalSeconds = payout.interval != 0 ? payout.interval : globalTimerSeconds;
				player.sendMessage(CC("&aTu siguiente pago es en: &c" + toPrettyString(intervalSeconds-playerPayoutData.getSecondsSinceLastPayout())));
			}
		} else {
			player.sendMessage(CC("&cYou don't have the permission to do that."));
		}
		return true;
	}

	private String toPrettyString(int seconds) {
		int hours = seconds / 3600;
		int minutes = (seconds % 3600) / 60;
		int secs = seconds % 60;
		return String.format("%02d:%02d:%02d", hours, minutes, secs);
	}
}
