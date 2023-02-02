package me.neoblade298.neogear.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import de.tr7zw.nbtapi.NBTItem;
import me.Neoblade298.NeoProfessions.Augments.Augment;
import me.Neoblade298.NeoProfessions.Inventories.CreateSlotInventory;
import me.Neoblade298.NeoProfessions.Managers.AugmentManager;
import me.Neoblade298.NeoProfessions.Objects.ScaleSet;
import me.neoblade298.neogear.Gear;
import net.md_5.bungee.api.ChatColor;

public class GearConfig {
	public static final Pattern HEX_PATTERN = Pattern.compile("&(#[A-Fa-f0-9]{6})");
	public static final String DURAPREFIX = "§7Durability ";
	public String id, type, title;
	public Material material;
	public ArrayList<String> prefixes, displayNames;
	public ArrayList<String> lore;
	public int duraBase;
	public int model;
	public ArrayList<String> requiredAugments;
	public ArrayList<Enchant> requiredEnchants, optionalEnchants;
	public int enchantmentMin, enchantmentMax;
	public int startingSlotsBase, startingSlotsRange;
	public int slotsMax;
	public int version;
	public HashMap<String, AttributeSet> attributes;
	public HashMap<Rarity, RarityBonuses> rarities;
	public double configPrice, value;
	
	private static HashMap<String, String> rarityUpgrades = new HashMap<String, String>();
	private static HashSet<String> decimalAttrs = new HashSet<String>();
	
	static {
		rarityUpgrades.put("common", "uncommon");
		rarityUpgrades.put("uncommon", "rare");
		rarityUpgrades.put("rare", "epic");
		rarityUpgrades.put("epic", "legendary");
		
		decimalAttrs.add("rrg");
	}
	
	public GearConfig(String id, String type, String title, int model, Material material, ArrayList<String> prefixes, ArrayList<String> displayNames,
			int duraBase, ArrayList<Enchant> requiredEnchants, ArrayList<Enchant> optionalEnchants, ArrayList<String> requiredAugments,
			int enchantmentMin, int enchantmentMax, HashMap<String, AttributeSet> attributes, HashMap<Rarity, RarityBonuses> rarities, int slotsMax,
			int startingSlotsBase, int startingSlotsRange, double price, int version, ArrayList<String> lore) {
		
		// Add color codes to all strings necessary
		for (String prefix : prefixes) {
			prefix = prefix.replaceAll("&", "§");
		}
		for (String displayName : displayNames) {
			displayName = displayName.replaceAll("&", "§");
		}
		
		this.id = id;
		if (title == null) {
			this.title = "§7Standard " + type;
		}
		else {
			this.title = title;
		}
		this.type = type;
		this.model = model;
		this.material = material;
		this.prefixes = prefixes;
		this.displayNames = displayNames;
		this.duraBase = duraBase;
		this.requiredEnchants = requiredEnchants;
		this.optionalEnchants = optionalEnchants;
		this.requiredAugments = requiredAugments;
		this.enchantmentMax = enchantmentMax;
		this.enchantmentMin = enchantmentMin;
		this.attributes = attributes;
		this.rarities = rarities;
		this.configPrice = price;
		this.slotsMax = slotsMax;
		this.startingSlotsBase = startingSlotsBase;
		this.startingSlotsRange = startingSlotsRange;
		this.version = version;
		this.lore = lore;
	}
	
	public double calculatePrice(ItemStack item) {
		NBTItem nbti = new NBTItem(item);
		if (this.configPrice == -1) {
			int level = nbti.getInteger("level");
			Rarity rarity = Gear.getRarities().get(nbti.getString("rarity"));
			double price = level * rarity.priceModifier;
			
			int slotsCreated = nbti.getInteger("slotsCreated");
			for (int i = 1; i <= slotsCreated; i++) {
				ScaleSet set = CreateSlotInventory.getGoldPrices().get(i);
				int augLevel = nbti.getInteger("slot" + i + "Level");
				price += set.getResult(level) * 0.1;
				price += set.getResult(augLevel) * 0.05;
			}
			return price;
		}
		else {
			return this.configPrice;
		}
	}
	
