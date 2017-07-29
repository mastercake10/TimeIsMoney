package de.Linus122.TimeIsMoney;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

class Cmd implements CommandExecutor {
	private final Main main;
	Cmd(Main main) {
		this.main = main;
	}

	@Override
	public boolean onCommand(CommandSender cs, Command arg1, String arg2, String[] arg3) {
		if(cs.hasPermission("tim.reload")){
			main.reload();
			cs.sendMessage(ChatColor.translateAlternateColorCodes('&',"&aTime is Money &cv" + Main.PL_VERSION + " &areloaded!"));
			
		}
		return true;
	}

}
