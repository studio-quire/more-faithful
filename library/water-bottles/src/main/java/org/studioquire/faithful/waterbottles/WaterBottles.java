package org.studioquire.faithful.waterbottles;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.studioquire.oldfaithful.core.api.Content;
import org.studioquire.oldfaithful.core.api.FaithfulMod;
import org.studioquire.oldfaithful.core.api.ItemEntry;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.Recipes;

public final class WaterBottles implements FaithfulMod {
	public static ItemEntry<GlassBottle> glassBottle;
	public static ItemEntry<WaterBottle> waterBottle;

	@Override
	public void content(Content c) {
		// A bowl for both: an empty vessel, and unlike stew or a water bucket it hands a vanilla reader nothing it didn't have.
		glassBottle = c.item("glass_bottle", s -> new GlassBottle(s.id(), s.texture("glass_bottle"))).standIn(Item.BOWL);
		waterBottle = c.item("water_bottle", s -> new WaterBottle(s.id(), s.texture("water_bottle"))).standIn(Item.BOWL);
	}

	@Override
	public void init(Mod mod) {
		Recipes.shaped(new ItemStack(glassBottle.get(), 3), "G G", " G ", 'G', "c:glass");
	}
}