	public double calculatePrice(int level, Rarity rarity, int slotsCreated) {
		if (this.configPrice == -1) {
			double price = level * rarity.priceModifier;
			for (int i = 1; i <= slotsCreated; i++) {
				ScaleSet set = CreateSlotInventory.getGoldPrices().get(i);
				price += set.getResult(level) * 0.05;
				price += CreateSlotInventory.getEssencePrices().get(i) * level * 0.5;
			}
			return price;
		}
		else {
			return this.configPrice;
		}
	}
	
	public ItemStack generateItem(String rarityString, int level) {
		ItemStack item = new ItemStack(material);
		Rarity rarity = Gear.getRarities().get(rarityString);
		if (rarities.get(rarity).material != null) {
			item = new ItemStack(rarities.get(rarity).material);
		}
		
		ItemMeta meta = item.getItemMeta();
		ArrayList<String> lore = new ArrayList<String>();
		
		// If item is a shield, give it damage
		if (material.equals(Material.SHIELD)) {
			meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(new UUID(1L, 2L), "Shield", 5, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));
		}
		
		// Decide Prefix if exists
		String prefix;
		ArrayList<String> rarityPrefixes = rarities.get(rarity).prefixes;
		if (rarityPrefixes != null && rarityPrefixes.size() != 0) {
			prefix = rarityPrefixes.get(Gear.gen.nextInt(rarityPrefixes.size())) + " ";
		}
		else if (prefixes.size() > 0) {
			prefix = prefixes.get(Gear.gen.nextInt(prefixes.size())) + " ";
		}
		else {
			prefix = "";
		}
		
		// Rest of display, use color code only if nonexistent
		String display = displayNames.get(Gear.gen.nextInt(displayNames.size()));
		if (display.contains("&")) {
			meta.setDisplayName((prefix + display).replaceAll("&", "§"));
		}
		else {
			meta.setDisplayName(rarity.colorCode + prefix + display);
		}
		
		if (model != -1) meta.setCustomModelData(model);
		
		
		// Add required enchantments
		if (rarity.isEnchanted) {
			meta.addEnchant(Enchantment.LUCK, 1, true);
		}
		for (Enchant enchant : requiredEnchants) {
			int lv = enchant.min + Gear.gen.nextInt(enchant.max - enchant.min + 1);
			if (lv > 0) {
				meta.addEnchant(enchant.enchantment, lv, true);
			}
		}
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		
		// Add optional enchantments
		if (enchantmentMax > 0) {
			int optEnchantNum = enchantmentMin + Gear.gen.nextInt(enchantmentMax - enchantmentMin);
			ArrayList<Enchant> optEnchants = new ArrayList<Enchant>();
			optEnchants.addAll(optionalEnchants);
			if (optEnchants.size() > 0) {
				for (int i = 0; i < optEnchantNum; i++) {
					Enchant enchant = optEnchants.get(Gear.gen.nextInt(optEnchants.size()));
					int lv = enchant.min + Gear.gen.nextInt(enchant.max - enchant.min + 1);
					if (lv > 0) {
						meta.addEnchant(enchant.enchantment, lv, true);
						optEnchants.remove(enchant);
					}
				}
			}
		}
		
		// Slots and augments
		int maxSlots = slotsMax;
		if (rarities.get(rarity).slotsMax != -1) {
			maxSlots = rarities.get(rarity).slotsMax;
		}
		int currentSlot = 0;
		int numSlots = startingSlotsBase;
		if (rarities.get(rarity).startingSlotsBase != -1) {
			numSlots = rarities.get(rarity).startingSlotsBase;
		}
		int slotsRange = startingSlotsRange;
		if (rarities.get(rarity).startingSlotsRange != -1) {
			slotsRange = rarities.get(rarity).startingSlotsRange;
		}
		if (slotsRange > 0) {
			numSlots += Gear.gen.nextInt(slotsRange + 1);
		}
		
