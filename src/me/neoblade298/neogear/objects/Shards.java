package me.neoblade298.neogear.objects;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import de.tr7zw.nbtapi.NBTItem;
import net.md_5.bungee.api.ChatColor;

public class Shards {
	public static ItemStack getRarityShard(Rarity rarityMax, int levelMax) {
		ItemStack item = new ItemStack(Material.AMETHYST_SHARD);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName("§6[" + levelMax + "] §c" + ChatColor.stripColor(rarityMax.displayName) + " Upgrade Shard");
		List<String> lore = new ArrayList<String>();
		lore.add("§7Increases rarity of quest equipment.");
		lore.add("§7Works on up to Lv " + levelMax + " " + rarityMax.displayName + " §7gear");
		lore.add("§7Drag and drop onto item to use!");
		meta.setLore(lore);
		meta.addEnchant(Enchantment.DURABILITY, 1, true);
		item.setItemMeta(meta);
		NBTItem nbti = new NBTItem(item);
		nbti.setString("shard", "rarity");
		nbti.setInteger("level", levelMax);
		nbti.setString("rarity", rarityMax.key);
		return nbti.getItem();
	}
	public static ItemStack getLevelShard(int levelMax) {
		ItemStack item = new ItemStack(Material.AMETHYST_SHARD);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName("§6[" + levelMax + "] §cLevel Upgrade Shard");
		List<String> lore = new ArrayList<String>();
		lore.add("§7Increases level of quest equipment by 5.");
		lore.add("§7Works on up to §6Lv " + levelMax + " §7gear");
		lore.add("§7Drag and drop onto item to use!");
		meta.setLore(lore);
		meta.addEnchant(Enchantment.DURABILITY, 1, true);
		item.setItemMeta(meta);
		NBTItem nbti = new NBTItem(item);
		nbti.setString("shard", "level");
		nbti.setInteger("level", levelMax);
		return nbti.getItem();
	}
}
