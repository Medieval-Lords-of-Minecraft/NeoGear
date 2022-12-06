package me.neoblade298.neogear.listeners;

import java.util.ArrayList;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import com.sucy.skill.api.event.PlayerSkillCastSuccessEvent;
import com.sucy.skill.api.util.FlagManager;

import de.tr7zw.nbtapi.NBTItem;
import me.neoblade298.neogear.Gear;

public class DurabilityListener implements Listener {
	Gear main;
	private final static Random gen = new Random();
	private final static String DURABILITYSTRING = "§7Durability ";
	private final static String WEAPONCD = "WeaponDurability";
	private final static String ARMORCD = "ArmorDurability";
	private final static int CDTIME = 20;
	
	public DurabilityListener(Gear main) {
		this.main = main;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onDamageMelee(EntityDamageByEntityEvent e) {
		Entity cause = e.getDamager();
		Entity target = e.getEntity();
		if (e.getDamage() <= 4) {
			return;
		}

		// Lowers durability of damager
		if (cause instanceof Player) {
			reduceWeaponDurability((Player) cause);
		}

		// Lowers durability of damaged
		if (target instanceof Player) {
			reduceArmorDurability((Player) target);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onShoot(EntityShootBowEvent e) {
		if (e.getEntity() instanceof Player) {
			reduceWeaponDurability((Player) e.getEntity());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onSkillCast(PlayerSkillCastSuccessEvent e) {
		reduceWeaponDurability(e.getPlayer());
	}

	@EventHandler(ignoreCancelled = true)
	public void onDurabilityLoss(PlayerItemDamageEvent e) {
		if (e.getItem().hasItemMeta() && e.getItem().getItemMeta().hasLore()) {
			if (!isQuestItem(e.getItem()) && !isOldQuestItem(e.getItem())) return;
			
			String world = e.getPlayer().getWorld().getName();
			if (world.equalsIgnoreCase("Argyll") || world.equalsIgnoreCase("Dev")) {
				e.setCancelled(true);
			}
		}
		if (e.getPlayer().getWorld().getName().equalsIgnoreCase("ClassPvp")) {
			e.setCancelled(true);
		}
	}
	
	private static boolean isQuestItem(ItemStack item) {
		if (item != null && item.hasItemMeta() && item.getItemMeta().hasLore()) {
			NBTItem nbti = new NBTItem(item);
			return nbti.hasKey("gear");
		}
		return false;
	}
	
	private static boolean isOldQuestItem(ItemStack item) {
		if (item != null && item.hasItemMeta() && item.getItemMeta().hasLore()) {
			return item.getItemMeta().getLore().get(0).contains("Tier");
		}
		return false;
	}
	
	private void reduceArmorDurability(Player p) {
		if (FlagManager.hasFlag(p, ARMORCD)) return;
		if (p.getWorld().getName().equalsIgnoreCase("ClassPvP")) {
			return;
		}
		
		FlagManager.addFlag(p, p, ARMORCD, CDTIME);
		reduceDurability(p, p.getInventory().getHelmet(), 1);
		reduceDurability(p, p.getInventory().getChestplate(), 2);
		reduceDurability(p, p.getInventory().getLeggings(), 3);
		reduceDurability(p, p.getInventory().getBoots(), 4);
		if (p.isBlocking()) {
			if (p.getEquipment().getItemInMainHand().getType() == Material.SHIELD) {
				reduceDurability(p, p.getInventory().getItemInMainHand(), 0);
			}
			if (p.getEquipment().getItemInOffHand().getType() == Material.SHIELD) {
				reduceDurability(p, p.getInventory().getItemInOffHand(), 5);
			}
		}
	}
	
	private void reduceWeaponDurability(Player p) {
		if (FlagManager.hasFlag(p, WEAPONCD)) return;
		if (p.getWorld().getName().equalsIgnoreCase("ClassPvP")) {
			return;
		}

		FlagManager.addFlag(p, p, WEAPONCD, CDTIME);
		if (gen.nextDouble() > 0.5) {
			if (isQuestItem(p.getInventory().getItemInMainHand())) {
				reduceDurability(p, p.getInventory().getItemInMainHand(), 0);
			}
			else {
				reduceDurability(p, p.getInventory().getItemInOffHand(), 5);
			}
		}
		else {
			if (isQuestItem(p.getInventory().getItemInOffHand())) {
				reduceDurability(p, p.getInventory().getItemInOffHand(), 5);
			}
			else {
				reduceDurability(p, p.getInventory().getItemInMainHand(), 0);
			}
		}
	}
	
	private void reduceDurability(Player p, ItemStack item, int slot) {
		if (!isQuestItem(item)) return;
		
		ItemMeta im = item.getItemMeta();
		ArrayList<String> lore = (ArrayList<String>) im.getLore();
		String line = lore.get(lore.size() - 1);
		
		if (line.contains(DURABILITYSTRING)) {
			String end = line.substring(line.indexOf(" ") + 1);
			String[] numbers = end.split("/");
			double d = Integer.parseInt(numbers[0].trim());
			double dM = Integer.parseInt(numbers[1].trim());
			
			if (d == 25 && p.hasPermission("donator.warndurability")) {
				p.sendMessage("§4[§c§lMLMC§4] §4WARNING: Your item, " + item.getItemMeta().getDisplayName() + "§4, is at 25 durability!");
			}

			d -= 1;
			if (d <= 0) {
				breakItem(item, p, slot);
				return;
			}
			
			line = DURABILITYSTRING + (int) d + " / " + (int) dM;
			if ((!im.isUnbreakable()) && (item.getType().getMaxDurability() > 0)) {
				double pct = 1.0D - d / dM;

				double dN = pct * (item.getType().getMaxDurability() - 1);
				((Damageable) im).setDamage((int) dN);
			}
		}
		lore.set(lore.size() - 1, line);
		im.setLore(lore);
		item.setItemMeta(im);
	}
	
	public static String repairItem(Player p, ItemStack item, double percentage) {
		if (!isQuestItem(item)) return "Item is not a quest item!";
		
		ItemMeta im = item.getItemMeta();
		ArrayList<String> lore = (ArrayList<String>) im.getLore();
		String line = lore.get(lore.size() - 1);
		
		if (line.contains(DURABILITYSTRING)) {
			String end = line.substring(line.indexOf(" ") + 1);
			String[] numbers = end.split("/");
			double d = Integer.parseInt(numbers[0].trim());
			double dM = Integer.parseInt(numbers[1].trim());
			
			if (d == dM) {
				return "Item is already fully repaired!";
			}
			
			d += Math.round(dM * percentage);
			d = Math.min(d, dM);
			
			line = DURABILITYSTRING + (int) d + " / " + (int) dM;
			if ((!im.isUnbreakable()) && (item.getType().getMaxDurability() > 0)) {
				double pct = 1.0D - d / dM;

				double dN = pct * (item.getType().getMaxDurability() - 1);
				((Damageable) im).setDamage((int) dN);
			}
		}
		lore.set(lore.size() - 1, line);
		im.setLore(lore);
		item.setItemMeta(im);
		
		// Update item
		NBTItem nbti = new NBTItem(item);
		String type = nbti.getString("gear");
		int level = nbti.getInteger("level");
		Gear.getGearConfig(type, level).updateStats(p, item, true);
		return null;
	}
	
	public static boolean fullRepairItem(Player p, ItemStack item) {
		if (item.getAmount() > 1) {
			return false;
		}
		
		ItemMeta im = item.getItemMeta();
		if (im.hasLore()) {
			ArrayList<String> lore = (ArrayList<String>) im.getLore();
			String line = lore.get(lore.size() - 1);
			
			if (line.contains(DURABILITYSTRING)) {
				String end = line.substring(line.indexOf(" ") + 1);
				String[] numbers = end.split("/");
				double dM = Integer.parseInt(numbers[1].trim());
				
				line = DURABILITYSTRING + (int) dM + " / " + (int) dM;
			}
			lore.set(lore.size() - 1, line);
			im.setLore(lore);
		}
		
		if ((!im.isUnbreakable()) && (item.getType().getMaxDurability() > 0)) {
			((Damageable) im).setDamage(0);
		}
		item.setItemMeta(im);
		
		NBTItem nbti = new NBTItem(item);
		
		// Update item
		if (nbti.hasKey("gear")) {
			String type = nbti.getString("gear");
			int level = nbti.getInteger("level");
			Gear.getGearConfig(type, level).updateStats(p, item, true);
		}
		
		return true;
	}
	
	private void breakItem(ItemStack item, Player p, int slot) {
		Bukkit.getServer().getPluginManager().callEvent(new PlayerItemBreakEvent(p, item));
		item.setAmount(0);
		p.getWorld().playSound(p.getLocation(), "entity.item.break", 1.0F, 1.0F);
	}
	

	@EventHandler
	public void onRepairItem(InventoryClickEvent e) {
		if (!e.isLeftClick()) {
			return;
		}
		if (e.getCursor() == null) {
			return;
		}
		if (e.getCurrentItem() == null) {
			return;
		}
		
		Player p = (Player) e.getWhoClicked();
		ItemStack repair = e.getCursor();
		ItemStack item = e.getCurrentItem();

		if (item == null || item.getType().isAir() || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
			return;
		}
		if (repair == null || repair.getType().isAir() || !repair.hasItemMeta() || !repair.getItemMeta().hasDisplayName()) {
			return;
		}

		NBTItem nbtr = new NBTItem(repair);
		NBTItem nbti = new NBTItem(item);
		if (!nbti.hasKey("gear")) {
			return;
		}
		if (!nbtr.hasKey("potency")) {
			return;
		}

		int repairLevel = nbtr.getInteger("level");
		double percentage = nbtr.getDouble("potency") / 100;
		int itemLevel = nbti.getInteger("level");
		
		if (itemLevel > repairLevel) {
			p.sendMessage("§4[§c§lMLMC§4] §cThis repair kit's level is too low!");
			return;
		}
		if (item.getAmount() != 1) {
			p.sendMessage("§4[§c§lMLMC§4] §cCannot repair more than one item at a time!");
			return;
		}
		
		ItemStack clone = repair.clone();
		clone.setAmount(1);
		String result = repairItem(p, item, percentage);
		if (result == null) {
			e.setCancelled(true);
			p.getOpenInventory().close();
			p.getInventory().removeItem(clone);
			p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 1F, 1F);
			p.sendMessage("§4[§c§lMLMC§4] §7Successfully repaired item!");
		}
		else {
			p.sendMessage("§4[§c§lMLMC§4] §c" + result);
		}
	}
}