		if (level == 0) {
			level = 1;
		}
		// Lore part 1
		lore.add(translateHexCodes("&7Title: " + this.title));
		lore.add("§7Type: " + this.type);
		lore.add("§7Rarity: " + rarity.displayName);
		lore.add("§7Level: " + level);
		lore.add("§7Max Slots: " + maxSlots);
		for (String loreLine : this.lore) {
			lore.add(loreLine.replaceAll("&", "§"));
		}
		lore.add("§8§m-----");
		// Lore part 2
		for (String key : Gear.attributeOrder.keySet()) {
			double amount = 0;
			String line = null;
			if (attributes.containsKey(key)) {
				AttributeSet attr = attributes.get(key);
				amount += attr.generateAmount(level);
				String id = attr.getAttr();
				if (decimalAttrs.contains(id)) {
					double amt = amount;
					amt /= 10;
					line = attr.format(amt);
				}
				else {
					line = attr.format(amount);
				}
			}
			if (rarities.get(rarity).attributes.containsKey(key)) {
				AttributeSet attr = rarities.get(rarity).attributes.get(key);
				amount += attr.generateAmount(level);
				String id = attr.getAttr();
				if (decimalAttrs.contains(id)) {
					double amt = amount;
					amt /= 10;
					line = attr.format(amt);
				}
				else {
					line = attr.format(amount);
				}
			}
			
			if (amount > 0) {
				lore.add(line);
			}
		}
		// Lore part 3, only add separator if there was at least 1 attribute
		HashMap<String, Integer> nbtIntegers = new HashMap<String, Integer>();
		HashMap<String, String> nbtStrings = new HashMap<String, String>();
		if (lore.size() >= 6) { lore.add("§8§m-----"); }
		for (String augment : requiredAugments) {
			currentSlot++;
			Augment aug = AugmentManager.getFromCache(augment.toLowerCase(), level);
			lore.add(aug.getLine());
			nbtIntegers.put("slot" + currentSlot + "Line", lore.size() - 1);
			nbtIntegers.put("slot" + currentSlot + "Level", level);
			nbtStrings.put("slot" + currentSlot + "Augment", aug.getName());
		}
		for (int i = 0; i < numSlots; i++) {
			currentSlot++;
			lore.add("§8[Empty Slot]");
			nbtIntegers.put("slot" + currentSlot + "Line", lore.size() - 1);
		}
		
		int durability = duraBase + rarities.get(rarity).duraBonus;
		lore.add("§7Durability " + durability + " / " + durability);
		meta.setLore(lore);
		
