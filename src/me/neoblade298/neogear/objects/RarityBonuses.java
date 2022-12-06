package me.neoblade298.neogear.objects;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Material;

public class RarityBonuses {
	public HashMap<String, AttributeSet> attributes;
	public int duraBonus;
	public ArrayList<String> prefixes;
	public Material material;
	public int slotsMax;
	public int startingSlotsBase;
	public int startingSlotsRange;
	
	public RarityBonuses(HashMap<String, AttributeSet> attributes, int duraBonus, ArrayList<String> prefixes, String material,
			int slotsMax, int startingSlotsBase, int startingSlotsRange) {
		this.attributes = attributes;
		this.duraBonus = duraBonus;
		this.prefixes = prefixes;
		if (material == null) {
			this.material = null;
		}
		else {
			this.material = Material.getMaterial(material);
		}
		this.slotsMax = slotsMax;
		this.startingSlotsRange = startingSlotsRange;
		this.startingSlotsBase = startingSlotsBase;
	}
	
	public RarityBonuses() {
		this.attributes = new HashMap<String, AttributeSet>();
		this.duraBonus = 0;
		this.prefixes = null;
		this.material = null;
		this.slotsMax = -1;
		this.startingSlotsRange = -1;
		this.startingSlotsBase = -1;
	}
}
