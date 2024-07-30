package de.Linus122.TimeIsMoney;

import static de.Linus122.TimeIsMoney.tools.Utils.CC;
import static de.Linus122.TimeIsMoney.tools.Utils.applyPlaceholders;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import de.Linus122.TimeIsMoney.data.*;
import de.Linus122.TimeIsMoney.tools.Utils;
import fr.euphyllia.energie.Energie;
import fr.euphyllia.energie.model.Scheduler;
import fr.euphyllia.energie.model.SchedulerType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import com.earth2me.essentials.Essentials;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;

import xyz.spaceio.metrics.Metrics;
import xyz.spaceio.spacegui.GUIProvider;

/**
 * The main class for TimeIsMoney
 *
 * @author Linus122
 * @since 1.9.6.1
 */
public class Main extends JavaPlugin {
	/**
	 * The economy being used by vault.
	 */
	static net.milkbowl.vault.economy.Economy economy = null;
	/**
	 * The scheduler being used by compatible with folia.
	 */
	public static Scheduler scheduler;
	/**
	 * The config version number.
	 */
	private static final int CFG_VERSION = 12;
	/**
	 * The TimeIsMoney version.
	 */
	static String PL_VERSION;
	/**
	 * The TimeIsMoney config.
	 */
	static FileConfiguration finalconfig;
	/**
	 * The list of worlds where the payout feature will be disabled.
	 */
	private static List<String> disabledWorlds;
	/**
	 * The payouts listed in the config.
	 */
	private final List<Payout> payouts = new ArrayList<>();
	
	/**
	 * Players that have already reached the payout limit for today
	 */
	private Set<UUID> payoutLimitReached = new HashSet<>();
	/**
	 * The last location of each player by UUID.
	 */
	private final HashMap<UUID, Location> lastLocation = new HashMap<>();
	/**
	 * The plugin logger.
	 */
	private final Logger logger = this.getLogger();
	
	/**
	 * Main task for keeping track of player's online time
	 */
	private BukkitTask playtimeWatcherTask;

	private PluginData pluginData;
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({"deprecation"})
	@Override
	public void onEnable() {

		scheduler = new Energie(this).getMinecraftScheduler();
		this.getCommand("timeismoney").setExecutor(new Cmd(this));
		PL_VERSION = this.getDescription().getVersion();
		
		GUIProvider.registerPlugin(this);
		//this.reloadConfig();
	
		
		File config = new File("plugins/TimeIsMoney/config.yml");
		
		if (config.exists()) {
			YamlConfiguration cfg = YamlConfiguration.loadConfiguration(config);
			String old_config = "config_old " + cfg.getInt("configuration-version") + ".yml";
			if (cfg.contains("configuration-version")) {
				if (cfg.getInt("configuration-version") < CFG_VERSION) {
					logger.warning("[TimeIsMoney] &cYOU ARE USING AN OLD CONFIG-VERSION. The plugin CANT work with this.");
					logger.warning("[TimeIsMoney] &cI have created an new config for you. The old one is saved as config_old.yml.");
					config.renameTo(new File("plugins/TimeIsMoney/" + old_config));
				}
			}
			this.saveDefaultConfig();
			for (String key : cfg.getConfigurationSection("").getKeys(true)) {
				if (!this.getConfig().contains(key)) {
					this.getConfig().set(key, cfg.get(key));
				}
			}
		} else {
			this.saveDefaultConfig();
		}
		
		finalconfig = this.getConfig();

		if (this.getConfig().getBoolean("debug-log")) {
			// enable debug level
			getLogger().setLevel(Level.ALL);
		}
		
		disabledWorlds = getConfig().getStringList("disabled_in_worlds");

		if (getConfig().getBoolean("enable_atm")) new ATM(this);

		startPlaytimeWatcher();
		
		// Placeholder API

        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
        	new NamePlaceholder(this).register();
        }

		setupEconomy();

		if (getConfig().contains("mysql")) {
			pluginData = new MySQLPluginData(this, getConfig().getString("mysql.host"),
					getConfig().getInt("mysql.port"), getConfig().getString("mysql.user"),
					getConfig().getString("mysql.database"), getConfig().getString("mysql.password"));

			pluginData.loadData();
		} else {
			pluginData = new YamlPluginData(this);
			pluginData.loadData();
		}

