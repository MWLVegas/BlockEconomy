package com.ivalicemud.be;


import java.util.Random;


import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.metadata.*;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.gmail.nossr50.mcMMO;

public class BlockEconomy extends JavaPlugin implements Listener
{

    
    public static BlockEconomy plugin;
	public static net.milkbowl.vault.permission.Permission permission = null;
    public static Economy economy = null;  
    public static mcMMO mcmmo = null;
    
    public BlockEconomy()
    {
    }

    public void onEnable()
    {
        plugin = this;
        
        if (!setupEconomy() ) {
        	getServer().getLogger().severe(String.format("[%s] - This plugin requires Vault to function. Disabling!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        mcmmo = (mcMMO)  getServer().getPluginManager().getPlugin("mcMMO");
        if ( mcmmo != null) {
        	 debug("Using McMMO to track blocks");
        	 }
         else
        	 debug("McMMO not found - using internal block checking.");
         

        getServer().getLogger().info(String.format("[%s] Economy being used: " + economy.getName(), getDescription().getName()));
        LoadConfiguration();
        getServer().getPluginManager().registerEvents(plugin, this);
        setupPermissions();
    }

    public void LoadConfiguration()
    {
    	if (!getConfig().contains("main.debug")) this.getConfig().set("main.debug","false");
    	if (!getConfig().contains("main.announce")) this.getConfig().set("main.announce","false");
    	if (!getConfig().contains("main.defaultPrice")) this.getConfig().set("main.defaultPrice","1");
    	if (!getConfig().contains("main.allowMCMMO")) this.getConfig().set("main.allowMCMMO","false");
    	
    	if (!getConfig().contains("block.0")) {
    		this.getConfig().set("block.0.override","100 // Ignore Muplier/Default - This is the reward");
    		this.getConfig().set("block.0.multiplier","1 //How much * defaultPrice per Block - Use one OR the other. ");
    		this.getConfig().set("block.0.chance","50 // Percentage chance to get the reward for breaking the block");
    		}
        saveConfig();
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    	if (cmd.getName().equalsIgnoreCase("blecon")) {
    	
    		
    		if ( args.length <= 0 )
    		{
    			sender.sendMessage("Block Economy, by raum266 - version "+plugin.getDescription().getVersion());
    			return true;
    		
    		}
    		
    		if ( args.length >= 1 )
    		{
    			
    			if (  args[0].equalsIgnoreCase("report") ) {
    				sender.sendMessage("Default: "+getConfig().getDouble("main.defaultPrice"));
    		  	for( String blockNum : getConfig().getConfigurationSection("block").getKeys(false)) {
    		  		double mult = getConfig().getDouble("block."+blockNum+".multiplier");
    		  		double chance = getConfig().getDouble("block."+blockNum+".chance");
    		  		double override = getConfig().getDouble("block."+blockNum+".override");
    	       		sender.sendMessage("Block " + blockNum + " - M:" +mult + " O:" + override + " %:" + chance);
    		  	}
    			}
    		}
    		
    		return false;
    	}
    	
    	return false;
    }
      
	public void debug(String msg) {
		if (plugin.getConfig().getBoolean("main.debug") == true) {
			Bukkit.getServer().getLogger().info("BlEcon DEBUG: " + msg);
		}
	}

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
    	if ( mcmmo != null )  {
    		mcMMO.getPlaceStore().setTrue( event.getBlock() );
    		debug("Mcmmo setting TRUE");
    	}
    	
    	event.getBlock().setMetadata("playerPlaced", new FixedMetadataValue(plugin,"true"));    	
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event){
    	
    	if ( event.isCancelled() ) { return; }
    	if ( event.getPlayer().getGameMode() == org.bukkit.GameMode.CREATIVE) { return; }
    		
     	if ( mcmmo != null && mcMMO.getPlaceStore().isTrue(event.getBlock() )) { // Mcmmo says it was player placed
    		debug("Player placed - ignore (MCMMO)");
    		return;
    	}
     	
    	if ( event.getBlock().hasMetadata("playerPlaced") && event.getBlock().getMetadata("playerPlaced").get(0).asBoolean() == true ) {
    	debug("Player placed - internal");	
    		return; }
    	
   
    	
    	  org.bukkit.block.Block b = event.getBlock();
    	  int id = b.getTypeId();
    	  double amt = 0;
    	  Random r = new Random();
    	  int rand = r.nextInt(100);
		  int data = b.getData();
    	  String chk = "";
    	  
    	  if ( data == 0 ) { chk = ""+id; } else { chk = id + ":"+data; }
		  
    	  
		  if ( !getConfig().contains("block." + chk ) ) { return; }
    	  if ( getConfig().getInt("block."+ chk + ".chance") <=  rand ) {
    		  return; // Didn't make the chance
    	  }

    	  if ( getConfig().contains("block."+chk+".override") ) { // Has Override 
    		  amt =  getConfig().getDouble("block."+chk+".override");
        	

    		  
    	  }
    	  else if ( getConfig().contains("block."+chk+".multiplier") ) { // Has Multiplier 
    		  amt = getConfig().getDouble("main.defaultPrice") * getConfig().getDouble("block."+chk+".multiplier");
        	

    		  
    	  }
    	  else { // Neither found - report the error 
    		  getServer().getLogger().warning("Error: Block "+chk+" is in the Config, but has no Multiplier or Override!");
    	  } 
    	  
    	  

    	  economy.depositPlayer(event.getPlayer().getName(), amt);
    	  if ( getConfig().getBoolean("main.announce") == true ) {
   			event.getPlayer().sendMessage(ChatColor.GREEN + "You have earned " + amt + economy.currencyNamePlural() + " while mining." );      		  
      	  

    	  }
    	  
       
    }       

        private boolean setupPermissions()
        {
            RegisteredServiceProvider<net.milkbowl.vault.permission.Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
            if (permissionProvider != null) {
                permission = permissionProvider.getProvider();
            }
            return (permission != null);
        }

        private boolean setupEconomy()
        {
            if (getServer().getPluginManager().getPlugin("Vault") == null) {
                return false;
            }
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
                return false;
            }
            economy = rsp.getProvider();
            return economy != null;
        }	

    
}