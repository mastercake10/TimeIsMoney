package modules.atm;

import com.google.common.primitives.Doubles;
import de.Linus122.TimeIsMoney.Main;
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

import static de.Linus122.TimeIsMoney.Utils.CC;

public class ATM implements Listener, CommandExecutor {
    private static final File fileBankAccounts = new File("plugins/TimeIsMoney/data.dat");
    private static YamlConfiguration cfg;
    private final Plugin pl;
    private double[] worths = new double[4];

    public ATM(Main pl) {
        this.pl = pl;
        pl.getServer().getPluginManager().registerEvents(this, pl);
        pl.getCommand("atm").setExecutor(this);
        if (!fileBankAccounts.exists()) {
            try {
                fileBankAccounts.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        cfg = YamlConfiguration.loadConfiguration(fileBankAccounts);

        worths = Doubles.toArray(Main.finalconfig.getDoubleList("atm_worth_gradation"));
    }

    private static void withdrawBank(Player p, double amount) {
        String bankString = getBankString(p);
        if (!cfg.contains(bankString)) cfg.set(bankString, 0.0);
        cfg.set(bankString, getBankBalance(p) - amount);
        saveBanks();
    }

    public static void depositBank(Player p, double amount) {
        String bankString = getBankString(p);
        if (!cfg.contains(bankString)) cfg.set(bankString, 0.0);
        cfg.set(bankString, getBankBalance(p) + amount);
        saveBanks();
    }

    private static boolean bankHas(Player p, double amount) {
        String bankString = getBankString(p);
        if (!cfg.contains(bankString)) cfg.set(bankString, 0.0);
        return getBankBalance(p) >= amount;

    }

    //Doesn't support groups
    private static double getBankBalance(OfflinePlayer p) {
        String bankString = p.getName() + "_TimBANK";
        if (!cfg.contains(bankString)) cfg.set(bankString, 0.0);
        return cfg.getDouble(bankString);
    }

    private static double getBankBalance(Player p) {
        String bankString = getBankString(p);
        if (!cfg.contains(bankString)) cfg.set(bankString, 0.0);
        return cfg.getDouble(bankString);
    }

    private static void saveBanks() {
        try {
            cfg.save(fileBankAccounts);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Converts old tim bank
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

    public static void changeMoney(Player p, double amount) {

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
                    } else
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
        } catch (Exception ignored) {

        }
    }

    private void openGUI(Player player) {
        convertOldBank(player);
        Inventory atm_gui = Bukkit.createInventory(null, 9, CC(Main.finalconfig.getString("atm_title")));

        //
        ItemStack is = new ItemStack(Material.GOLD_NUGGET, 1);
        ItemMeta im = is.getItemMeta();
        im.setDisplayName(CC(Main.finalconfig.getString("atm_balance")) + " " + Main.economy.format(ATM.getBankBalance(player)));
        is.setItemMeta(im);
        atm_gui.setItem(4, is);


        //
        is = new ItemStack(Material.CLAY_BRICK, 1);
        im = is.getItemMeta();
        im.setDisplayName(CC(Main.finalconfig.getString("atm_withdraw") + " &a") + Main.economy.format(worths[0]));
        is.setItemMeta(im);
        atm_gui.setItem(3, is);

        //
        is = new ItemStack(Material.IRON_INGOT, 1);
        im = is.getItemMeta();
        im.setDisplayName(CC(Main.finalconfig.getString("atm_withdraw") + " &a") + Main.economy.format(worths[1]));
        is.setItemMeta(im);
        atm_gui.setItem(2, is);

        //
        is = new ItemStack(Material.GOLD_INGOT, 1);
        im = is.getItemMeta();
        im.setDisplayName(CC(Main.finalconfig.getString("atm_withdraw") + " &a") + Main.economy.format(worths[2]));
        is.setItemMeta(im);
        atm_gui.setItem(1, is);

        //
        is = new ItemStack(Material.DIAMOND, 1);
        im = is.getItemMeta();
        im.setDisplayName(CC(Main.finalconfig.getString("atm_withdraw") + " &a") + Main.economy.format(worths[3]));
        is.setItemMeta(im);
        atm_gui.setItem(0, is);

        //DEPOSITE
        //
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
    public void onItem2(InventoryDragEvent e) {
        if (e == null) return;
        if (e.getInventory() == null) return;
        if (e.getInventory().getTitle() == null) return;
        if (e.getInventory().getTitle().equals(CC(Main.finalconfig.getString("atm_title")))) {
            e.setResult(Result.DENY);
        }
    }

    @EventHandler
    public void onSign(final SignChangeEvent e) {
        final Block b = e.getBlock();
        if (b.getType() == Material.WALL_SIGN || b.getType() == Material.SIGN || b.getType() == Material.SIGN_POST) {
            pl.getServer().getScheduler().scheduleSyncDelayedTask(pl, () -> {
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
