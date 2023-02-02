package me.neoblade298.neogear;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import de.tr7zw.nbtapi.NBTItem;
import me.Neoblade298.NeoProfessions.Utilities.Util;
import me.neoblade298.neogear.listeners.DurabilityListener;
import me.neoblade298.neogear.objects.AttributeSet;
import me.neoblade298.neogear.objects.Enchant;
import me.neoblade298.neogear.objects.GearConfig;
import me.neoblade298.neogear.objects.ItemSet;
import me.neoblade298.neogear.objects.Rarity;
import me.neoblade298.neogear.objects.RarityBonuses;
import net.milkbowl.vault.economy.Economy;

public class Gear extends JavaPlugin implements org.bukkit.event.Listener {
	static HashMap<String, HashMap<Integer, GearConfig>> settings;
	public static LinkedHashMap<String, String> attributeOrder = new LinkedHashMap<String, String>();
	private YamlConfiguration cfg;
	public static int lvlMax;
	public static int lvlInterval;
	private static HashMap<String, Rarity> rarities; // Color codes within
	HashMap<String, ArrayList<String>> raritySets;
	HashMap<String, ItemSet> itemSets;
	private HashMap<String, String> typeConverter;
	public static Random gen = new Random();
	private static Economy econ = null;

	static {
		attributeOrder.put("str", "Strength +$amt$");
		attributeOrder.put("dex", "Dexterity +$amt$");
		attributeOrder.put("int", "Intelligence +$amt$");
		attributeOrder.put("spr", "Spirit +$amt$");
		attributeOrder.put("end", "Endurance +$amt$");
		attributeOrder.put("mhp", "Max HP +$amt$");
		attributeOrder.put("mmp", "Max MP +$amt$");
		attributeOrder.put("hrg", "Health Regen +$amt$");
		attributeOrder.put("rrg", "Resource Regen +$amt$%");
		attributeOrder.put("hlr", "Healing Received +$amt$%");
	}

	public void onEnable() {
		Bukkit.getServer().getLogger().info("NeoGear Enabled");
		getServer().getPluginManager().registerEvents(this, this);
		this.getCommand("gear").setExecutor(new Commands(this));

		if (!setupEconomy()) {
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		Bukkit.getPluginManager().registerEvents(new DurabilityListener(this), this);
		typeConverter = new HashMap<String, String>();
		typeConverter.put("reinforced helmet", "rhelmet");
		typeConverter.put("reinforced chestplate", "rchestplate");
		typeConverter.put("reinforced leggings", "rleggings");
		typeConverter.put("reinforced boots", "rboots");
		typeConverter.put("infused helmet", "ihelmet");
		typeConverter.put("infused chestplate", "ichestplate");
		typeConverter.put("infused leggings", "ileggings");
		typeConverter.put("infused boots", "iboots");

		loadConfigs();
	}

	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}

	public void onDisable() {
		org.bukkit.Bukkit.getServer().getLogger().info("NeoGear Disabled");
		super.onDisable();
	}

	public void loadConfigs() {
		File cfg = new File(getDataFolder(), "config.yml");
		File gearFolder = new File(getDataFolder().getPath() + "/gear");

		// Save config if doesn't exist
		if (!cfg.exists()) {
			saveResource("config.yml", false);
		}
		this.cfg = YamlConfiguration.loadConfiguration(cfg);

		// Load config
		Gear.lvlInterval = this.cfg.getInt("lvl-interval");
		Gear.lvlMax = this.cfg.getInt("lvl-max");

		// Rarities and color codes
		rarities = new HashMap<String, Rarity>();
		ConfigurationSection raritySec = this.cfg.getConfigurationSection("rarities");
		for (String rarity : raritySec.getKeys(false)) {
			ConfigurationSection specificRarity = raritySec.getConfigurationSection(rarity);
			Rarity rarityObj = new Rarity(rarity, specificRarity.getString("color-code"),
					specificRarity.getString("display-name"), specificRarity.getDouble("price-modifier"),
					specificRarity.getBoolean("is-enchanted"), specificRarity.getInt("priority"));
			rarities.put(rarity, rarityObj);
		}

		// Rarity sets
		this.raritySets = new HashMap<String, ArrayList<String>>();
		ConfigurationSection rareSets = this.cfg.getConfigurationSection("rarity-sets");
		for (String set : rareSets.getKeys(false)) {
			this.raritySets.put(set, (ArrayList<String>) rareSets.getStringList(set));
		}

		// Item sets
		this.itemSets = new HashMap<String, ItemSet>();
		ConfigurationSection itemSets = this.cfg.getConfigurationSection("item-sets");
		for (String set : itemSets.getKeys(false)) {
			ArrayList<String> setContents = (ArrayList<String>) itemSets.getStringList(set);
			ItemSet itemset = new ItemSet(this, setContents);
			this.itemSets.put(set, itemset);
		}

		// Set up gear folder
		if (!gearFolder.exists()) {
			gearFolder.mkdir();
		}

		// Load in all gear files
		Gear.settings = new HashMap<String, HashMap<Integer, GearConfig>>();
		loadGearDirectory(gearFolder);
	}