		item.setItemMeta(meta);
		NBTItem nbti = new NBTItem(item);
		double price = this.configPrice == -1 ? calculatePrice(level, rarity, currentSlot) : this.configPrice * rarity.priceModifier;
		nbti.setDouble("value", price);
		nbti.setString("gear", id);
		nbti.setInteger("version", version);
		nbti.setInteger("slotsMax", Math.max(currentSlot, maxSlots));
		nbti.setInteger("level", level);
		nbti.setString("rarity", rarity.key);
		nbti.setInteger("slotsCreated", currentSlot);
		for (String key : nbtIntegers.keySet()) {
			nbti.setInteger(key, nbtIntegers.get(key));
		}
		for (String key : nbtStrings.keySet()) {
			nbti.setString(key, nbtStrings.get(key));
		}
		return nbti.getItem();
	}
	
	public String increaseRarity(Player p, ItemStack item) {
		NBTItem nbti = new NBTItem(item);
		if (!item.hasItemMeta()) {
			Bukkit.getLogger().log(Level.WARNING, "[NeoGear] Could not increase " + p.getName() + "'s item rarity, it has no meta.");
			return "item is not a quest item!";
		}
		if (!nbti.hasKey("version")) {
			Bukkit.getLogger().log(Level.WARNING, "[NeoGear] Could not increase " + p.getName() + "'s " + item.getItemMeta().getDisplayName() +
					" rarity, it has no version.");
			return "item is outdated! Try /prof convert?";
		}
		if (!nbti.hasKey("level")) {
			Bukkit.getLogger().log(Level.WARNING, "[NeoGear] Could not increase " + p.getName() + "'s " + item.getItemMeta().getDisplayName() +
					" rarity, it has no level.");
			return "item doesn't have level! Is it outdated?";
		}
		if (!nbti.hasKey("rarity")) {
			Bukkit.getLogger().log(Level.WARNING, "[NeoGear] Could not increase " + p.getName() + "'s " + item.getItemMeta().getDisplayName() +
					" rarity, it has no rarity.");
			return "item doesn't have rarity! Is it outdated?";
		}
		String oldRarity = nbti.getString("rarity");
		if (oldRarity.equalsIgnoreCase("legendary")) {
			Bukkit.getLogger().log(Level.WARNING, "[NeoGear] Could not increase " + p.getName() + "'s " + item.getItemMeta().getDisplayName() +
					" rarity, its rarity is already legendary.");
			return "cannot upgrade legendary! Must be artifacted at /warp artifactupgrade";
		}
		String newRarity = rarityUpgrades.get(oldRarity);
		Rarity r = Gear.getRarities().get(newRarity);
		String rarityDisplay = r.displayName;
		nbti.setString("rarity", newRarity);
		
		// Add slots if rarity increased to rare+
		String type = item.getType().name();
		boolean addSlot = !newRarity.equalsIgnoreCase("uncommon") && !type.endsWith("BOOTS") && !type.endsWith("HELMET");
		int slotsMaxOld = nbti.getInteger("slotsMax");
		int slotsMaxNew = slotsMaxOld + 1;
		if (addSlot) {
			nbti.setInteger("slotMax",  slotsMaxNew);
		}
		nbti.applyNBT(item);
		ItemMeta meta = item.getItemMeta();
		if (meta.getDisplayName().contains("Standard")) {
			meta.setDisplayName(r.colorCode + ChatColor.stripColor(meta.getDisplayName()));
		}
		List<String> lore = meta.getLore();
		lore.set(2, "§7Rarity: " + rarityDisplay);
		lore.set(4, "§7Max Slots: " + slotsMaxNew);
		meta.setLore(lore);
		item.setItemMeta(meta);
		p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
		p.sendMessage("§4[§c§lMLMC§4] §7" + item.getItemMeta().getDisplayName() + "§7's rarity has been increased to " + rarityDisplay + "§7!");
		updateStats(p, item, false);
		return null;
	}
	
	public String increaseLevel(Player p, ItemStack item, int increase) {
		NBTItem nbti = new NBTItem(item);
		if (!item.hasItemMeta()) {
			Bukkit.getLogger().log(Level.WARNING, "[NeoGear] Could not increase " + p.getName() + "'s item level, it has no meta.");
			return "item is not a quest item!";
		}
		if (!nbti.hasKey("version")) {
			Bukkit.getLogger().log(Level.WARNING, "[NeoGear] Could not increase " + p.getName() + "'s " + item.getItemMeta().getDisplayName() +
					" level, it has no version.");
			return "item is outdated! Try /prof convert?";
		}
		if (!nbti.hasKey("level")) {
			Bukkit.getLogger().log(Level.WARNING, "[NeoGear] Could not increase " + p.getName() + "'s " + item.getItemMeta().getDisplayName() +
					" level, it has no level.");
			return "item has no level! Seems outdated?";
		}
		int oldLevel = nbti.getInteger("level");
		int newLevel = oldLevel + increase;
		newLevel -= newLevel % 5;
		if (newLevel < 5 || newLevel > 60) {
			Bukkit.getLogger().log(Level.WARNING, "[NeoGear] Could not increase " + p.getName() + "'s " + item.getItemMeta().getDisplayName() +
					" level, its new level would be " + newLevel + ".");
			return "new level is beyond the game bounds!";
		}
		nbti.setInteger("level", newLevel);
		nbti.applyNBT(item);
		p.sendMessage("§4[§c§lMLMC§4] §7" + item.getItemMeta().getDisplayName() + "§7's level has been increased to §e" + newLevel + "§7!");
		p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
		updateStats(p, item, false);
		return null;
	}
	
	public void updateStats(Player p, ItemStack item, boolean announceChanges) {
		NBTItem nbti = new NBTItem(item);
		if (!nbti.hasKey("version")) {
			return;
		}
		Rarity rarity = Gear.getRarities().get(nbti.getString("rarity"));
		int level = nbti.getInteger("level");
		boolean hasChanged = false;
		
		// Update value if needed
		if (nbti.getDouble("value") != calculatePrice(item)) {
			nbti.setDouble("value", calculatePrice(item));
			hasChanged = true;
		}
		nbti.applyNBT(item);
		
		ItemMeta meta = item.getItemMeta();
		if (model != -1 && meta.getCustomModelData() < 10000) meta.setCustomModelData(model);
		List<String> lore = meta.getLore();
		ListIterator<String> loreIter = lore.listIterator();
		while (loreIter.hasNext()) {
			String line = loreIter.next();
			if (line.contains("---")) {
				break;
			}
			else if (line.contains("Level") && !line.endsWith("" + level)) {
				loreIter.remove();
				loreIter.add("§7Level: " + level);
			}
		}
		
		String line = loreIter.next();
		for (String key : Gear.attributeOrder.keySet()) {
			String format = Gear.attributeOrder.get(key);
			int formatIndex = format.indexOf('+');
			if (formatIndex == -1) {
				formatIndex = format.indexOf('-');
			}
			String attr = format.substring(0, formatIndex - 1);
			
			// Check an existing lore attribute to see if it's correct
			if (line.contains(attr)) {
				int index = line.indexOf('+');
				if (index == -1) {
					index = line.indexOf('-');
				}
				String num = line.substring(index);
                double dbleAmt = Double.parseDouble(num.replaceAll("[^0-9-.]", ""));
				if (decimalAttrs.contains(id)) {
					// Turn decimals to int
					dbleAmt *= 10;
				}
				int amt = (int) dbleAmt;
                
                AttributeSet aset = attributes.get(key);
                AttributeSet rset = rarities.get(rarity).attributes.get(key);
                
                int min = aset.getMinAmount(level);
                int max = min + aset.getRange();
                if (rset != null) {
                	min += rset.getMinAmount(level);
                    max = min + aset.getRange() + rset.getRange();
                }
                
                // Attribute has updated value
                if (amt < min || amt > max) {
                	hasChanged = true;
                	loreIter.remove();
                	// If max = 0, attribute was deleted entirely
                	if (max > 0) {
        				if (decimalAttrs.contains(id)) {
        					double amount = min + Gear.gen.nextInt(max - min + 1);
        					amount /= 10;
        					loreIter.add(aset.format(amount));
        				}
        				else {
                        	loreIter.add(aset.format(min + Gear.gen.nextInt(max - min + 1)));
        				}
                	}
                }
            	line = loreIter.next();
			}
			// If the attribute is missing, add it (in correct order)
			else {
                AttributeSet aset = attributes.get(key);
                AttributeSet rset = rarities.get(rarity).attributes.get(key);

                int min = aset.getMinAmount(level);
                int max = min + aset.getRange();
                if (rset != null) {
                	min += rset.getMinAmount(level);
                    max = min + aset.getRange() + rset.getRange();
                }
                
                // New attribute was added to gear
                if (max > 0) {
                	hasChanged = true;
                	loreIter.previous();
    				if (decimalAttrs.contains(id)) {
    					double amount = min + Gear.gen.nextInt(max - min + 1);
    					amount /= 10;
    					loreIter.add(aset.format(amount));
    				}
    				else {
                    	loreIter.add(aset.format(min + Gear.gen.nextInt(max - min + 1)));
    				}
                	loreIter.next();
                }
			}
		}
		
		// Update durability as necessary
		while (loreIter.hasNext()) {
			line = loreIter.next();
			if (line.contains("Durability")) {
				String args[] = line.substring(line.indexOf("y") + 2).split(" / ");
				double dura = Double.parseDouble(args[0]);
				double duraMax = Double.parseDouble(args[1]);
				double percentage = dura / duraMax;
				
				double newDuraMax = duraBase + rarities.get(rarity).duraBonus;
				double newDura = newDuraMax * percentage;
				loreIter.remove();
				loreIter.add(DURAPREFIX + (int) newDura + " / " + (int) newDuraMax);
			}
		}
		
		// Update enchantments if needed
		for (Enchant enchant : requiredEnchants) {
			int curr = meta.getEnchantLevel(enchant.enchantment);
			if (curr >= enchant.min && curr <= enchant.max) {
				int lv = enchant.min + Gear.gen.nextInt(enchant.max - enchant.min + 1);
				meta.addEnchant(enchant.enchantment, lv, true);
			}
		}
		
		meta.setLore(lore);
		item.setItemMeta(meta);
		if (hasChanged && announceChanges) {
			p.sendMessage("§4[§c§lMLMC§4] §7Your item's stats have been changed due to server balancing.");
		}
	}
	
	private String translateHexCodes(String textToTranslate) {
		Matcher matcher = HEX_PATTERN.matcher(textToTranslate);
		StringBuffer buffer = new StringBuffer();

		while (matcher.find()) {
			matcher.appendReplacement(buffer, ChatColor.of(matcher.group(1)).toString());
		}

		return ChatColor.translateAlternateColorCodes('&', matcher.appendTail(buffer).toString());
	}
	
	public ArrayList<String> getRequiredAugments() {
		return requiredAugments;
	}
}
