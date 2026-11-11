package org.studioquire.faithful.featherlure;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.studioquire.oldfaithful.core.api.Attachment;
import org.studioquire.oldfaithful.core.api.Channel;
import org.studioquire.oldfaithful.core.api.Codec;
import org.studioquire.oldfaithful.core.api.Content;
import org.studioquire.oldfaithful.core.api.FaithfulMod;
import org.studioquire.oldfaithful.core.api.ItemEntry;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.Recipes;

/**
 * A feather tied on a fishing rod's line: fish bite twice as often, and now and then one takes the feather. The rod is {@link LuredFishingRod}; its bobber is beta's, which
 * knows it's on a lured line by the rod in its owner's hand ({@code mixin/FishingBobberEntityMixin}).
 */
public final class FeatherLure implements FaithfulMod {
	/** Beta's bobber rolls for a bite each tick, 1 in 500, or 1 in 300 with rain falling on it; on a lured line, against half of either. */
	public static final int BITE_DIVISOR = 2;
	/** Each fish caught takes the feather one time in this many. */
	public static final int LOSE_ONE_IN = 8;

	public static ItemEntry<LuredFishingRod> luredRod;
	/** Where the world runs, to the player whose lure a fish took: where the bobber was, for the feather's bits. */
	public static Channel lost;
	/**
	 * On a bobber whose line is lured, so it's drawn with the feather: kept where the world runs by the same test as the bite's roll, and sent to everyone who can see the
	 * bobber, as beta never tells a client whose a bobber is. Not saved: beta doesn't save a bobber.
	 */
	public static Attachment<Entity, Boolean> luredLine;

	@Override
	public void content(Content c) {
		// Without the mod it's a fishing rod, the feather gone (and new, as any worn item of a mod's reads to a vanilla reader).
		luredRod = c.item("lured_fishing_rod", s -> new LuredFishingRod(s.id(), s.texture("lured_fishing_rod"))).standIn(Item.FISHING_ROD);
	}

	@Override
	public void init(Mod mod) {
		Recipes.shapeless(new ItemStack(luredRod.get()), FeatherLure::lure, new ItemStack(Item.FISHING_ROD, 1, -1), Item.FEATHER);
		lost = mod.channel("lost", 1);
		luredLine = mod.attachment("lured_line", Attachment.entity(Codec.BOOL)).sync(Attachment.Sync.TRACKING);
	}

	/** The rod's wear comes along. */
	private static ItemStack lure(List<ItemStack> inputs) {
		for (ItemStack stack : inputs) {
			if (stack.itemId == Item.FISHING_ROD.id) return new ItemStack(luredRod.get(), 1, stack.getDamage());
		}
		return null;
	}

	/** Whether a bobber of this player's is on a lured line: the rod in their hand, which beta's bobber checks every tick, going when it isn't a fishing rod. */
	public static boolean lured(PlayerEntity owner) {
		if (owner == null) return false;
		ItemStack hand = owner.getHand();
		return hand != null && hand.itemId == luredRod.get().id;
	}

	/** Where the world runs, each tick of a bobber: whether its line is lured now. Only a change goes to clients. */
	public static void markLine(Entity bobber, boolean lured) {
		if (luredLine.has(bobber) != lured) luredLine.set(bobber, lured ? Boolean.TRUE : null);
	}

	/** Whether a bobber is drawn with the feather: in single player the world's own word, on a server the server's, and never from a server without the mod. */
	public static boolean looksLured(Entity bobber) {
		return luredLine.has(bobber);
	}

	/** Where the world runs: the feather came off where the bobber was, which the player sees as a few bits of it. */
	public static void lose(PlayerEntity player, double x, double y, double z) {
		if (!lost.available(player)) return;
		lost.toPlayer(player, out -> {
			out.writeDouble(x);
			out.writeDouble(y);
			out.writeDouble(z);
		});
	}
}