	private void loadGearDirectory(File dir) {
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				loadGearDirectory(file);
			}
			else {
				YamlConfiguration gearCfg = YamlConfiguration.loadConfiguration(file);
				String id = gearCfg.getString("id");
				String type = gearCfg.getString("type");
				String title = gearCfg.getString("title");
				int model = gearCfg.getInt("model", -1);
				Material material = Material.getMaterial(gearCfg.getString("material").toUpperCase());
				double price = gearCfg.getDouble("price", -1);
				int version = gearCfg.getInt("version");

				ConfigurationSection nameSec = gearCfg.getConfigurationSection("display-name");
				ArrayList<String> prefixes = (ArrayList<String>) nameSec.getStringList("prefix");
				ArrayList<String> displayNames = (ArrayList<String>) nameSec.getStringList("name");

				ConfigurationSection duraSec = gearCfg.getConfigurationSection("durability");
				int duraMinBase = duraSec.getInt("base");

				// Parse enchantments
				ConfigurationSection enchSec = gearCfg.getConfigurationSection("enchantments");
				ArrayList<Enchant> reqEnchList = parseEnchantments(
						(ArrayList<String>) enchSec.getStringList("required"));
				ArrayList<Enchant> optEnchList = parseEnchantments(
						(ArrayList<String>) enchSec.getStringList("optional"));
				int enchMin = enchSec.getInt("optional-min");
				int enchMax = enchSec.getInt("optional-max");

				HashMap<String, AttributeSet> attributes = parseAttributes(
						gearCfg.getConfigurationSection("attributes"));

				// Augments
				ConfigurationSection augSec = gearCfg.getConfigurationSection("augments");
				ArrayList<String> reqAugmentList = (ArrayList<String>) augSec.getStringList("required");

				ConfigurationSection rareSec = gearCfg.getConfigurationSection("rarity");
				HashMap<Rarity, RarityBonuses> rarities = new HashMap<Rarity, RarityBonuses>();
				// Load in rarities
				for (String rarity : Gear.rarities.keySet()) {
					ConfigurationSection specificRareSec = null;
					if (rareSec != null) {
						specificRareSec = rareSec.getConfigurationSection(rarity);
					}
					if (specificRareSec != null) {
						rarities.put(Gear.rarities.get(rarity),
								new RarityBonuses(parseAttributes(specificRareSec),
										specificRareSec.getInt("added-durability"),
										(ArrayList<String>) specificRareSec.getStringList("prefix"),
										specificRareSec.getString("material"), specificRareSec.getInt("slots-max"),
										specificRareSec.getInt("starting-slots-base"),
										specificRareSec.getInt("starting-slots-range")));
					}
					else {
						rarities.put(Gear.rarities.get(rarity), new RarityBonuses());
					}
				}

				// Slots
				int slotsMax = gearCfg.getInt("slots-max");
				int startingSlotsBase = gearCfg.getInt("starting-slots-base");
				int startingSlotsRange = gearCfg.getInt("starting-slots-range");

				// Lore
				ArrayList<String> lore = (ArrayList<String>) gearCfg.getStringList("lore");

				ConfigurationSection overrideSec = gearCfg.getConfigurationSection("lvl-overrides");
				HashMap<Integer, GearConfig> gearLvli = new HashMap<Integer, GearConfig>();
				for (int i = 0; i <= Gear.lvlMax; i += Gear.lvlInterval) {
					GearConfig gearConf = new GearConfig(id, type, title, model, material, prefixes, displayNames,
							duraMinBase, reqEnchList, optEnchList, reqAugmentList, enchMin, enchMax, attributes,
							rarities, slotsMax, startingSlotsBase, startingSlotsRange, price, version, lore);

					if (overrideSec != null) {
						// Level override
						ConfigurationSection lvlOverride = overrideSec.getConfigurationSection(i + "");
						if (lvlOverride != null) {
							overrideLevel(i, gearConf, lvlOverride);
						}
					}
					gearLvli.put(i, gearConf);
				}
				if (settings.containsKey(id)) {
					Bukkit.getLogger().log(Level.WARNING, "[NeoGear] Failed to load file " + file.getName() + ", gear id already exists: " + id);
				}
				else {
					settings.put(id, gearLvli);
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	private ArrayList<Enchant> parseEnchantments(ArrayList<String> enchList) {
		ArrayList<Enchant> enchantments = new ArrayList<Enchant>();
		for (String ench : enchList) {
			String[] enchParams = ench.split(":");
			enchantments.add(new Enchant(Enchantment.getByName(enchParams[0]), Integer.parseInt(enchParams[1]),
					Integer.parseInt(enchParams[2])));
		}
		return enchantments;
	}

	private HashMap<String, AttributeSet> parseAttributes(ConfigurationSection sec) {
		HashMap<String, AttributeSet> attrs = new HashMap<String, AttributeSet>(attributeOrder.size());
		for (String key : Gear.attributeOrder.keySet()) {
			int base = sec.getInt(key + "-base", 0);
			int scale = sec.getInt(key + "-per-lvl", 0);
			int range = sec.getInt(key + "-range", 0);
			int rounded = sec.getInt(key + "-rounded", 0);
			attrs.put(key, new AttributeSet(key, Gear.attributeOrder.get(key), base, scale, range, rounded));
		}

		return attrs;
	}

	private HashMap<String, AttributeSet> overrideAttributes(HashMap<String, AttributeSet> current,
			ConfigurationSection sec) {
		HashMap<String, AttributeSet> attrs = new HashMap<String, AttributeSet>(attributeOrder.size());
		for (String key : Gear.attributeOrder.keySet()) {
			int base = sec.getInt(key + "-base", current.get(key).getBase());
			int scale = sec.getInt(key + "-per-lvl", current.get(key).getScale());
			int range = sec.getInt(key + "-range", current.get(key).getRange());
			int rounded = sec.getInt(key + "-rounded", current.get(key).getRounded());
			attrs.put(key, new AttributeSet(key, Gear.attributeOrder.get(key), base, scale, range, rounded));
		}
		return attrs;
	}

	private RarityBonuses overrideRarities(RarityBonuses current, ConfigurationSection sec) {
		HashMap<String, AttributeSet> currAttr = current.attributes;
		HashMap<String, AttributeSet> newAttr = overrideAttributes(currAttr, sec);
		int addedDura = sec.getInt("added-durability", -1) != -1 ? sec.getInt("added-durability", -1)
				: current.duraBonus;
		ArrayList<String> currPrefixes = current.prefixes;
		ArrayList<String> newPrefixes = (ArrayList<String>) sec.getStringList("prefix");
		ArrayList<String> changedPrefixes = currPrefixes;
		if (newPrefixes != null) {
			changedPrefixes = currPrefixes.equals(newPrefixes) ? currPrefixes : newPrefixes;
		}
		String currMaterial = current.material.toString();
		if (sec.getString("material") != null) {
			currMaterial = sec.getString("material");
		}
		int changedSlotsMax = current.slotsMax;
		if (sec.getInt("slots-max", -1) != -1) {
			changedSlotsMax = sec.getInt("slots-max");
		}
		int changedStartingSlotsBase = current.startingSlotsBase;
		if (sec.getInt("starting-slots-base", -1) != -1) {
			changedSlotsMax = sec.getInt("starting-slots-base");
		}
		int changedStartingSlotsRange = current.startingSlotsRange;
		if (sec.getInt("starting-slots-range", -1) != -1) {
			changedSlotsMax = sec.getInt("starting-slots-range");
		}
		return new RarityBonuses(newAttr, addedDura, changedPrefixes, currMaterial, changedSlotsMax,
				changedStartingSlotsBase, changedStartingSlotsRange);
	}

	private void overrideLevel(int level, GearConfig conf, ConfigurationSection sec) {
		Material material = Material.getMaterial(sec.getString("material", "STONE").toUpperCase());
		if (material != null && !material.equals(Material.STONE)) {
			conf.material = material;
		}

		double price = sec.getDouble("price", -2);
		if (price != -2) {
			conf.configPrice = price;
		}

		ConfigurationSection nameSec = sec.getConfigurationSection("display-name");
		if (nameSec != null) {
			ArrayList<String> prefixes = (ArrayList<String>) nameSec.getStringList("prefix");
			ArrayList<String> displayNames = (ArrayList<String>) nameSec.getStringList("name");
			if (!prefixes.isEmpty()) {
				conf.prefixes = prefixes;
			}
			if (!displayNames.isEmpty()) {
				conf.displayNames = displayNames;
			}
		}

		ConfigurationSection duraSec = sec.getConfigurationSection("durability");
		if (duraSec != null) {
			int duraBase = duraSec.getInt("base", -1);
			if (duraBase != -1) {
				conf.duraBase = duraBase;
			}
		}

		// Parse enchantments
		ConfigurationSection enchSec = sec.getConfigurationSection("enchantments");
		if (enchSec != null) {
			ArrayList<Enchant> reqEnchList = parseEnchantments((ArrayList<String>) enchSec.getStringList("required"));
			ArrayList<Enchant> optEnchList = parseEnchantments((ArrayList<String>) enchSec.getStringList("optional"));
			int enchMin = enchSec.getInt("optional-min", -1);
			int enchMax = enchSec.getInt("optional-max", -1);
			if (!reqEnchList.isEmpty()) {
				conf.requiredEnchants = reqEnchList;
			}
			if (!optEnchList.isEmpty()) {
				conf.optionalEnchants = optEnchList;
			}
			if (enchMin != -1) {
				conf.enchantmentMin = enchMin;
			}
			if (enchMax != -1) {
				conf.enchantmentMax = enchMax;
			}
		}

		// override augments
		ConfigurationSection augSec = sec.getConfigurationSection("augments");
		if (augSec != null) {
			conf.requiredAugments = (ArrayList<String>) augSec.getStringList("required");
		}

		ConfigurationSection attrSec = sec.getConfigurationSection("attributes");
		if (attrSec != null) {
			conf.attributes = overrideAttributes(conf.attributes, attrSec);
		}

		ConfigurationSection raresSec = sec.getConfigurationSection("rarity");
		// Load in rarities
		if (raresSec != null) {
			for (String rarity : rarities.keySet()) {
				ConfigurationSection raritySec = raresSec.getConfigurationSection(rarity);
				if (raritySec != null) {
					conf.rarities.put(Gear.rarities.get(rarity), overrideRarities(conf.rarities.get(Gear.rarities.get(rarity)), raritySec));
				}
			}
		}
	}
	
		@EventHandler(priority = EventPriority.HIGHEST)
	public void onEnchantItem(EnchantItemEvent e) {
		ItemStack item = e.getItem();
		if (item != null) {
			if (isQuestGear(item)) {
				e.setCancelled(true);
				Util.sendMessage(e.getEnchanter(), "&cYou cannot enchant RPG items!");
				return;
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPrepareAnvilEvent(PrepareAnvilEvent e) {
		ItemStack[] contents = e.getInventory().getContents();
		for (int i = 0; i < contents.length; i++) {
			ItemStack item = contents[i];
			if (item != null) {
				if (isQuestGear(item)) {
					e.setResult(null);
					return;
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInteract(PlayerInteractEvent e) {
		Player p = e.getPlayer();
		ItemStack item = e.getItem();
		if (item == null || item.getType().isAir())
			return;
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (isArmor(item)) {
                e.setUseItemInHand(Result.DENY);
                p.sendMessage("§c[§4§lMLMC§4] §cEquipping armor via right click is disabled in quest worlds!");
            }
        }
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent e) {
		// Disable netheriting quest gear
		if (e.getView().getTopInventory().getType().equals(InventoryType.SMITHING)) {
			SmithingInventory smith = (SmithingInventory) e.getView().getTopInventory();
			if (isQuestGear(smith.getContents()[0]) && e.getSlot() == 2) {
				e.setCancelled(true);
				e.getView().getPlayer().sendMessage("§c[§4§lMLMC§4] §cYou cannot apply netherite to quest gear!");
			}
		}
	}

	public boolean isQuestGear(ItemStack item) {
		if (item == null || !item.hasItemMeta()) {
			return false;
		}
		if (new NBTItem(item).hasKey("gear")) {
			return true;
		}
		return item.getItemMeta().hasLore()
				&& item.getItemMeta().getLore().get(0).contains("Tier") && !item.getType().equals(Material.PLAYER_HEAD);
	}

	public boolean isArmor(ItemStack item) {
		if (item == null)
			return false;
		String mat = item.getType().name();
		return mat.contains("HELMET") || mat.contains("CHESTPLATE") || mat.contains("LEGGINGS")
				|| mat.contains("BOOTS");
	}

	@EventHandler
	public void onTridentThrow(ProjectileLaunchEvent e) {
		Projectile proj = e.getEntity();
		if (proj.getShooter() instanceof Player) {
			Player p = (Player) proj.getShooter();
			String world = e.getEntity().getLocation().getWorld().getName();
			if (world.equals("Argyll") || world.equals("ClassPVP") || world.equals("Dev")) {
				if (e.getEntity().getType().equals(EntityType.TRIDENT)) {
					e.setCancelled(true);
					p.sendMessage("§4[§c§lMLMC§4] §cYou cannot throw tridents in this world!");
				}
			}
		}
	}

	public Economy getEcon() {
		return econ;
	}

	public HashMap<String, HashMap<Integer, GearConfig>> getSettings() {
		return settings;
	}
	
	public static GearConfig getGearConfig(String type, int level) {
		return settings.get(type).get(level);
	}
	
	public static HashMap<String, Rarity> getRarities() {
		return rarities;
	}
}
