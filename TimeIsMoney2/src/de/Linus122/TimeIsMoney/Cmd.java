package de.Linus122.TimeIsMoney;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class Cmd implements CommandExecutor {
	Main main;
	public Cmd(Main main) {
		this.main = main;
	}

	@Override
	public boolean onCommand(CommandSender cs, Command arg1, String arg2, String[] arg3) {
		if(cs.hasPermission("tim.reload")){
			main.reload();
			cs.sendMessage("§aReloaded!");
			
		}
		return true;
	}

}
