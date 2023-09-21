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

public class YamlPluginData extends PluginData{
    final String filePath = "plugins/TimeIsMoney/data/";
    final File dataFile = new File(filePath + "data.yml");

    public YamlPluginData(Main main) {
        super(main);
    }

    /**
     * Loads data from file if {@link #dataFile} exists.
     */

    @SuppressWarnings("unchecked")
    public void loadData() {

        if(dataFile.exists()) {
            YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(dataFile);
            playerDataMap = new HashMap<>();

            for(String key : yamlConfiguration.getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                PlayerData playerData = new PlayerData();
                for(String payout_id : yamlConfiguration.getConfigurationSection(key).getKeys(false)) {
                    try {
                        Integer.parseInt(payout_id);
                    } catch(NumberFormatException e) {
                        continue;
                    }
                    double receivedToday = yamlConfiguration.getDouble(key + "." + payout_id + ".receivedToday");
                    int secondsOnline = yamlConfiguration.getInt(key + "." + payout_id + ".secondsSinceLastPayout");
                    Date date = yamlConfiguration.getObject(key + "." + payout_id + ".lastPayoutDate", Date.class);

                    PayoutData payoutData = new PayoutData(receivedToday, date, secondsOnline);
                    playerData.getPayoutDataMap().put(Integer.parseInt(payout_id), payoutData);
                }

                playerDataMap.put(uuid, playerData);
            }
        }

        if(playerDataMap == null)
            playerDataMap = new HashMap<>();

    }

    /**
     * Saves the data on disc to file {@link #dataFile}
     */
    public void saveData() {
        if(!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
        }
        YamlConfiguration yamlConfiguration = new YamlConfiguration();
        super.playerDataMap.forEach((uuid, playerData) -> {
            playerData.getPayoutDataMap().forEach((payout_id, payoutData) -> {
                yamlConfiguration.set(uuid + "." + payout_id + ".receivedToday" , payoutData.getReceivedToday());
                yamlConfiguration.set(uuid + "." + payout_id + ".secondsSinceLastPayout" , payoutData.getSecondsSinceLastPayout());
                yamlConfiguration.set(uuid + "." + payout_id + ".lastPayoutDate" , payoutData.getLastPayoutDate());
            });

        });
        try {
            yamlConfiguration.save(dataFile);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public PlayerData getPlayerData(@NotNull Player player) {
        if(!this.playerDataMap.containsKey(player.getUniqueId())) {
            // create a new PlayerData object
            PlayerData playerData = new PlayerData();

            this.playerDataMap.put(player.getUniqueId(), playerData);
            return playerData;
        }
        return playerDataMap.get(player.getUniqueId());
    }
}
