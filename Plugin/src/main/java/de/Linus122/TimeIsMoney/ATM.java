package de.Linus122.TimeIsMoney;

import com.earth2me.essentials.api.Economy;
import com.google.common.primitives.Doubles;

import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.TreeMap;

import static de.Linus122.TimeIsMoney.tools.Utils.CC;

/**
 * ATM listener and command executor.
 *
 * @author Linus122
 * @since 1.9.6.1
 */
public class ATM implements Listener, CommandExecutor {
	/**
	 * The {@link Plugin}.
	 */
	private final Plugin plugin;
	/**
	 * The bank accounts {@link java.io.File} that stores all data.
	 */
	private static final File bankAccountsFile = new File("plugins/TimeIsMoney/data.dat");
	/**
	 * The bank accounts {@link org.bukkit.configuration.file.YamlConfiguration} to manage the {@link #bankAccountsFile}.
	 */
	private static YamlConfiguration bankAccountsConfig;
	/**
	 * The different amounts of money shown in the atm to withdraw and deposit (atm_worth_gradation).
	 */
	private double[] worths = new double[4];
	
	private List<Inventory> openATMs = new ArrayList<Inventory>();
	
	/**
	 * Creates a new atm instance with the {@link de.Linus122.TimeIsMoney.Main} class.
	 *
	 * @param plugin The {@link de.Linus122.TimeIsMoney.Main} class that implements {@link org.bukkit.plugin.java.JavaPlugin}.
	 */
	public ATM(Main plugin) {
		this.plugin = plugin;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		plugin.getCommand("atm").setExecutor(this);
		
		if (!bankAccountsFile.exists()) {
			try {
				bankAccountsFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		bankAccountsConfig = YamlConfiguration.loadConfiguration(bankAccountsFile);
		
		worths = Doubles.toArray(Main.finalconfig.getDoubleList("atm_worth_gradation"));
	}
	
	
	/**
	 * Withdraws the specified amount of money from the specified player's bank.
	 *
	 * @param player The player to withdraw money from.
	 * @param amount The amount of money to withdraw.
	 */
	private static void withdrawBank(Player player, double amount) {
		withdrawBank(player, player.getWorld(), amount);
	}
	
	private static void withdrawBank(OfflinePlayer offlinePlayer, World inWorld, double amount) {
		String bankString = getBankString(offlinePlayer, inWorld);
		if (!bankAccountsConfig.contains(bankString)) bankAccountsConfig.set(bankString, 0.0);
		bankAccountsConfig.set(bankString, getBankBalance(offlinePlayer, inWorld) - amount);
		saveBanks();
	}
	
	/**
	 * Deposits the specified amount of money to the specified player's bank.
	 *
	 * @param player The player to deposit money to.
	 * @param amount The amount of money to deposit.
	 */
	public static void depositBank(Player player, double amount) {
		depositBank(player, player.getWorld(), amount);
	}
	
	public static void depositBank(OfflinePlayer offlinePlayer, World inWorld, double amount) {
		String bankString = getBankString(offlinePlayer, inWorld);
		if (!bankAccountsConfig.contains(bankString)) bankAccountsConfig.set(bankString, 0.0);
		bankAccountsConfig.set(bankString, getBankBalance(offlinePlayer, inWorld) + amount);
		saveBanks();
	}
	
	/**
	 * Checks if the player has the specified amount of money in their bank.
	 *
	 * @param p The player to check the balance of.
	 * @param amount The amount of money.
	 * @return True if the player has the specified amount of money, false otherwise.
	 */
	private static boolean bankHas(Player player, double amount) {
		return bankHas(player, player.getWorld(), amount);
	}
	
	private static boolean bankHas(OfflinePlayer offlinePlayer, World inWorld, double amount) {
		String bankString = getBankString(offlinePlayer, inWorld);
		if (!bankAccountsConfig.contains(bankString)) bankAccountsConfig.set(bankString, 0.0);
		return getBankBalance(offlinePlayer) >= amount;
		
	}
	
	/**
	 * Gets the balance of the specified player's bank (doesn't support groups).
	 *
	 * @param offlinePlayer The offline player to get the balance of.
	 * @return The offline player's balance in the bank.
	 */
	public static double getBankBalance(OfflinePlayer offlinePlayer) {
		String bankString = offlinePlayer.getName() + "_TimBANK";
		if (!bankAccountsConfig.contains(bankString)) bankAccountsConfig.set(bankString, 0.0);
		return bankAccountsConfig.getDouble(bankString);
	}
	
	/**
	 * Gets the balance of the specified player's bank.
	 *
	 * @param player The player to get the balance of.
	 * @return The player's balance in the bank.
	 */
	public static double getBankBalance(Player player) {
		return getBankBalance(player, player.getWorld());
	}
	
	public static double getBankBalance(OfflinePlayer offlinePlayer, World inWorld) {
		String bankString = getBankString(offlinePlayer, inWorld);
		if (!bankAccountsConfig.contains(bankString)) bankAccountsConfig.set(bankString, 0.0);
		return bankAccountsConfig.getDouble(bankString);
	}
	
	/**
	 * Saves the banks.
	 */
	private static void saveBanks() {
		try {
			bankAccountsConfig.save(bankAccountsFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Converts the old TimeIsMoney bank data to the new format.
	 * 
	 * @deprecated ancient method, will be deleted soon
	 * @param p The player to convert data for.
	 */
	@Deprecated
	private static void convertOldBank(Player p) {
		String bankString = getBankString(p, p.getWorld());
		if (Main.economy.hasAccount(bankString)) {
			if (Main.economy.getBalance(bankString) > 0) {
				p.sendMessage(CC("&aSuccessfully converted your old TIM-Bank to new version!"));
				depositBank(p, Main.economy.getBalance(bankString));
				Main.economy.withdrawPlayer(bankString, Main.economy.getBalance(bankString));
			}
		}
	}
	
	/**
	 * Gets the bank string for the specified player.
	 * Converts old bank accounts (those saved using the user name) to new bank accounts using UUID's.
	 *
	 * @param player The player to get the bank string of.
	 * @param inWorld The World. Only needs to be specified when working with grouped ATM's (world-wise)
	 * @return The bank string of the specified player.
	 */
	private static String getBankString(OfflinePlayer player, World inWorld) {
		String oldBank = getBankStringByPrefix(player.getName(), inWorld);
		if(bankAccountsConfig.contains(oldBank)) {
			double oldMoneyAmount = bankAccountsConfig.getDouble(oldBank);
			bankAccountsConfig.set(getBankStringByPrefix(player.getUniqueId().toString(), inWorld), oldMoneyAmount);
			bankAccountsConfig.set(oldBank, null);
		}
		
		return getBankStringByPrefix(player.getUniqueId().toString(), inWorld);
	}
	
	/**
	 * Returns the bank string of a player that is used internally for storing the money on.
	 * 
	 * @param prefix The prefix to work with
	 * @param inWorld The World. Only needs to be specified when working with grouped ATM's (world-wise)
	 * @return The bank string of the specified player.
	 */
	private static String getBankStringByPrefix(String prefix, World inWorld) {
		if (!Main.finalconfig.getBoolean("group-atms")) {
			return prefix + "_TimBANK";
		} else {
			for (String key : Main.finalconfig.getConfigurationSection("atm_groups").getKeys(false)) {
				List<String> list = Main.finalconfig.getStringList("atm_groups." + key);
				if (list.contains(inWorld.getName())) {
					return inWorld.getName() + "_TimBANK_" + key;
				}
			}
		}
		return prefix + "_TimBANK";
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInteract(PlayerInteractEvent e) {
		if (e.getClickedBlock() != null) {
			if (e.getClickedBlock().getState() instanceof Sign) {
				Sign sign = (Sign) e.getClickedBlock().getState();
				if (sign.getLine(0).equalsIgnoreCase(CC(Main.finalconfig.getString("atm_sign_label")))) {
					if (!e.getPlayer().hasPermission("tim.atm.use")) {
						e.getPlayer().sendMessage(CC(Main.finalconfig.getString("message_atm_noperms")));
					} else {
						this.openGUI(e.getPlayer());
					}
				}
			}
		}
	}
	
	@EventHandler
	public void onClose(InventoryCloseEvent e) {
		if (e.getInventory() != null)
			openATMs.remove(e.getInventory());
	}
	
	@EventHandler
	public void onMove(InventoryMoveItemEvent e) {
		if (e.getSource() == null || e.getSource().getViewers().size() == 0 || e.getSource().getViewers().get(0).getOpenInventory() == null) return;
		if (openATMs.contains(e.getSource().getViewers().get(0).getOpenInventory().getTopInventory())) {
			e.setCancelled(true);
		}
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onClick(InventoryClickEvent e) {
		try {
			if (e == null || e.getInventory() == null) return;
			if (openATMs.contains(e.getView().getTopInventory())) {
				e.setResult(Result.DENY);
				Player p = (Player) e.getWhoClicked();
				//e.setCancelled(true);
				if (e.getCurrentItem() != null) {
					// left side
					if (e.getSlot() < 4) {
						double amount = worths[3 - e.getSlot()];
						
						if (ATM.bankHas(p, amount)) {
							EconomyResponse response = Main.economy.depositPlayer(p, amount);
							if (response.type == ResponseType.SUCCESS) {
								ATM.withdrawBank(p, amount);
								e.getWhoClicked().sendMessage(CC(Main.finalconfig.getString("atm_withdraw")) + " " + Main.economy.format(amount));
							}
						} else {
							e.getWhoClicked().sendMessage(CC(Main.finalconfig.getString("message_atm_nomoneyinbank")));
						}
					} else {
						// right side
						if (e.getSlot() > 4) {
							double amount = worths[3 - (3 - (e.getSlot() - 5))];
							
							if (Main.economy.has((Player) e.getWhoClicked(), amount)) {
								ATM.depositBank(p, amount);
								Main.economy.withdrawPlayer((Player) e.getWhoClicked(), amount);
								e.getWhoClicked().sendMessage(CC(Main.finalconfig.getString("atm_deposit")) + " " + Main.economy.format(amount));
							} else {
								e.getWhoClicked().sendMessage(CC(Main.finalconfig.getString("message_atm_nomoney")));
							}
						}
					}
					// updating atm balance
					ItemStack is = new ItemStack(Material.GOLD_NUGGET, 1);
					ItemMeta im = is.getItemMeta();
					im.setDisplayName(CC(Main.finalconfig.getString("atm_balance")) + " " + Main.economy.format(ATM.getBankBalance(p)));
					is.setItemMeta(im);
					e.getInventory().setItem(4, is);
				}
			}
		} catch (Exception ignored) {
		}
	}
	
	/**
	 * Opens the atm gui for the specified player.
	 *
	 * @param player The player to open the atm gui for.
	 */
	private void openGUI(Player player) {
		if(worths.length == 0) {
			player.sendMessage("§cError in config.yml: atm_worth_gradation is empty.");
			return;
		}
		convertOldBank(player);
		Inventory atm_gui = Bukkit.createInventory(null, 9, CC(Main.finalconfig.getString("atm_title")));
		
		// Balance
		ItemStack is = new ItemStack(Material.GOLD_NUGGET, 1);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName(CC(Main.finalconfig.getString("atm_balance")) + " " + Main.economy.format(ATM.getBankBalance(player)));
		is.setItemMeta(im);
		atm_gui.setItem(4, is);
		
		// Withdraw
		is = new ItemStack(Material.getMaterial("CLAY_BRICK"), 1);
		im = is.getItemMeta();
		im.setDisplayName(CC(Main.finalconfig.getString("atm_withdraw") + " &a") + Main.economy.format(worths[0]));
		is.setItemMeta(im);
		atm_gui.setItem(3, is);
		
		is = new ItemStack(Material.IRON_INGOT, 1);
		im = is.getItemMeta();
		im.setDisplayName(CC(Main.finalconfig.getString("atm_withdraw") + " &a") + Main.economy.format(worths[1]));
		is.setItemMeta(im);
		atm_gui.setItem(2, is);
		
		is = new ItemStack(Material.GOLD_INGOT, 1);
		im = is.getItemMeta();
		im.setDisplayName(CC(Main.finalconfig.getString("atm_withdraw") + " &a") + Main.economy.format(worths[2]));
		is.setItemMeta(im);
		atm_gui.setItem(1, is);
		
		is = new ItemStack(Material.DIAMOND, 1);
		im = is.getItemMeta();
		im.setDisplayName(CC(Main.finalconfig.getString("atm_withdraw") + " &a") + Main.economy.format(worths[3]));
		is.setItemMeta(im);
		atm_gui.setItem(0, is);
		
		// Deposit
		is = new ItemStack(Material.getMaterial("CLAY_BRICK"), 1);
		im = is.getItemMeta();
		im.setDisplayName(CC(Main.finalconfig.getString("atm_deposit") + " &4") + Main.economy.format(worths[0]));
		is.setItemMeta(im);
		atm_gui.setItem(5, is);
		
		//
		is = new ItemStack(Material.IRON_INGOT, 1);
		im = is.getItemMeta();
		im.setDisplayName(CC(Main.finalconfig.getString("atm_deposit") + " &4") + Main.economy.format(worths[1]));
		is.setItemMeta(im);
		atm_gui.setItem(6, is);
		
		//
		is = new ItemStack(Material.GOLD_INGOT, 1);
		im = is.getItemMeta();
		im.setDisplayName(CC(Main.finalconfig.getString("atm_deposit") + " &4") + Main.economy.format(worths[2]));
		is.setItemMeta(im);
		atm_gui.setItem(7, is);
		
		//
		is = new ItemStack(Material.DIAMOND, 1);
		im = is.getItemMeta();
		im.setDisplayName(CC(Main.finalconfig.getString("atm_deposit") + " &4") + Main.economy.format(worths[3]));
		is.setItemMeta(im);
		atm_gui.setItem(8, is);
		
		
		openATMs.add(atm_gui);
		player.openInventory(atm_gui);
	}
	
	@EventHandler
	public void onInventoryDrag(InventoryDragEvent e) {
		if (e == null || e.getInventory() == null) return;

		if (openATMs.contains(e.getView().getTopInventory())) {
			e.setResult(Result.DENY);
		}
	}
	
	@EventHandler
	public void onSignChange(final SignChangeEvent e) {
		final Block b = e.getBlock();
		if (b.getState() instanceof Sign) {
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
				if (b.getState() instanceof Sign) {
					Sign sign = (Sign) e.getBlock().getState();
					if (sign.getLine(0).equalsIgnoreCase("[ATM]") 
							|| sign.getLine(0).equalsIgnoreCase(Main.finalconfig.getString("atm_sign_label")) 
							|| sign.getLine(0).equalsIgnoreCase(CC(Main.finalconfig.getString("atm_sign_label")))) {
						if (!e.getPlayer().hasPermission("tim.atm.place")) {
							e.getPlayer().sendMessage(CC(Main.finalconfig.getString("message_atm_nopermbuild")));
							sign.setLine(0, "");
						} else {
							sign.setLine(0, CC(Main.finalconfig.getString("atm_sign_label")));
							sign.update();
							e.getPlayer().sendMessage(CC(Main.finalconfig.getString("message_atm_created")));
						}
					}
				}
			}, 10L);
		}
	}
	
	@Override
	public boolean onCommand(CommandSender cs, Command arg1, String arg2, String[] args) {
		if (args.length == 0) {
			if (!(cs instanceof Player)) {
				cs.sendMessage("Only players can use atms.");
				return true;
			}
			if (cs.hasPermission("tim.use")) {
				openGUI((Player) cs);
				return true;
			}
		}
		if (cs.hasPermission("tim.admin")) {
			if (args.length > 0) {
				switch (args[0]) {
					case "balance":
						if (args.length > 1) {
							cs.sendMessage(CC("&2ATM-Balance of&c " + args[1] + "&2: &c") + getBankBalance(Bukkit.getOfflinePlayer(args[1])));
						} else {
							cs.sendMessage("/atm balance <player>");
						}
						break;
					case "balancetop":
						cs.sendMessage("§cTop Bank Accounts:");
						Map<String, Double> topBal = new TreeMap<String, Double>();

						for (String keyBankString : bankAccountsConfig.getKeys(false)) {
							double amount = bankAccountsConfig.getDouble(keyBankString);
							topBal.put(keyBankString, amount);
						}
						topBal.entrySet().stream().
						    sorted(Entry.comparingByValue(Comparator.reverseOrder())).limit(10).forEachOrdered(entry -> cs.sendMessage("§a" + entry.getKey() + "§2: " + Main.economy.format(entry.getValue())));
						break;
					case "take":
						if(args.length > 2) {
							OfflinePlayer playerToTake = Bukkit.getOfflinePlayer(args[1]);
							String inWorld = args.length > 3 ? args[3] : "world";
							
							if(playerToTake == null) {
								cs.sendMessage("§cThis player does not exists");
								return true;
							}
							try {
								double amount = Double.parseDouble(args[2]);
								double bal = ATM.getBankBalance(playerToTake);
								if(amount > bal) {
									cs.sendMessage("§cAmount to high! Player only has " + Economy.format(bal));
									return true;
								}
								ATM.withdrawBank(playerToTake, Bukkit.getWorld(inWorld), amount);
								cs.sendMessage("§aWithdrew §2" + Economy.format(amount) + ".");
							}catch(NumberFormatException e) {
								cs.sendMessage("§cPlease enter a valid decimal");
							}
						}else {
							cs.sendMessage("§c/tim take <player> <amount> [world]");
						}
						break;
					case "give":
						if(args.length > 2) {
							
							OfflinePlayer playerToGive = Bukkit.getOfflinePlayer(args[1]);
							String inWorld = args.length > 3 ? args[3] : "world";
							
							if(playerToGive == null) {
								cs.sendMessage("§cThis player does not exists");
								return true;
							}
							try {
								double amount = Double.parseDouble(args[2]);
								double bal = ATM.getBankBalance(playerToGive);
								ATM.depositBank(playerToGive, Bukkit.getWorld(inWorld), amount);
								cs.sendMessage("§aDeposited §2" + Economy.format(amount) + ".");
							}catch(NumberFormatException e) {
								cs.sendMessage("§cPlease enter a valid decimal");
							}
						}else {
							cs.sendMessage("§c/tim give <player> <amount> [world]");
						}
						break;
					default:
						@SuppressWarnings("deprecation")
						OfflinePlayer op = Bukkit.getOfflinePlayer(args[0]);
						if (op == null) {
							cs.sendMessage("Player is offline");
							return true;
						}
						if (op.isOnline()) {
							openGUI(op.getPlayer());
							cs.sendMessage("opened!");
							return true;
						}
						cs.sendMessage(CC("&c/atm <player> &a- opens atm for player"));
						cs.sendMessage(CC("&c/atm balance <player> &a- gets balance of player"));
						cs.sendMessage(CC("&c/atm balancetop - Shows the top 10 player atm balances"));
						cs.sendMessage(CC("&c/atm give <player> [world] &a- Deposits money into a players atm"));
						cs.sendMessage(CC("&c/atm take <player> [world] &a- Withdraws money from a players atm"));
						break;
				}
			}
		}
		return true;
	}
}
