package de.Linus122.TimeIsMoney;

import de.Linus122.TimeIsMoney.data.MySQLPluginData;
import de.Linus122.TimeIsMoney.data.PayoutData;
import de.Linus122.TimeIsMoney.data.PlayerData;
import de.Linus122.TimeIsMoney.tools.Utils;
import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
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
        TaskToPlayer(event.getPlayer());
    }
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if(main.getPluginData() instanceof MySQLPluginData) {
            ((MySQLPluginData) main.getPluginData()).savePlayerData(event.getPlayer().getUniqueId(), main.getPluginData().getPlayerData(event.getPlayer()));

            main.getLogger().log(Level.ALL, "Updated payout data for player " + event.getPlayer().getName() + " in MySQL");
        }
        TaskToPlayer(event.getPlayer());
    }

    static HashMap<Player, ScheduledTask> taskIds = new HashMap<Player, ScheduledTask>();
    public static void TaskToPlayer(Player player) {
        if (taskIds.containsKey(player)) {
            taskIds.get(player).cancel();
            taskIds.remove(player);
            return;
        }



        EntityScheduler scheduler = player.getScheduler();

        Consumer<ScheduledTask> entityTask = entity -> {
            if (Main.disabledWorlds.contains(player.getWorld().getName()))
                return;

            String intervalString = Main.plugin.getConfig().getString("global_interval", Main.plugin.getConfig().getInt("give_money_every_second") + "s");
            int globalTimerSeconds = Utils.parseTimeFormat(intervalString);

            PlayerData playerData = Main.plugin.pluginData.getPlayerData(player);

            for(Payout payout : Main.plugin.getApplicablePayoutsForPlayer(player)) {
                PayoutData playerPayoutData = playerData.getPayoutData(payout.id);
                playerPayoutData.setSecondsSinceLastPayout(playerPayoutData.getSecondsSinceLastPayout() + 1);

                int intervalSeconds = payout.interval != 0 ? payout.interval : globalTimerSeconds;

                if (playerPayoutData.getSecondsSinceLastPayout() >= intervalSeconds) {
                    // new payout triggered, handling the payout
                    Main.plugin.pay(player, payout, playerPayoutData);

                    if(Main.plugin.pluginData instanceof MySQLPluginData) {
                        // let other servers know of this payout
                        ((MySQLPluginData) Main.plugin.pluginData).createPendingPayout(player);
                    }
                    playerPayoutData.setSecondsSinceLastPayout(0);
                }
            }
        };

        Runnable endTask = () -> {
            // Este código se ejecutará una vez que todas las entidades hayan sido procesadas
            // Generalmente este código se utiliza para tareas de limpieza o para actualizar el estado global
        };

        long initialDelay = 1;
        long period = 20;

        ScheduledTask task = scheduler.runAtFixedRate(Main.plugin, entityTask, endTask, initialDelay, period);
        taskIds.put(player, task);
    }
}
