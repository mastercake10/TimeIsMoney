package de.Linus122.TimeIsMoney;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import modules.atm.ATM;
import net.milkbowl.vault.economy.Economy;

public class Main extends JavaPlugin{
	
	List<Payout> payouts = new ArrayList<Payout>();
	HashMap<String, Double> payedMoney = new HashMap<String, Double>();
	
	HashMap<UUID, Integer> onlineSeconds = new HashMap<UUID, Integer>();
	
	HashMap<UUID, Location> lastLocation = new HashMap<UUID, Location>();
	
	public static Economy economy = null;
	public static Utils utils = null;
	String message;
	
	ConsoleCommandSender clogger = this.getServer().getConsoleSender();
	
	public static int CFG_VERSION = 12;
	public static int PL_VERSION;

	int currentDay = 0;
	
	public static YamlConfiguration finalconfig;
	
	boolean use18Features = true;
	
	public static List<String> disabledWorlds;
	public static HashMap<String, UUID> boundIPs = new HashMap<String, UUID>();
	
	@SuppressWarnings({ "deprecation", "unchecked" })
	@Override
	public void onEnable(){
		PL_VERSION = Integer.parseInt(((Plugin)this).getDescription().getVersion());
		this.getCommand("timeismoney").setExecutor(new Cmd(this));
		
		currentDay = (new Date()).getDay();
	
		File config = new File("plugins/TimeIsMoney/config.yml");
		if(config.exists()){
			YamlConfiguration cfg = YamlConfiguration.loadConfiguration(config);	
			String old_config = "config_old " + cfg.getInt("configuration-version") + ".yml";
			if(cfg.contains("configuration-version")){
				if(cfg.getInt("configuration-version") < CFG_VERSION){
					clogger.sendMessage("[TimeIsMoney] §cYOU ARE USING AN OLD CONFIG-VERSION. The plugin CANT work with this.");
					clogger.sendMessage("[TimeIsMoney] §cI have created an new config for you. The old one is saved as config_old.yml.");
					config.renameTo(new File("plugins/TimeIsMoney/" + old_config));
				}
			}
			this.saveDefaultConfig();
			for(String key : cfg.getConfigurationSection("").getKeys(true)){
				if(!this.getConfig().contains(key)){
					this.getConfig().set(key, cfg.get(key));
				}
			}
		}else{
			this.saveDefaultConfig();
		}
		
		new ATM(this);
		
		finalconfig = YamlConfiguration.loadConfiguration(config);
		disabledWorlds = getConfig().getStringList("disabled_in_worlds");
		final int seconds = getConfig().getInt("give_money_every_second");
		Bukkit.getScheduler().runTaskTimerAsynchronously(this, new Runnable(){
			public void run(){
				for(Player p : Bukkit.getOnlinePlayers()){
					if(disabledWorlds.contains(p.getWorld().getName())) continue;
					if(!boundIPs.containsKey(p.getAddress().getHostName())){
						boundIPs.put(p.getAddress().getHostName(), p.getUniqueId());
					}
					if(onlineSeconds.containsKey(p.getUniqueId())){
						
						onlineSeconds.put(p.getUniqueId(), onlineSeconds.get(p.getUniqueId()) + 1);
					}else{
						onlineSeconds.put(p.getUniqueId(), 1);
					}
					if(onlineSeconds.get(p.getUniqueId()) > seconds){
						pay(p);
						onlineSeconds.remove(p.getUniqueId());
					}
				}
			}
		}, 20L, 20L);
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){
			public void run(){
				if(currentDay != new Date().getDay()){ //Next day, clear payouts!
					log("Cleared all payouts");
					payedMoney.clear();
					currentDay = new Date().getDay();
				}
			}
		}, 20L * 60, 20L * 60 * 15);
		setupEconomy();
		
		message = finalconfig.getString("message");
		message = message.replace('&', '§');

		try{
		    FileInputStream fis = new FileInputStream(new File("plugins/TimeIsMoney/payed_today.data"));
		    ObjectInputStream ois = new ObjectInputStream(fis);
		    payedMoney = (HashMap<String, Double>) ((HashMap<String, Double>) ois.readObject()).clone();

		    ois.close();	
		}catch(Exception e){
			
		}
		
		loadPayouts();
		
		
		
		String packageName = this.getServer().getClass().getPackage().getName();
        // Get full package string of CraftServer.
        // org.bukkit.craftbukkit.version
        String Bukkitversion = packageName.substring(packageName.lastIndexOf('.') + 1);
        // Get the last element of the package
        try {
            final Class<?> clazz = Class.forName(Bukkitversion + ".NBTUtils");
            // Check if we have a NMSHandler class at that location.
            if (Utils.class.isAssignableFrom(clazz)) { // Make sure it actually implements NMS
                utils = (Utils) clazz.getConstructor().newInstance(); // Set our handler
     
            }
            
        } catch (final Exception e) {
            this.getLogger().severe("Actionbars are not supported on your spigot version, sorry.");
            use18Features = false;
            return;
        }

		if(Bukkit.getPluginManager().isPluginEnabled("Essentials")){
			clogger.sendMessage("Time is Money: Essentials found. Hook in it -> Will use Essentials's AFK feature if afk is enabled.");
		}
		clogger.sendMessage("§aTime is Money §2v" + PL_VERSION + " §astarted.");
	}
	@Override
	public void onDisable(){
	    FileOutputStream fos;
	    try {
	    	fos = new FileOutputStream(new File("plugins/TimeIsMoney/payed_today.data"));
	        ObjectOutputStream oos = new ObjectOutputStream(fos);
	        oos.writeObject(payedMoney);
	        oos.close();
	    }catch(Exception e){
	    	
	    }
	}
	public void reload(){
		//File config = new File("plugins/TimeIsMoney/config.yml");
		//finalconfig = YamlConfiguration.loadConfiguration(config);
		Bukkit.getPluginManager().disablePlugin(this);
		Bukkit.getPluginManager().enablePlugin(this);
		//this.onDisable();
		//this.onEnable();
		//loadPayouts();
	}
	public void loadPayouts(){
		try{
			payouts.clear();
			for(String key : finalconfig.getConfigurationSection("payouts").getKeys(false)){
				Payout payout = new Payout();
				payout.max_payout_per_day = finalconfig.getDouble("payouts." + key + ".max_payout_per_day");
				payout.payout_amount = finalconfig.getDouble("payouts." + key + ".payout_amount");
				if(finalconfig.getString("payouts." + key + ".permission") != null){
					payout.permission = finalconfig.getString("payouts." + key + ".permission");	
				}
				if(finalconfig.getString("payouts." + key + ".commands") != null){
					payout.commands = finalconfig.getStringList("payouts." + key + ".commands");
				}
				
				if(finalconfig.getString("payouts." + key + ".chance") != null){
					payout.chance = finalconfig.getInt("payouts." + key + ".chance");
				}
				payouts.add(payout);
			}
			clogger.sendMessage("[TimeIsMoney] §aLoaded " + finalconfig.getConfigurationSection("payouts").getKeys(false).size() + " Payouts!");
		}catch(Exception e){
			clogger.sendMessage("[TimeIsMoney] §aFailed to load Payouts! (May made a mistake in config.yml?)");
		}
	}
    boolean setupEconomy()
    {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }
    public Payout getPayOutForPlayer(Player p){
    	Payout finalpayout = null;
    	if(!this.getConfig().getBoolean("choose-payout-by-chance")){
    		//by Permission
    		for(Payout payout: payouts){
    			if(payout.permission == "") finalpayout = payout;
    			if(p.hasPermission(payout.permission)){
    				finalpayout = payout;
    			}
    		}	
    	}else{
    		//by Chance
    		Random rnd = new Random();
    		List<Payout> list = new ArrayList<Payout>();
    		for(Payout payout: payouts){
    			for(int i = 0; i < payout.chance; i++) list.add(payout);
    		}
    		finalpayout = list.get(rnd.nextInt(list.size() - 1));
    	}
    	return finalpayout;
    }
	@SuppressWarnings("deprecation")
	public void pay(Player p){
		if(p == null) return;
		
		//REACHED MAX PAYOUT CHECK
		double payed = 0;
		if(payedMoney.containsKey(p.getName())){
			payed = payedMoney.get(p.getName());
		}
		Payout payout = getPayOutForPlayer(p);
		if(payout == null) return;
		if(payout.max_payout_per_day != -1){
			if(payed >= payout.max_payout_per_day){ //Reached max payout
				if(finalconfig.getBoolean("display-messages-in-chat")){
					sendMessage(p, finalconfig.getString("message_payoutlimit_reached"));
				}
				if(finalconfig.getBoolean("display-messages-in-actionbar") && use18Features){
					sendActionbar(p, finalconfig.getString("message_payoutlimit_reached_actionbar"));
				}
				return;
			}	
		}
		
		if(!finalconfig.getBoolean("allow-multiple-accounts")){
			if(boundIPs.containsKey(p.getAddress().getHostName())){
				if(!boundIPs.get(p.getAddress().getHostName()).equals(p.getUniqueId())){
					sendMessage(p, finalconfig.getString("message_multiple_ips"));
					return;
				}
			}
		}
		
		//AFK CHECK
		if(!finalconfig.getBoolean("afk_payout") && !p.hasPermission("tim.afkbypass")){
			//ESENTIALS_AFK_FEATURE
			if(Bukkit.getServer().getPluginManager().isPluginEnabled("Essentials")){
				com.earth2me.essentials.Essentials essentials = (com.earth2me.essentials.Essentials) Bukkit.getServer().getPluginManager().getPlugin("Essentials");
			    if(essentials.getUser(p).isAfk()){
			    	//AFK
					if(finalconfig.getBoolean("display-messages-in-chat")){
						sendMessage(p, finalconfig.getString("message_afk"));	
					}
					if(finalconfig.getBoolean("display-messages-in-actionbar") && use18Features){
						sendActionbar(p, finalconfig.getString("message_afk_actionbar"));
					}
					return;
			    }
			}else
			//PLUGIN_AFK_FEATURE
			if(lastLocation.containsKey(p.getUniqueId())){ //AntiAFK
				if(lastLocation.get(p.getUniqueId()).getX() == p.getLocation().getX() && lastLocation.get(p.getUniqueId()).getY() == p.getLocation().getY() && lastLocation.get(p.getUniqueId()).getZ() == p.getLocation().getZ() || lastLocation.get(p.getUniqueId()).getYaw() == p.getLocation().getYaw()){
					//AFK
					if(finalconfig.getBoolean("display-messages-in-chat")){
						sendMessage(p, finalconfig.getString("message_afk"));	
					}
					if(finalconfig.getBoolean("display-messages-in-actionbar") && use18Features){
						sendActionbar(p, finalconfig.getString("message_afk_actionbar"));
					}
					return;
				}
			}	
		}
		
		//DEPOSIT
		if(finalconfig.getBoolean("store-money-in-bank")){
			ATM.depositBank(p, payout.payout_amount);
		}else{
			double before = 0;
			if(economy.hasAccount(p)){
				before = economy.getBalance(p);
			}
		
			economy.depositPlayer(p, payout.payout_amount);
			log(p.getName() + ": Deposited: " + payout.payout_amount + " Balance-before: " + before + " Balance-now: " + economy.getBalance(p));
			
		}
		if(finalconfig.getBoolean("display-messages-in-chat")){
			sendMessage(p, message.replace("%money%", economy.format(payout.payout_amount)));	
		}
		if(finalconfig.getBoolean("display-messages-in-actionbar") && use18Features){
			sendActionbar(p, message.replace("%money%", economy.format(payout.payout_amount)));
		}
		for(String cmd : payout.commands){
			dispatchCommandSync(cmd.replace("/", "").replaceAll("%player%", p.getName()));
		}
		
		//ADD PAYED MONEY
		if(payedMoney.containsKey(p.getName())){
			payedMoney.put(p.getName(), payedMoney.get(p.getName()) + payout.payout_amount);
		}else{
			payedMoney.put(p.getName(), payout.payout_amount);
		}

		lastLocation.put(p.getUniqueId(), p.getLocation());
	
	}
	public void dispatchCommandSync(final String cmd){
		final Server server = this.getServer();
		
		this.getServer().getScheduler().runTask(this, new Runnable(){
			
			public void run(){
				server.dispatchCommand(server.getConsoleSender(), cmd);
			}
		});
	}
	@SuppressWarnings("deprecation")
	public void log(String msg){
		if(!this.getConfig().getBoolean("debug-log")){
			return;	
		}
		Timestamp currentTimestamp = new Timestamp(Calendar.getInstance().getTime().getTime());

		File file = new File("plugins/TimeIsMoney/log.txt");
			try {
				if(!file.exists()){
					file.createNewFile();
				}
				FileReader pr = new FileReader(file);
				int number = 0;
				StringBuffer text = new StringBuffer();
				while((number = pr.read()) != -1){
					
					text.append((char) number);
				}
				text.append(currentTimestamp.toGMTString() + ":" + msg + "\n");
				PrintWriter pw = new PrintWriter(file);
				pw.print(text);
				
				pw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	public void sendMessage(Player p, String msg){
		if(msg == null) return;
		if(msg.length() == 0) return;
		p.sendMessage(msg.replace('&', '§'));
	}
	public void sendActionbar(final Player p, final String msg){
		if(msg.length() == 0) return;
		int times = finalconfig.getInt("display-messages-in-actionbar-time");
		if(times == 1){
			if(utils != null){
				utils.sendActionBarMessage(p, msg);	
			}
		}else if(times > 1){
			if(utils != null){
				utils.sendActionBarMessage(p, msg);	
			}
			times--;
			for(int i = 0; i < times; i++){
				Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable(){
					public void run(){
						utils.sendActionBarMessage(p, msg.replace('&', '§'));
					}
				}, 20L * i);
			}
		}
	}
	private boolean unloadPlugin(String pluginName)
		    throws Exception
		  {
		    PluginManager manager = getServer().getPluginManager();
		    SimplePluginManager spmanager = (SimplePluginManager)manager;
		    if (spmanager != null)
		    {
		      Field pluginsField = spmanager.getClass().getDeclaredField("plugins");
		      pluginsField.setAccessible(true);
		      List<Plugin> plugins = (List)pluginsField.get(spmanager);
		      
		      Field lookupNamesField = spmanager.getClass().getDeclaredField("lookupNames");
		      lookupNamesField.setAccessible(true);
		      Map<String, Plugin> lookupNames = (Map)lookupNamesField.get(spmanager);
		      
		      Field commandMapField = spmanager.getClass().getDeclaredField("commandMap");
		      commandMapField.setAccessible(true);
		      SimpleCommandMap commandMap = (SimpleCommandMap)commandMapField.get(spmanager);
		      
		      Field knownCommandsField = null;
		      Map<String, Command> knownCommands = null;
		      if (commandMap != null)
		      {
		        knownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
		        knownCommandsField.setAccessible(true);
		        knownCommands = (Map)knownCommandsField.get(commandMap);
		      }
		      Plugin plugin;
		      Iterator<Map.Entry<String, Command>> it;
		      for (Plugin plugin1 : manager.getPlugins()) {
		        if (plugin1.getDescription().getName().equalsIgnoreCase(pluginName))
		        {
		          manager.disablePlugin(plugin1);
		          if ((plugins != null) && (plugins.contains(plugin1))) {
		            plugins.remove(plugin1);
		          }
		          if ((lookupNames != null) && (lookupNames.containsKey(pluginName))) {
		            lookupNames.remove(pluginName);
		          }
		          if (commandMap != null) {
		            for (it = knownCommands.entrySet().iterator(); it.hasNext();)
		            {
		              Map.Entry<String, Command> entry = (Map.Entry)it.next();
		              if ((entry.getValue() instanceof PluginCommand))
		              {
		                PluginCommand command = (PluginCommand)entry.getValue();
		                if (command.getPlugin() == plugin1)
		                {
		                  command.unregister(commandMap);
		                  it.remove();
		                }
		              }
		            }
		          }
		        }
		      }
		    }
		    else
		    {

		      return true;
		    }


		    return true;
		  }
		  
		  private boolean loadPlugin(String pluginName)
		  {
		    try
		    {
		      PluginManager manager = getServer().getPluginManager();
		      Plugin plugin = manager.loadPlugin(new File("plugins", pluginName + ".jar"));
		      if (plugin == null)
		      {
		        return false;
		      }
		      plugin.onLoad();
		      manager.enablePlugin(plugin);
		    }
		    catch (Exception e)
		    {

		      return false;
		    }

		    return true;
		  }
		  
		  private boolean reloadPlugin(String pluginName)
		    throws Exception
		  {
		    boolean unload = unloadPlugin(pluginName);
		    boolean load = loadPlugin(pluginName);

		    if ((unload) && (load))
		    {

		    }
		    else
		    {

		      return false;
		    }
		    return true;
		  }
}
