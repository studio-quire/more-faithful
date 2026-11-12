package org.studioquire.faithful.pumpkins;

import net.minecraft.block.Block;
import net.minecraft.item.FoodItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.studioquire.oldfaithful.core.api.BoolRule;
import org.studioquire.oldfaithful.core.api.Content;
import org.studioquire.oldfaithful.core.api.FaithfulMod;
import org.studioquire.oldfaithful.core.api.ItemEntry;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.Recipes;

/** Pumpkin pie, and pumpkins that spread on grass while the world's rule says so ({@link Spreading}). */
public final class Pumpkins implements FaithfulMod {
	/** Half-hearts, a cooked porkchop's. With Hearty Food it's that mod's to scale, as for any food. */
	public static final int PIE_HEALS = 8;

	public static ItemEntry<Item> pie;
	/** Off by default, so a world from before the mod plays as it did; a pack starts its new worlds with it on (the pack's {@code starting-rules.properties}). */
	public static BoolRule spreading;

	@Override
	public void content(Content c) {
		// A plain food, one to a slot like beta's others. What a vanilla reader sees is the pumpkin it was made from.
		pie = c.item("pumpkin_pie", s -> new FoodItem(s.id(), PIE_HEALS, false).setTextureId(s.texture("pumpkin_pie"))).standIn(Item.ITEMS[Block.PUMPKIN.id]);
	}

	@Override
	public void init(Mod mod) {
		Recipes.shapeless(new ItemStack(pie.get()), Block.PUMPKIN, Item.SUGAR, Item.EGG);
		spreading = mod.rules().bool("spreading", false);
	}
}
