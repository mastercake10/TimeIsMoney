package de.Linus122.TimeIsMoney;

import com.google.common.primitives.Doubles;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.List;

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
	 * @param p The player to withdraw money from.
	 * @param amount The amount of money to withdraw.
	 */
	private static void withdrawBank(Player p, double amount) {
		String bankString = getBankString(p);
		if (!bankAccountsConfig.contains(bankString)) bankAccountsConfig.set(bankString, 0.0);
		bankAccountsConfig.set(bankString, getBankBalance(p) - amount);
		saveBanks();
	}
	
	/**
	 * Deposits the specified amount of money to the specified player's bank.
	 *
	 * @param p The player to deposit money to.
	 * @param amount The amount of money to deposit.
	 */
	public static void depositBank(Player p, double amount) {
		String bankString = getBankString(p);
		if (!bankAccountsConfig.contains(bankString)) bankAccountsConfig.set(bankString, 0.0);
		bankAccountsConfig.set(bankString, getBankBalance(p) + amount);
		saveBanks();
	}
	
	/**
	 * Checks if the player has the specified amount of money in their bank.
	 *
	 * @param p The player to check the balance of.
	 * @param amount The amount of money.
	 * @return True if the player has the specified amount of money, false otherwise.
	 */
	private static boolean bankHas(Player p, double amount) {
		String bankString = getBankString(p);
		if (!bankAccountsConfig.contains(bankString)) bankAccountsConfig.set(bankString, 0.0);
		return getBankBalance(p) >= amount;
		
	}
	
	/**
	 * Gets the balance of the specified player's bank (doesn't support groups).
	 *
	 * @param p The offline player to get the balance of.
	 * @return The offline player's balance in the bank.
	 */
	private static double getBankBalance(OfflinePlayer p) {
		String bankString = p.getName() + "_TimBANK";
		if (!bankAccountsConfig.contains(bankString)) bankAccountsConfig.set(bankString, 0.0);
		return bankAccountsConfig.getDouble(bankString);
	}
	
	/**
	 * Gets the balance of the specified player's bank.
	 *
	 * @param p The player to get the balance of.
	 * @return The player's balance in the bank.
	 */
	private static double getBankBalance(Player p) {
		String bankString = getBankString(p);
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
	 * @param p The player to convert data for.
	 */
	private static void convertOldBank(Player p) {
		String bankString = getBankString(p);
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
	 *
	 * @param p The player to get the bank string of.
	 * @return The bank string of the specified player.
	 */
	private static String getBankString(Player p) {
		if (!Main.finalconfig.getBoolean("group-atms")) {
			return p.getName() + "_TimBANK";
		} else {
			for (String key : Main.finalconfig.getConfigurationSection("atm_groups").getKeys(false)) {
				List<String> list = Main.finalconfig.getStringList("atm_groups." + key);
				if (list.contains(p.getWorld().getName())) {
					return p.getName() + "_TimBANK_" + key;
				}
			}
		}
		return p.getName() + "_TimBANK";
		/*if(!Main.finalconfig.getBoolean("group-atms")){
			return p.getName() + "_TimBANK";
		}else{
			for(String key : Main.finalconfig.getConfigurationSection("atm_groups").getKeys(false)){
				List<String> list = Main.finalconfig.getStringList("atm_groups." + key);
				if(list.contains(p.getWorld().getName())){
					return p.getName() + "_TimBANK_" + key;
				}
			}
		}
		return p.getName() + "_TimBANK";*/
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInteract(PlayerInteractEvent e) {
		if (e.getClickedBlock() != null) {
			if (e.getClickedBlock().getType() == Material.WALL_SIGN || e.getClickedBlock().getType() == Material.SIGN || e.getClickedBlock().getType() == Material.SIGN_POST) {
				Sign sign = (Sign) e.getClickedBlock().getState();
				if (sign.getLine(0).equalsIgnoreCase(CC("&cATM"))) {
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
	public void onMove(InventoryMoveItemEvent e) {
		if (e.getSource() == null) return;
		if (e.getSource().getTitle() == null) return;
		if (e.getSource().getTitle().equals(CC(Main.finalconfig.getString("atm_title")))) {
			e.setCancelled(true);
		}
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onClick(InventoryClickEvent e) {
		try {
			if (e == null) return;
			if (e.getInventory() == null) return;
			if (e.getInventory().getTitle() == null) return;
			if (e.getInventory().getTitle().equals(CC(Main.finalconfig.getString("atm_title")))) {
				e.setResult(Result.DENY);
				Player p = (Player) e.getWhoClicked();
				//e.setCancelled(true);
				if (e.getCurrentItem() != null) {
					// left side
					if (e.getSlot() < 4) {
						double amount = worths[3 - e.getSlot()];
						
						if (ATM.bankHas(p, amount)) {
							ATM.withdrawBank(p, amount);
							Main.economy.depositPlayer(p, amount);
							e.getWhoClicked().sendMessage(CC(Main.finalconfig.getString("atm_withdraw")) + " " + Main.economy.format(amount));
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
						ItemStack is = new ItemStack(Material.GOLD_NUGGET, 1);
						ItemMeta im = is.getItemMeta();
						im.setDisplayName(CC(Main.finalconfig.getString("atm_balance")) + " " + Main.economy.format(ATM.getBankBalance(p)));
						is.setItemMeta(im);
						e.getInventory().setItem(4, is);
					}
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
		is = new ItemStack(Material.CLAY_BRICK, 1);
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
		is = new ItemStack(Material.CLAY_BRICK, 1);
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
		
		player.openInventory(atm_gui);
	}
	
	@EventHandler
	public void onInventoryDrag(InventoryDragEvent e) {
		if (e == null) return;
		if (e.getInventory() == null) return;
		if (e.getInventory().getTitle() == null) return;
		if (e.getInventory().getTitle().equals(CC(Main.finalconfig.getString("atm_title")))) {
			e.setResult(Result.DENY);
		}
	}
	
	@EventHandler
	public void onSignChange(final SignChangeEvent e) {
		final Block b = e.getBlock();
		if (b.getType() == Material.WALL_SIGN || b.getType() == Material.SIGN || b.getType() == Material.SIGN_POST) {
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
				if (b.getType() == Material.WALL_SIGN || b.getType() == Material.SIGN || b.getType() == Material.SIGN_POST) {
					Sign sign = (Sign) e.getBlock().getState();
					if (sign.getLine(0).equalsIgnoreCase("[atm]")) {
						if (!e.getPlayer().hasPermission("tim.atm.place")) {
							e.getPlayer().sendMessage(CC("&cYou dont have permissions to build ATM's!"));
							sign.setLine(0, "");
						} else {
							sign.setLine(0, CC("&cATM"));
							sign.update();
							e.getPlayer().sendMessage(CC("&2ATM created! (You can also write something in the Lines 2-4)"));
						}
					}
				}
			}, 10L);
		}
	}
	
	@Override
	public boolean onCommand(CommandSender cs, Command arg1, String arg2, String[] args) {
		if (!(cs instanceof Player)) {
			cs.sendMessage("Only players can use atms.");
			return true;
		}
		if (args.length == 0) {
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
						}
						break;
				}
			} else {
				cs.sendMessage(CC("&c/atm <player> &a- opens atm for player"));
				cs.sendMessage(CC("&c/atm balance <player> &a- gets balance of player"));
				return true;
			}
		}
		return true;
	}
}