		this.getServer().getPluginManager().registerEvents(new Listeners(this), this);
		
		loadPayouts();
		
		if (Bukkit.getPluginManager().isPluginEnabled("Essentials") && this.getConfig().getBoolean("afk_use_essentials")) {
			logger.info("Time is Money: Essentials found. Hook in it -> Will use Essentials's AFK feature if afk is enabled.");
		}
		if (!Energie.isFolia())
			new Metrics(this);
		
		logger.info(CC("&aTime is Money &2v" + PL_VERSION + " &astarted."));
	}
	
	public void startPlaytimeWatcher() {
		String intervalString = getConfig().getString("global_interval", getConfig().getInt("give_money_every_second") + "s");
		int globalTimerSeconds = Utils.parseTimeFormat(intervalString);

		scheduler.runAtFixedRate(SchedulerType.SYNC, task -> {
			for (Player player : Bukkit.getOnlinePlayers()) {
				if (disabledWorlds.contains(player.getWorld().getName())) continue;
				PlayerData playerData = this.pluginData.getPlayerData(player);

				for(Payout payout : this.getApplicablePayoutsForPlayer(player)) {
					PayoutData playerPayoutData = playerData.getPayoutData(payout.id);
					playerPayoutData.setSecondsSinceLastPayout(playerPayoutData.getSecondsSinceLastPayout() + 1);

					int intervalSeconds = payout.interval != 0 ? payout.interval : globalTimerSeconds;

					if (playerPayoutData.getSecondsSinceLastPayout() >= intervalSeconds) {
						// new payout triggered, handling the payout
						pay(player, payout, playerPayoutData);

						if(this.pluginData instanceof MySQLPluginData) {
							// let other servers know of this payout
							((MySQLPluginData) this.pluginData).createPendingPayout(player);
						}
						playerPayoutData.setSecondsSinceLastPayout(0);
					}
				}


			}
		}, 20L, 20L);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onDisable() {
		scheduler.cancelAllTask();

		this.pluginData.saveData();

	}

	/**
	 * Reloads TimeIsMoney
	 */
	void reload() {
		this.reloadConfig();
		finalconfig = this.getConfig();
		if (getConfig().getBoolean("enable_atm")) new ATM(this);
		loadPayouts();
	}
	
	/**
	 * Loads the payouts.
	 */
	private void loadPayouts() {
		try {
			payouts.clear();
			for (String key : finalconfig.getConfigurationSection("payouts").getKeys(false)) {
				Payout payout = new Payout();
				payout.id = Integer.parseInt(key);
				payout.max_payout_per_day = finalconfig.getDouble("payouts." + key + ".max_payout_per_day");
				payout.payout_amount = finalconfig.getDouble("payouts." + key + ".payout_amount");
				if (finalconfig.isSet("payouts." + key + ".permission")) {
					payout.permission = finalconfig.getString("payouts." + key + ".permission");
				}
				if (finalconfig.isSet("payouts." + key + ".commands")) {
					payout.commands = finalconfig.getStringList("payouts." + key + ".commands");
				}
				if (finalconfig.isSet("payouts." + key + ".commands_if_afk")) {
					payout.commands_if_afk = finalconfig.getStringList("payouts." + key + ".commands_if_afk");
				}
				
				if (finalconfig.isSet("payouts." + key + ".chance")) {
					payout.chance = finalconfig.getDouble("payouts." + key + ".chance");
				}
				if (finalconfig.isSet("payouts." + key + ".interval")) {
					// TODO: Add error message when parsing failed
					payout.interval = Utils.parseTimeFormat(finalconfig.getString("payouts." + key + ".interval"));
				}
				payouts.add(payout);
			}
			logger.info("[TimeIsMoney] &aLoaded " + finalconfig.getConfigurationSection("payouts").getKeys(false).size() + " Payouts!");
		} catch (Exception e) {
			logger.info("[TimeIsMoney] &aFailed to load Payouts! (May made a mistake in config.yml?)");
		}
	}
	
	/**
	 * Sets up the vault economy.
	 *
	 * @return True if the economy was set up, false otherwise.
	 */
	private boolean setupEconomy() {
		RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
		if (economyProvider != null) {
			economy = economyProvider.getProvider();
		}
		
		return economy != null;
	}
	
	/**
	 * Gets a list of applicable payouts for a player. Only returns one payout when using chance.
	 * 
	 * @param player The player to get the payouts of.
	 * @return A list of payouts
	 */
	private List<Payout> getApplicablePayoutsForPlayer(Player player){
		if (!this.getConfig().getBoolean("choose-payout-by-chance")) {
			// Choose applicable payouts by permission
			List<Payout> payouts_ = payouts.stream().filter(payout -> player.hasPermission(payout.permission) || payout.permission.length() == 0).collect(Collectors.toList());

			if(finalconfig.getBoolean("choose-only-one-payout", true)) {
				List<Payout> finalPayouts = new ArrayList<>();
				// add payouts with a custom timer anyways
				finalPayouts.addAll(payouts_.stream().filter(payout -> payout.interval != 0).collect(Collectors.toList()));

				// choose the last element of the payouts that does not have a custom timer
				List<Payout> payoutsWithoutInterval = payouts_.stream().filter(payout -> payout.interval == 0).collect(Collectors.toList());
				finalPayouts.add(payoutsWithoutInterval.get(payoutsWithoutInterval.size() - 1));
				return finalPayouts;
			} else if(this.getConfig().getBoolean("merge-payouts")) {
				// Mering multiple payouts to one
				Payout payout = new Payout();
				payout.id = 1;
				for (Payout payout_ : payouts_) {
					if(payout_.interval != 0) {
						continue;
					}
					payout.commands.addAll(payout_.commands);
					payout.commands_if_afk.addAll(payout_.commands_if_afk);
					payout.payout_amount += payout_.payout_amount;
					payout.max_payout_per_day += payout_.max_payout_per_day;
				}
			}
			return payouts_;
		}else {
			// Get a random payout
			Random rnd = new Random();
			
			double d = rnd.nextDouble() * 100;
			
			for (Payout payout : payouts) {
				if(payout.chance == 0.0) continue;
				if ((d -= payout.chance) < 0){
					return Collections.singletonList(payout);
				}
			}
			
		}
		return Collections.emptyList(); 
	}
	
	/**
	 * Pays the specified player.
	 *
	 * @param player The player to pay.
	 */
	public void pay(Player player, Payout payout, PayoutData payoutPlayerData) {
		if (player == null) return;

		if (payout.max_payout_per_day != -1) {
			if (payoutPlayerData.getReceivedToday() >= payout.max_payout_per_day) { //Reached max payout
				
				if(finalconfig.getBoolean("display-payout-limit-reached-message-once") && payoutLimitReached.contains(player.getUniqueId())) {
					return;
				}
				
				if (finalconfig.getBoolean("display-messages-in-chat")) {
					sendMessage(player, finalconfig.getString("message_payoutlimit_reached"));
				}
				if (finalconfig.getBoolean("display-messages-in-actionbar")) {
					sendActionbar(player, finalconfig.getString("message_payoutlimit_reached_actionbar"));
				}
				if(finalconfig.getBoolean("display-payout-limit-reached-message-once"))
					payoutLimitReached.add(player.getUniqueId());
				return;
			}
		}

		if (!finalconfig.getBoolean("allow-multiple-accounts") && !player.hasPermission("tim.multipleaccountsbypass")) {
			Set<? extends Player> sameAddressPlayers = Bukkit.getOnlinePlayers().stream().filter(p -> p.getAddress().getHostString().equals(p.getAddress().getHostString())).collect(Collectors.toSet());
			int same_address_count = sameAddressPlayers.size();

			if (same_address_count > finalconfig.getInt("max-multiple-accounts")) {
				Optional<? extends Player> firstPlayer = sameAddressPlayers.stream().min(Comparator.comparing(Player::getName));

				if (firstPlayer.isPresent()) {
					// one of the players with multiple ips still should receive a payout
					if (firstPlayer.get() != player) {
						sendMessage(player, finalconfig.getString("message_multiple_ips"));
						return;
					}
				}
			}
		}
		
		//AFK CHECK
		boolean afk = false;
		double afkPercent = 0.0D;
		if (!player.hasPermission("tim.afkbypass")) {
			//ESENTIALS_AFK_FEATURE
			if (Bukkit.getServer().getPluginManager().isPluginEnabled("Essentials") && this.getConfig().getBoolean("afk_use_essentials")) {
				Essentials essentials = (com.earth2me.essentials.Essentials) Bukkit.getServer().getPluginManager().getPlugin("Essentials");
				if (essentials.getUser(player).isAfk()) {
					afk = true;
				}
			} else {
				//PLUGIN_AFK_FEATURE
				if (lastLocation.containsKey(player.getUniqueId())) { //AntiAFK
					if (lastLocation.get(player.getUniqueId()).getX() == player.getLocation().getX() && lastLocation.get(player.getUniqueId()).getY() == player.getLocation().getY() && lastLocation.get(player.getUniqueId()).getZ() == player.getLocation().getZ() || lastLocation.get(player.getUniqueId()).getYaw() == player.getLocation().getYaw()) {
						afk = true;
					}
				}
			}
			if (afk) {
				if (!finalconfig.getBoolean("afk_payout")) { // Payout is disabled
					if (finalconfig.getBoolean("display-messages-in-chat")) {
						sendMessage(player, finalconfig.getString("message_afk"));
					}
					if (finalconfig.getBoolean("display-messages-in-actionbar")) {
						sendActionbar(player, finalconfig.getString("message_afk_actionbar"));
					}
					return;
				} else { // Payout is enabled
					if (!finalconfig.isSet("afk_payout_percent")) {
						afkPercent = 100; // Payout % isn't set (older config), so assume 100% as before
					} else {
						afkPercent = finalconfig.getDouble("afk_payout_percent");
					}
				}
			}
		}
		
		//DEPOSIT
		double payout_amt = afk ? payout.payout_amount * (afkPercent / 100) : payout.payout_amount;
		
		// Take money from bank
		if (finalconfig.getString("bank-account").length() > 0) {
			EconomyResponse response = economy.bankWithdraw(finalconfig.getString("bank-account"), payout_amt);
			if(response.type == ResponseType.FAILURE) {
				System.out.println("§cFailed to take money from bank account: " + finalconfig.getString("bank-account") + " amount " + payout_amt);
				return;
			}
		}

		if (finalconfig.getBoolean("store-money-in-bank")) {
			if(ATM.getBankBalance(player) >= finalconfig.getDouble("atm_balance_limit", Double.MAX_VALUE)) {
				sendMessage(player, CC(finalconfig.getString("message_atm_limit_reached")));
				return;
			}
			ATM.depositBank(player, payout_amt);
		} else {
			double before = 0;
			if (economy.hasAccount(player)) {
				before = economy.getBalance(player);
			}
			
			economy.depositPlayer(player, payout_amt);
			log(player.getName() + ": Deposited: " + payout_amt + " Balance-before: " + before + " Balance-now: " + economy.getBalance(player));
		}
		
		if (!afk) {
			if (finalconfig.getBoolean("display-messages-in-chat") && payout_amt > 0d) {
				sendMessage(player, CC(finalconfig.getString("message")).replace("%money%", economy.format(payout_amt)));
			}
			if (finalconfig.getBoolean("display-messages-in-actionbar") && payout_amt > 0d) {
				sendActionbar(player, CC(finalconfig.getString("message_actionbar")).replace("%money%", economy.format(payout_amt)));
			}
			for (String cmd : payout.commands) {
				dispatchCommandSync(applyPlaceholders(player, cmd.replace("/", "").replaceAll("%player%", player.getName())));
			}
		} else {
			if (finalconfig.getBoolean("display-messages-in-chat") && finalconfig.isSet("message_afk_payout") && payout_amt > 0d) {
				sendMessage(player, CC(finalconfig.getString("message_afk_payout").replace("%money%", economy.format(payout_amt)).replace("%percent%", "" + afkPercent)));
			}
			if (finalconfig.getBoolean("display-messages-in-actionbar") && finalconfig.isSet("message_afk_actionbar_payout") && payout_amt > 0d) {
				sendActionbar(player, CC(finalconfig.getString("message_afk_actionbar_payout").replace("%money%", economy.format(payout_amt)).replace("%percent%", "" + afkPercent)));
			}
			for (String cmd : payout.commands_if_afk) {
				dispatchCommandSync(applyPlaceholders(player, cmd.replace("/", "").replaceAll("%player%", player.getName())));
			}
		}
		
		//ADD PAYED MONEY
		payoutPlayerData.setReceivedToday(payoutPlayerData.getReceivedToday() + payout_amt);

		
		lastLocation.put(player.getUniqueId(), player.getLocation());
		
		// clear payout limit reached message
		if(finalconfig.getBoolean("display-payout-limit-reached-message-once"))
			payoutLimitReached.remove(player.getUniqueId());
	}
	
	/**
	 * Dispatches a command as sync.
	 *
	 * @param cmd The command to dispatch sync.
	 */
	private void dispatchCommandSync(final String cmd) {
		final Server server = this.getServer();
		
		scheduler.runTask(SchedulerType.SYNC, task -> server.dispatchCommand(server.getConsoleSender(), cmd));
	}
	
	/**
	 * Logs debug information if enabled in the config.
	 *
	 * @param msg The debug message to log.
	 */
	@SuppressWarnings("deprecation")
	private void log(String msg) {
		if (!this.getConfig().getBoolean("debug-log")) {
			return;
		}
		Timestamp currentTimestamp = new Timestamp(Calendar.getInstance().getTime().getTime());
		
		File file = new File("plugins/TimeIsMoney/log.txt");
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
			FileReader pr = new FileReader(file);
			int number;
			StringBuffer text = new StringBuffer();
			while ((number = pr.read()) != -1) {
				
				text.append((char) number);
			}
			text.append(currentTimestamp.toGMTString()).append(":").append(msg).append("\n");
			PrintWriter pw = new PrintWriter(file);
			pw.print(text);
			
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Sends a chat message that supports color codes to the specified player.
	 *
	 * @param player The player to send the chat message to.
	 * @param msg The message the chat should give the player.
	 */
	private void sendMessage(Player player, String msg) {
		if (msg == null) return;
		if (msg.length() == 0) return;
		player.sendMessage(applyPlaceholders(player, CC(msg)));
	}

	/**
	 * Sends an actionbar message to the specified player.
	 *
	 * @param player The player to send the actionbar message to.
	 * @param message The message the actionbar should give to the player.
	 */
	private void sendActionbar(final Player player, final String message) {
		if (message.length() == 0) return;
		int times = finalconfig.getInt("display-messages-in-actionbar-time");
		if (times == 1) {
			sendSingleActionbarMessage(player, applyPlaceholders(player, CC(message)));
		} else if (times > 1) {
			sendSingleActionbarMessage(player, applyPlaceholders(player, CC(message)));
			
			times--;
			for (int i = 0; i < times; i++) {
				scheduler.runDelayed(SchedulerType.SYNC, task -> sendSingleActionbarMessage(player, applyPlaceholders(player, CC(message))), 20L * i);
			}
		}
	}
	
	private void sendSingleActionbarMessage(final Player player, final String message) {
		String[] minorMajorVersion = Bukkit.getBukkitVersion().split("-")[0].split("\\.");

        int majorVersion = Integer.parseInt(minorMajorVersion[1]);
        if(majorVersion == 8 || majorVersion == 9) {
        	// 1_8 -> 1_9
        	sendActionbarReflect(player, message);
        	return;
        } else if (majorVersion < 8) {
        	// no action bar support
        	return;
        }
        
		player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
	}
	
	private void sendActionbarReflect(final Player player, final String message) {
		String packageName = this.getServer().getClass().getPackage().getName();
		String version = packageName.substring(packageName.lastIndexOf('.') + 1);
		
		try {
			Object ichatbasecomponent = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer").getDeclaredMethod("a", String.class).invoke(null, "{\"text\": \"" + CC(message) + "\"}");
			Object packet = Class.forName("net.minecraft.server." + version + ".PacketPlayOutChat").getConstructor(Class.forName("net.minecraft.server." + version + ".IChatBaseComponent"), byte.class).newInstance(ichatbasecomponent, (byte) 2);
			
			Object craftPlayer = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer").cast(player);
			Object nmsPlayer = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer").getMethod("getHandle").invoke(craftPlayer);
			Object pCon = Class.forName("net.minecraft.server." + version + ".EntityPlayer").getField("playerConnection").get(nmsPlayer);

			Class.forName("net.minecraft.server." + version + ".PlayerConnection").getMethod("sendPacket", Class.forName("net.minecraft.server." + version + ".Packet")).invoke(pCon, packet);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public PluginData getPluginData() {
		return pluginData;
	}

	public List<Payout> getPayouts() {
		return payouts;
	}
}
