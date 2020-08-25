package de.Linus122.TimeIsMoney;

import org.bukkit.OfflinePlayer;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;


public class NamePlaceholder extends PlaceholderExpansion {

	Main timeIsMoney;

	public NamePlaceholder(Main plugin) {
		this.timeIsMoney = plugin;
	}

	// This tells PlaceholderAPI to not unregister your expansion on reloads since it is provided by the dependency
	// Introduced in PlaceholderAPI 2.8.5
	@Override
	public boolean persist() {
		return true;
	}

	// Our placeholders will be %tim <params>%
	@Override
	public String getIdentifier() {
		return "tim";
	}

	// the author
	@Override
	public String getAuthor() {
		return "Linus122";
	}

	// This is the version
	@Override
	public String getVersion() {
		return timeIsMoney.getDescription().getVersion();
	}

	@Override
	public String onRequest(OfflinePlayer player, String label) {
		switch(label) {
			case "atm":
				return Main.economy.format(ATM.getBankBalance(player, null));
		}
		return null;
	}
}
