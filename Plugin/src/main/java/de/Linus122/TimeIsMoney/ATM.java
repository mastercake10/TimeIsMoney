package de.Linus122.TimeIsMoney;

import static de.Linus122.TimeIsMoney.tools.Utils.CC;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;

import fr.euphyllia.energie.model.SchedulerType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import com.earth2me.essentials.api.Economy;
import com.google.common.primitives.Doubles;

import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import xyz.spaceio.spacegui.SpaceGUI;
import xyz.spaceio.spacegui.helpers.StackBuilder;
import xyz.spaceio.spaceitem.DecorationMaterial;
import xyz.spaceio.spaceitem.SpaceItem;

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
	
	private File guiFile = new File("plugins/TimeIsMoney/atm_gui.yml");
	
	private List<Inventory> openATMs = new ArrayList<Inventory>();
	
	private SpaceGUI gui;
	
	private Material[] mats = new Material[] {Material.getMaterial("CLAY_BRICK") == null ? Material.getMaterial("BRICK") : Material.getMaterial("CLAY_BRICK"), Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND};
	
	/**
	 * Creates a new atm instance with the {@link de.Linus122.TimeIsMoney.Main} class.
	 *
	 * @param plugin The {@link de.Linus122.TimeIsMoney.Main} class that implements {@link org.bukkit.plugin.java.JavaPlugin}.
	 */
	public ATM(Main plugin) {
		this.plugin = plugin;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		
		worths = Doubles.toArray(Main.finalconfig.getDoubleList("atm_worth_gradation"));
		gui = new SpaceGUI().title("§cATM").size(9*3);
		gui.fillBackground(new SpaceItem().setStack(DecorationMaterial.GRAY_STAINED_GLASS_PANE.get()));
		
		FileConfiguration fileConfig = new YamlConfiguration();
	
		if(guiFile.exists()) {
			try {
				fileConfig.load(guiFile);
				gui = (SpaceGUI) fileConfig.get("atm");
			} catch (IOException | InvalidConfigurationException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} else {
			gui.getOrCreateItem(new SpaceItem().setStack(new StackBuilder(Material.GOLD_NUGGET).setDisplayname(CC("&cBalance &a%s"))).setLabel("balance"), 4 + 9);
			for(int i = 0; i < 4; i++) {
				gui.getOrCreateItem(new SpaceItem().setStack(new StackBuilder(mats[i]).setDisplayname(CC("&cWithdraw &a%s"))).setLabel("witdraw-" + i), 3 - i + 9);
			}
			for(int i = 0; i < 4; i++) {
				gui.getOrCreateItem(new SpaceItem().setStack(new StackBuilder(mats[i]).setDisplayname(CC("&cDeposit &a%s"))).setLabel("deposit-" + i), 5 + i + 9);
			}
		}
		
		// balance item
		

		SpaceItem balanceItem = gui.getItemWithLabel("balance");
		if(balanceItem != null) {
			balanceItem.setFormat((p) -> 
				Main.economy.format(ATM.getBankBalance(p))
			);	
		}
		
		for(int i = 0; i < 4; i++) {
			final int index = i;
			SpaceItem item = gui.getItemWithLabel("witdraw-" + i);
			if(item == null) continue;
			
			item.addAction((p, action) -> {
				ATM.interactWithdraw(p, worths[index]);
				action.getView().update(balanceItem);
			})
			.setFormat((p) -> Main.economy.format(worths[index]));	
		}
		for(int i = 0; i < 4; i++) {
			final int index = i;
			SpaceItem item = gui.getItemWithLabel("deposit-" + i);
			if(item == null) continue;
			
			item.addAction((p, action) -> {
				ATM.interactDeposit(p, worths[index]);
				action.getView().update(balanceItem);
			})
			.setFormat((p) -> Main.economy.format(worths[index]));	
		}
	
		
		if(!fileConfig.contains("atm")) {
			try {
				fileConfig.set("atm", gui);
				fileConfig.save(guiFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	
		
		plugin.getCommand("atm").setExecutor(this);
		
		
		
		if (!bankAccountsFile.exists()) {
			try {
				bankAccountsFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		bankAccountsConfig = YamlConfiguration.loadConfiguration(bankAccountsFile);
		

	}
	private static void interactWithdraw(Player p, double amount) {
		if (ATM.bankHas(p, amount)) {
			EconomyResponse response = Main.economy.depositPlayer(p, amount);
			if (response.type == ResponseType.SUCCESS) {
				ATM.withdrawBank(p, amount);
				p.sendMessage(String.format(CC(Main.finalconfig.getString("message_atm_withdrew")), Main.economy.format(amount)));
			}
		} else {
			p.sendMessage(CC(Main.finalconfig.getString("message_atm_nomoneyinbank")));
		}
	}
	
	private static void interactDeposit(Player p, double amount) {
		if (ATM.getBankBalance(p) >= Main.finalconfig.getDouble("atm_balance_limit", Double.MAX_VALUE)) {
			p.sendMessage(CC(Main.finalconfig.getString("message_atm_limit_reached")));
			return;
		}
		if (Main.economy.has(p, amount)) {
			ATM.depositBank(p, amount);
			Main.economy.withdrawPlayer(p, amount);
			p.sendMessage(String.format(CC(Main.finalconfig.getString("message_atm_deposited")), Main.economy.format(amount)));
		} else {
			p.sendMessage(CC(Main.finalconfig.getString("message_atm_nomoney")));
		}
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
	 * @param player The player to check the balance of.
	 * @param amount The amount of money.
	 * @return True if the player has the specified amount of money, false otherwise.
	 */
	private static boolean bankHas(Player player, double amount) {
		return bankHas(player, player.getWorld(), amount);
	}
	
	private static boolean bankHas(OfflinePlayer offlinePlayer, World inWorld, double amount) {
		String bankString = getBankString(offlinePlayer, inWorld);
		if (!bankAccountsConfig.contains(bankString)) bankAccountsConfig.set(bankString, 0.0);
		return getBankBalance(offlinePlayer, inWorld) >= amount;
		
	}
	
	/**
	 * Gets the balance of the specified player's bank (doesn't support groups).
	 *
	 * @param offlinePlayer The offline player to get the balance of.
	 * @param inWorld The World. Only needs to be specified when working with grouped ATM's (world-wise)
	 * @return The offline player's balance in the bank.
	 */
	public static double getBankBalance(OfflinePlayer offlinePlayer, World inWorld) {
		String bankString = getBankString(offlinePlayer, inWorld);
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
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onInteract(PlayerInteractEvent e) {
		if (e.getClickedBlock() != null) {
			if (e.getClickedBlock().getState() instanceof Sign) {
				Sign sign = (Sign) e.getClickedBlock().getState();
				if (sign.getLine(0).equalsIgnoreCase(CC(Main.finalconfig.getString("atm_sign_label")))) {
					if(e.getAction() == Action.RIGHT_CLICK_BLOCK) {
						e.setCancelled(true);
						if (!e.getPlayer().hasPermission("tim.atm.use")) {
							e.getPlayer().sendMessage(CC(Main.finalconfig.getString("message_atm_noperms")));
						} else {
							this.openGUI(e.getPlayer());
						}
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
		gui.open(player);
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
			Main.scheduler.runDelayed(SchedulerType.SYNC, e.getBlock().getLocation(), task -> {
				if (b.getState() instanceof Sign) {
					Sign sign = (Sign) e.getBlock().getState();
					if (sign.getLine(0).equalsIgnoreCase("[ATM]") 
							|| sign.getLine(0).equalsIgnoreCase(Main.finalconfig.getString("atm_sign_label")) 
							|| sign.getLine(0).equalsIgnoreCase(CC(Main.finalconfig.getString("atm_sign_label")))) {
						if (!e.getPlayer().hasPermission("tim.atm.place")) {
							e.getPlayer().sendMessage(CC(Main.finalconfig.getString("message_atm_nopermbuild")));
							sign.setLine(0, "");
							e.setCancelled(true);
							b.setType(Material.AIR);
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
							cs.sendMessage(CC("&2ATM-Balance of&c " + args[1] + "&2: &c") + Economy.format(getBankBalance(Bukkit.getOfflinePlayer(args[1]), null)));
						} else {
							cs.sendMessage("/atm balance <player>");
						}
						break;
					case "balancetop":
						cs.sendMessage("§cTop Bank Accounts:");
						Map<String, Double> topBal = new TreeMap<String, Double>();

						for (String keyBankString : bankAccountsConfig.getKeys(false)) {
							double amount = bankAccountsConfig.getDouble(keyBankString);
							String formattedDisplayString = keyBankString.split("_")[0];
							if(formattedDisplayString.length() > 16) {
								// uuid
								formattedDisplayString = Bukkit.getOfflinePlayer(UUID.fromString(formattedDisplayString)).getName();
							}
							topBal.put(formattedDisplayString, amount);
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
								double bal = ATM.getBankBalance(playerToTake, null);
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
								double bal = ATM.getBankBalance(playerToGive, null);
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
						cs.sendMessage(CC("&c/atm give <player> <amount> [world] &a- Deposits money into a players atm"));
						cs.sendMessage(CC("&c/atm take <player> <amount> [world] &a- Withdraws money from a players atm"));
						break;
				}
			}
		}
		return true;
	}
}
