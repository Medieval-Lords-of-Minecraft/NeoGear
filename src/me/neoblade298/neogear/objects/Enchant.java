package me.neoblade298.neogear.objects;

import org.bukkit.enchantments.Enchantment;

public class Enchant {
	public Enchantment enchantment;
	public int min;
	public int max;
	
	public Enchant(Enchantment enchantment, int min, int max) {
		this.enchantment = enchantment;
		this.min = min;
		this.max = max;
	}
}
