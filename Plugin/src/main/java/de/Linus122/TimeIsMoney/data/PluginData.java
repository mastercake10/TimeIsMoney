package de.Linus122.TimeIsMoney.data;

import de.Linus122.TimeIsMoney.Main;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public abstract class PluginData {
	/**
	 * Data for each player
	 */
	protected HashMap<UUID, PlayerData> playerDataMap = new HashMap<>();
	protected Main plugin;

	public PluginData(Main main) {
		this.plugin = main;
	}

	public abstract PlayerData getPlayerData(Player player);
	public abstract void saveData();
	public abstract void loadData();

}
