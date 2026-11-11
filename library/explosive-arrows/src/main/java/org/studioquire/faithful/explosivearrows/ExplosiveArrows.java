package org.studioquire.faithful.explosivearrows;

import net.minecraft.block.TntBlock;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.studioquire.oldfaithful.core.api.Arrows;
import org.studioquire.oldfaithful.core.api.Channel;
import org.studioquire.oldfaithful.core.api.Content;
import org.studioquire.oldfaithful.core.api.Explosions;
import org.studioquire.oldfaithful.core.api.FaithfulMod;
import org.studioquire.oldfaithful.core.api.ItemEntry;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.Recipes;

/** Gunpowder on an arrow: a small blast of beta's own where it lands, which breaks no blocks but lights TNT. The arrow is {@link ExplosiveArrow}. */
public final class ExplosiveArrows implements FaithfulMod {
	public static ItemEntry<Item> explosiveArrow;

	/**
	 * How far off a burst's puffs are drawn, where beta draws any particle only within 16 blocks of the camera and so showed a burst at bow range not at all:
	 * as far as beta's server tells a client of a blast, and about as far as its bang is heard (16 blocks a unit of a sound's volume, 4 for a blast's).
	 */
	public static final double SEEN = 64.0;

	/** A server's burst, for its clients to draw: beta's own packet for a blast, which this takes the place of, would be drawn as any blast's. */
	public static Channel bursts;

	/** On a client: draws one of a burst's puffs that beta would leave out for distance, and says whether it did. Null where nothing's drawn. */
	public static Puffs puffs;

	/** On a client: a burst from {@link #bursts} is being drawn. */
	public static boolean drawing;

	public interface Puffs {
		boolean far(World world, String name, double x, double y, double z, double velocityX, double velocityY, double velocityZ);
	}

	@Override
	public void content(Content c) {
		// Without the mod, an arrow is what's left once the gunpowder's gone.
		explosiveArrow = c.item("explosive_arrow", s -> new Item(s.id()) {}.setTextureId(s.texture("explosive_arrow"))).standIn(Item.ARROW);
		// As beta tracks its arrows: a client flies one itself from where and how fast it set off.
		c.entity("explosive_arrow", ExplosiveArrow.class).tracking(64, 20);
	}

	@Override
	public void init(Mod mod) {
		// Two, so a creeper's drop is about one arrow's worth (the user, 2026-11-09): a consumable weapon, not an everyday arrow.
		Recipes.shapeless(new ItemStack(explosiveArrow.get()), Item.ARROW, Item.GUNPOWDER, Item.GUNPOWDER);
		Explosions.onBlocks(e -> {
			if (e.source() instanceof ExplosiveArrow) e.spareBlocks(block -> block instanceof TntBlock);
		});
		bursts = mod.channel("burst", 1);
		// What this arrow flies as, for shooters that ask the core (a golem's bow). Beta's bow and dispenser don't ask: the mixins here patch them.
		Arrows.flies(explosiveArrow.get(), ExplosiveArrow::new);
	}

	/**
	 * A bow's draw: if the first arrows in the inventory, in beta's own order (the hotbar from the left, then the rows from the top), are explosive, one of
	 * them is taken and this says so. Plain ones first are left to beta's bow, which takes from that same stack.
	 */
	public static boolean takeExplosive(PlayerInventory inventory) {
		int explosive = explosiveArrow.get().id;
		ItemStack[] main = inventory.main;
		for (int i = 0; i < main.length; i++) {
			ItemStack stack = main[i];
			if (stack == null) continue;
			if (stack.itemId == Item.ARROW.id) return false;
			if (stack.itemId == explosive) {
				if (--stack.count <= 0) main[i] = null;
				return true;
			}
		}
		return false;
	}
}
