package de.Linus122.TimeIsMoney;

import de.Linus122.TimeIsMoney.data.MySQLPluginData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.Date;
import java.util.List;
import java.util.logging.Level;

public class Listeners implements Listener {
    Main main;
    public Listeners(Main main) {
        this.main = main;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event){
        main.getPluginData().getPlayerData(event.getPlayer());

        if(main.getPluginData() instanceof MySQLPluginData) {
            Date lastPlayed = new Date(event.getPlayer().getLastPlayed());

            MySQLPluginData pluginData = (MySQLPluginData) main.getPluginData();
            List<Integer> pending = pluginData.getPendingPayouts(lastPlayed, event.getPlayer());

            pending.forEach(k -> {
                System.out.println("paying user from pending payout: " + k);
                main.pay(event.getPlayer(), main.getPayouts().get(k), main.getPluginData().getPlayerData(event.getPlayer()).getPayoutData(k));
            });

        }
    }
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if(main.getPluginData() instanceof MySQLPluginData) {
            ((MySQLPluginData) main.getPluginData()).savePlayerData(event.getPlayer().getUniqueId(), main.getPluginData().getPlayerData(event.getPlayer()));

            main.getLogger().log(Level.ALL, "Updated payout data for player " + event.getPlayer().getName() + " in MySQL");
        }
    }
}
