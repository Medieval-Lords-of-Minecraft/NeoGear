package me.neoblade298.neogear;

import java.util.ArrayList;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.sucy.skill.SkillAPI;

import me.neoblade298.neogear.listeners.DurabilityListener;
import me.neoblade298.neogear.objects.Shards;


public class Commands implements CommandExecutor{
	
	Gear main;
	ArrayList<String> validAttrs;
	private static final String DEFAULT_SET = "random";
	
	public Commands(Gear main) {
		this.main = main;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String lbl, String[] args) {
		if (sender.hasPermission("neogear.admin")) {
			
			// Gear get [rarity] [type] [lvl]
			if (args.length == 4 && args[0].equalsIgnoreCase("get") && sender instanceof Player) {
				Player p = (Player) sender;
				String rarity = selectRarity(args[1]);
				String type = selectType(args[2], p);
				int lvl = selectLevel(args[3], (Player) sender);
				
				if (rarity == null) {
					sender.sendMessage("§4[§c§lMLMC§4] §cIncorrect format: rarity");
					return true;
				}
				if (type == null) {
					sender.sendMessage("§4[§c§lMLMC§4] §cIncorrect format: type");
					return true;
				}
				if (lvl == -1) {
					sender.sendMessage("§4[§c§lMLMC§4] §cIncorrect format: level");
					return true;
				}
				int failures = p.getInventory().addItem(Gear.getGearConfig(type, lvl).generateItem(rarity, lvl)).size();
				if (failures > 0) {
					Bukkit.getLogger().log(Level.WARNING, "[NeoGear] Failed to get " + rarity + " " + type + " to " + p.getName() + ", inventory full");
				}
				else {
					Bukkit.getLogger().log(Level.INFO, "[NeoGear] Successfully spawned " + rarity + " " + type + " for " + p.getName());
				}
			}

			// Gear give [player] [rarity] [type] [lvl]
			else if (args.length == 5 && args[0].equalsIgnoreCase("give")) {
				Player p = Bukkit.getPlayer(args[1]);
				String rarity = selectRarity(args[2]);
				String type = selectType(args[3], Bukkit.getPlayer(args[1]));
				int lvl = selectLevel(args[4], p);

				if (p == null) {
					sender.sendMessage("§4[§c§lMLMC§4] §cPlayer not found");
					return true;
				}
				if (rarity == null) {
					sender.sendMessage("§4[§c§lMLMC§4] §cIncorrect format: rarity");
					return true;
				}
				if (type == null) {
					sender.sendMessage("§4[§c§lMLMC§4] §cIncorrect format: type");
					return true;
				}
				if (lvl == -1) {
					sender.sendMessage("§4[§c§lMLMC§4] §cIncorrect format: level");
					return true;
				}
				
				int failures = 1;
				try {
					failures = p.getInventory().addItem(Gear.getGearConfig(type, lvl).generateItem(rarity, lvl)).size();
				}
				catch (Exception e) {
					Bukkit.getLogger().log(Level.WARNING,
							"[NeoGear] Failed to generate item with command: " + args[0] + " " + args[1] + " " + args[2] + " " + args[3] + " " + args[4]);
				}
				if (failures > 0) {
					Bukkit.getLogger().log(Level.WARNING, "[NeoGear] Failed to get " + rarity + " " + type + " to " + p.getName() + ", inventory full");
				}
				else {
					Bukkit.getLogger().log(Level.INFO, "[NeoGear] Successfully spawned " + rarity + " " + type + " for " + p.getName());
				}
			}
			// /gear get/giveshard {player} [type] --rarity:[rarity] --level:[level]
			else if (args.length >= 4 && (args[0].equalsIgnoreCase("getshard") || args[0].equalsIgnoreCase("giveshard"))) {
				Player p = (Player) sender;
				int offset = 0;
				if (Bukkit.getPlayer(args[1]) != null) {
					offset++;
					p = Bukkit.getPlayer(args[1]);
				}
				String type = args[1 + offset];
				String rarity = "common";
				int level = 5;
				for (int i = 2 + offset; i < args.length; i++) {
					if (args[i].startsWith("--rarity:")) {
						rarity = args[i].substring(args[i].indexOf(":") + 1);
					}
					else if (args[i].startsWith("--level:")) {
						level = Integer.parseInt(args[i].substring(args[i].indexOf(":") + 1));
					}
				}
				
				ItemStack item = type.equalsIgnoreCase("rarity") ? Shards.getRarityShard(Gear.getRarities().get(rarity), level) :
					Shards.getLevelShard(level);
				int failures = p.getInventory().addItem(item).size();
				if (failures > 0) {
					Bukkit.getLogger().log(Level.WARNING, "[NeoGear] Failed to get " + rarity + " " + type + " upgrade shard to " + p.getName() + ", inventory full");
				}
				else {
					Bukkit.getLogger().log(Level.INFO, "[NeoGear] Successfully spawned " + rarity + " " + level + " " + type + " upgrade shard for " + p.getName());
				}
			}
			else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
				main.loadConfigs();
				sender.sendMessage("§4[§c§lMLMC§4] §7Successfully reloaded");
			}
			else if (args.length == 2 && args[0].equalsIgnoreCase("repair")) {
				Player p = Bukkit.getPlayer(args[1]);
				if (DurabilityListener.fullRepairItem(p, p.getInventory().getItemInMainHand())) {
					sender.sendMessage("§4[§c§lMLMC§4] §7Successfully repaired item");
				}
				else {
					sender.sendMessage("§4[§c§lMLMC§4] §7Failed to repair item!");
				}
			}
			else {
				sender.sendMessage("§4[§c§lMLMC§4] §c/gear get/give {player} [rarity/set] [type/set/auto] [lvl/auto/range] §7- Spawns you gear");
				sender.sendMessage("§4[§c§lMLMC§4] §c/gear getshard/giveshard {player} [rarity/level] --rarity:[rarity] --level:[level] §7- Spawns a shard");
				sender.sendMessage("§4[§c§lMLMC§4] §7[auto] gives gear according to the player's level or class, {range} is formatted as lvl:lvl");
				sender.sendMessage("§4[§c§lMLMC§4] §7Add :lvl to [auto] if you want to set a max level to the gear (auto:30)");
				sender.sendMessage("§4[§c§lMLMC§4] §c/gear repair {player} §7- Repairs a player's mainhand");
				sender.sendMessage("§4[§c§lMLMC§4] §c/gear reload §7- Reloads config");
			}
		}
		else {
			sender.sendMessage("§4[§c§lMLMC§4] §cYou don't have permission to do that!");
		}
		return true;
	}
	
	private String selectRarity(String param) {
		if (main.raritySets.containsKey(param)) {
			double selector = Gear.gen.nextDouble();
			for (String item : main.raritySets.get(param)) {
				String[] iParams = item.split(":");
				selector -= Double.parseDouble(iParams[1]);
				if (selector < 0) {
					return iParams[0];
				}
			}
		}
		else if (Gear.getRarities().containsKey(param)) {
			return param;
		}
		return null;
	}
	
	private String selectType(String param, Player p) {
		if (main.itemSets.containsKey(param)) {
			// return main.itemSets.get(param).get(main.gen.nextInt(main.itemSets.get(param).size()));
			// First get the max possible weight
			return main.itemSets.get(param).pickItem();
		}
		else if (Gear.settings.containsKey(param)) {
			return param;
		}
		else if (param.equalsIgnoreCase("auto")) {
			if (p.hasPermission("filters.neogear")) {
				String pClass = null;
				try {
					pClass = SkillAPI.getPlayerAccountData(p).getActiveData().getClass("class").getData().getName().toLowerCase();
				}
				catch (Exception e) {
					Bukkit.getLogger().log(Level.WARNING,
							"[NeoGear] Failed to get player class of " + p.getName() + ", defaulting to " + DEFAULT_SET);
					pClass = DEFAULT_SET;
				}
				if (main.itemSets.containsKey(pClass)) {
					return main.itemSets.get(pClass).pickItem();
				}
			}
			else {
				return main.itemSets.get(DEFAULT_SET).pickItem();
			}
		}
		Bukkit.getLogger().warning("[NeoGear] Failed to find item set of " + param + ", defaulting to " + DEFAULT_SET);
		return main.itemSets.get(DEFAULT_SET).pickItem();
	}
	
	private int selectLevel(String param, Player p) {
		int level = -1;
		if (StringUtils.isNumeric(param)) {
			int temp = Integer.parseInt(param);
			if (temp <= Gear.lvlMax + Gear.lvlInterval - 1 && temp >= 0) {
				return temp - (temp % Gear.lvlInterval);	// Round to level interval
			}
		}
		else if (param.startsWith("auto")) {
			String[] iParams = param.split(":");
			int pLvl = SkillAPI.getPlayerData(p).getClass("class").getLevel();
			if (iParams.length > 1) {	// Capped generation of gear according to player level
				int max = Integer.parseInt(iParams[1]);
				return pLvl > max ? max : pLvl - (pLvl % Gear.lvlInterval);
			}
			else {	// Uncapped
				return pLvl - (pLvl % Gear.lvlInterval);
			}
		}
		else if (param.contains(":")) {
			String[] iParams = param.split(":");
			int min = Integer.parseInt(iParams[0]);
			int max = Integer.parseInt(iParams[1]);
			if (min <= max) {
				if (min < 0) {
					min = 0;
				}
				if (max > Gear.lvlMax) {
					max = Gear.lvlMax;
				}
				int lvl = min + Gear.gen.nextInt(max - min + 1);
				return lvl - (lvl % Gear.lvlInterval);
			}
		}
		return level;
	}
}