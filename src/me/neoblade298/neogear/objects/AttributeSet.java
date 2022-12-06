package me.neoblade298.neogear.objects;

import java.text.DecimalFormat;

import me.neoblade298.neogear.Gear;

public class AttributeSet {
	private static DecimalFormat df = new DecimalFormat("##.#");
	private String attr;
	private String format;
	private int base;
	private int scale;
	private int range;
	private int rounded;
	
	public AttributeSet (String attr, String format, int base, int scale, int range, int rounded) {
		this.attr = attr;
		this.format = format;
		this.base = base;
		this.scale = scale;
		this.range = range;
		this.rounded = rounded;
	}

	public String getAttr() {
		return attr;
	}

	public void setAttr(String attr) {
		this.attr = attr;
	}

	public int getBase() {
		return base;
	}

	public void setBase(int base) {
		this.base = base;
	}

	public int getScale() {
		return scale;
	}

	public void setScale(int scale) {
		this.scale = scale;
	}

	public int getRange() {
		return range;
	}

	public void setRange(int range) {
		this.range = range;
	}

	public int getRounded() {
		return rounded;
	}

	public void setRounded(int rounded) {
		this.rounded = rounded;
	}
	
	public String format(double amount) {
		return "§9" + this.format.replaceAll("\\$amt\\$", df.format(amount));
	}
	
	public int generateAmount(int level) {
		return this.getBase() + (this.getScale() * (level / Gear.lvlInterval)) + Gear.gen.nextInt(this.getRange() + 1);
	}
	
	public int getMinAmount(int level) {
		return this.getBase() + (this.getScale() * (level / Gear.lvlInterval));
	}
}
