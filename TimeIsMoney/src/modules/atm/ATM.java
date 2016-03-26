package modules.atm;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import de.Linus122.TimeIsMoney.Main;
import net.milkbowl.vault.economy.EconomyResponse;

public class ATM implements Listener {
	Plugin pl;
	
	public ATM(Plugin pl){
		this.pl = pl;
		pl.getServer().getPluginManager().registerEvents(this, pl);
		
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInteract(PlayerInteractEvent e){
		if(e.getClickedBlock() != null){
			if(e.getClickedBlock().getType() == Material.WALL_SIGN || e.getClickedBlock().getType() == Material.SIGN || e.getClickedBlock().getType() == Material.SIGN_POST){
				Sign sign = (Sign) e.getClickedBlock().getState();	
				if(sign.getLine(0).equalsIgnoreCase("§cATM")){
					if(!e.getPlayer().hasPermission("tim.atm.use")){
						e.getPlayer().sendMessage(Main.finalconfig.getString("message_atm_noperms").replace('&', '§'));
					}else{
						this.openGUI(e.getPlayer());
					}
				}
			}
		}
	}
	@EventHandler
	public void onMove(InventoryMoveItemEvent e){
		if(e.getSource() == null) return;
		if(e.getSource().getTitle() == null) return;
		if(e.getSource().getTitle().equals(Main.finalconfig.getString("atm_title").replace('&', '§'))){
			e.setCancelled(true);
		}
	}
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onClick(InventoryClickEvent e){
		try{
			if(e == null) return;
			if(e.getInventory() == null) return;
			if(e.getInventory().getTitle() == null) return;
			if(e.getInventory().getTitle().equals(Main.finalconfig.getString("atm_title").replace('&', '§'))){
				e.setCancelled(true);
				String bank = e.getWhoClicked().getName() + "_TimBANK";
				if(e.getCurrentItem() != null){
					if(e.getCurrentItem().getItemMeta().getDisplayName().split(" ")[0].equals(Main.finalconfig.getString("atm_withdraw").replace('&', '§'))){
	
						double amount = Double.parseDouble(e.getCurrentItem().getItemMeta().getLore().get(0));
						
						if(!Main.economy.hasAccount(bank)){
							Main.economy.createPlayerAccount(bank);
						}
						if(Main.economy.has(bank, amount)){
							Main.economy.withdrawPlayer(bank, amount);
							Main.economy.depositPlayer((Player) e.getWhoClicked(), amount);
							e.getWhoClicked().sendMessage(Main.finalconfig.getString("atm_withdraw").replace('&', '§') + " " + Main.economy.format(amount));
						}else{
							e.getWhoClicked().sendMessage(Main.finalconfig.getString("message_atm_nomoneyinbank").replace('&', '§'));
						}
					}else
					if(e.getCurrentItem().getItemMeta().getDisplayName().split(" ")[0].equals(Main.finalconfig.getString("atm_deposit").replace('&', '§'))){
						
						double amount = Double.parseDouble(e.getCurrentItem().getItemMeta().getLore().get(0));
						
						if(Main.economy.has((Player) e.getWhoClicked(), amount)){
							if(!Main.economy.hasAccount(bank)){
								Main.economy.createPlayerAccount(bank);
							}
							Main.economy.depositPlayer(bank, amount);
							Main.economy.withdrawPlayer((Player) e.getWhoClicked(), amount);
							e.getWhoClicked().sendMessage(Main.finalconfig.getString("atm_deposit").replace('&', '§') + " " + Main.economy.format(amount));
						}else{
							e.getWhoClicked().sendMessage(Main.finalconfig.getString("message_atm_nomoney").replace('&', '§'));
						}
					}
					ItemStack is = new ItemStack(Material.GOLD_NUGGET, 1);
					ItemMeta im = is.getItemMeta();
					im.setDisplayName("§cBank balance: " + getBankbalance(e.getWhoClicked().getName() + "_TimBANK"));
					is.setItemMeta(im);
					e.getInventory().setItem(4, is);
				}
			}
		}catch(Exception e2){
			
		}
	}
	public double getBankbalance(String name){
			
			return Main.economy.getBalance(name);
	}
	private void openGUI(Player player) {
		Inventory atm_gui = Bukkit.createInventory(null, 9, "§cATM");
		
		//
		ItemStack is = new ItemStack(Material.GOLD_NUGGET, 1);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName(Main.finalconfig.getString("atm_balance").replace('&', '§') + " " + Main.economy.format(getBankbalance(player.getName() + "_TimBANK")));
		is.setItemMeta(im);
		atm_gui.setItem(4, is);
		

		//
		is = new ItemStack(Material.CLAY_BRICK, 1);
		im = is.getItemMeta();
		im.setDisplayName(Main.finalconfig.getString("atm_withdraw").replace('&', '§') + " §a" + Main.economy.format(10));
		List<String> lore = new ArrayList<String>();
		lore.add("10");
		im.setLore(lore);
		is.setItemMeta(im);
		atm_gui.setItem(3, is);
		
		//
		is = new ItemStack(Material.IRON_INGOT, 1);
		im = is.getItemMeta();
		im.setDisplayName(Main.finalconfig.getString("atm_withdraw").replace('&', '§') +  " §a" + Main.economy.format(100));
		lore.clear();
		lore = new ArrayList<String>();
		lore.add("100");
		im.setLore(lore);
		is.setItemMeta(im);
		atm_gui.setItem(2, is);
		
		//
		is = new ItemStack(Material.GOLD_INGOT, 1);
		im = is.getItemMeta();
		im.setDisplayName(Main.finalconfig.getString("atm_withdraw").replace('&', '§') + " §a" + Main.economy.format(1000));
		lore.clear();
		lore = new ArrayList<String>();
		lore.add("1000");
		im.setLore(lore);
		is.setItemMeta(im);
		atm_gui.setItem(1, is);
		
		//
		is = new ItemStack(Material.DIAMOND, 1);
		im = is.getItemMeta();
		im.setDisplayName(Main.finalconfig.getString("atm_withdraw").replace('&', '§') + " §a" + Main.economy.format(10000));
		lore.clear();
		lore = new ArrayList<String>();
		lore.add("10000");
		im.setLore(lore);
		is.setItemMeta(im);
		atm_gui.setItem(0, is);
		
		//DEPOSITE
		//
		is = new ItemStack(Material.CLAY_BRICK, 1);
		im = is.getItemMeta();
		im.setDisplayName(Main.finalconfig.getString("atm_deposit").replace('&', '§') + " §4" + Main.economy.format(10));
		lore.clear();
		lore = new ArrayList<String>();
		lore.add("10");
		im.setLore(lore);
		is.setItemMeta(im);
		atm_gui.setItem(5, is);
		
		//
		is = new ItemStack(Material.IRON_INGOT, 1);
		im = is.getItemMeta();
		im.setDisplayName(Main.finalconfig.getString("atm_deposit").replace('&', '§') + " §4" + Main.economy.format(100));
		lore.clear();
		lore = new ArrayList<String>();
		lore.add("100");
		im.setLore(lore);
		is.setItemMeta(im);
		atm_gui.setItem(6, is);
		
		//
		is = new ItemStack(Material.GOLD_INGOT, 1);
		im = is.getItemMeta();
		im.setDisplayName(Main.finalconfig.getString("atm_deposit").replace('&', '§') + " §4" + Main.economy.format(1000));
		lore.clear();
		lore = new ArrayList<String>();
		lore.add("1000");
		im.setLore(lore);
		is.setItemMeta(im);
		atm_gui.setItem(7, is);
		
		//
		is = new ItemStack(Material.DIAMOND, 1);
		im = is.getItemMeta();
		im.setDisplayName(Main.finalconfig.getString("atm_deposit").replace('&', '§') + " §4" + Main.economy.format(10000));
		lore.clear();
		lore = new ArrayList<String>();
		lore.add("10000");
		im.setLore(lore);
		is.setItemMeta(im);
		atm_gui.setItem(8, is);
		
		player.openInventory(atm_gui);
		
	}
	public static void changeMoney(Player p, double amount){
		
	}
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onSign(final SignChangeEvent e){
		final Block b = e.getBlock();
		if(b.getType() == Material.WALL_SIGN || b.getType() == Material.SIGN || b.getType() == Material.SIGN_POST){
			pl.getServer().getScheduler().scheduleAsyncDelayedTask(pl, new Runnable(){
				public void run(){
					if(b.getType() == Material.WALL_SIGN || b.getType() == Material.SIGN || b.getType() == Material.SIGN_POST){
						Sign sign = (Sign) e.getBlock().getState();
						if(sign.getLine(0).equalsIgnoreCase("[atm]")){
							if(!e.getPlayer().hasPermission("tim.atm.place")){
								e.getPlayer().sendMessage("§cYou dont have permissions to build ATM's!");
								sign.setLine(0, "");
								return;
							}else{
								sign.setLine(0, "§cATM");
								sign.update();
								e.getPlayer().sendMessage("§2ATM created! (You can also write something in the Lins 2-4)");
							}
						}
					}
				}
			}, 10L);
		}
	}
}
