package org.studioquire.faithful.alchemy;

import net.minecraft.entity.player.PlayerEntity;
import org.studioquire.oldfaithful.core.api.Attachment;
import org.studioquire.oldfaithful.core.api.Codec;
import org.studioquire.oldfaithful.core.api.Mod;

/**
 * The Ward Potion: a set of armour in a bottle. Drinking one fills the whole armour bar with blue, as putting on a whole set fills it; hits drain the blue as
 * beta's bar drops while a set wears out; it's worn over the player's own armour, which takes no wear until the blue is gone and then shows again as it was. A potion's size is how tough the set
 * is, as leather and diamond fill the same bar and differ in how long they last.
 * <p>
 * Beta's armour: a set's protection is {@code (points - 1) * left / whole + 1} of the bar's twenty, where {@code left} and {@code whole} are its durability, and a hit takes {@code protection / 25}
 * of its damage; every piece worn loses the hit's whole damage. Here the blue is one set of twenty points with {@link #wear} left of {@link #whole}: a hit's damage comes off it first, and what it
 * can't take goes on to the armour. The blue and the armour's points add up to the bar's twenty at most, so over a full bar the blue adds no protection and spares the armour. No timer: hits wear it
 * away. Gone at death, kept over a relog, as worn armour is.
 */
public final class Ward {
	/** The armour bar's points: ten pips, a whole set's. */
	public static final int BAR = 20;

	/** Damage the blue can still take; {@link #whole}, what it could when drunk. Both reach the player's client for the bar. */
	public static Attachment<PlayerEntity, Integer> wear, whole;

	private Ward() {
	}

	static void init(Mod mod) {
		wear = mod.attachment("ward", Attachment.player(Codec.INT)).persist().sync(Attachment.Sync.SELF).defaultValue(() -> 0);
		whole = mod.attachment("ward_whole", Attachment.player(Codec.INT)).persist().sync(Attachment.Sync.SELF).defaultValue(() -> 0);
	}

	/** The toughest set: the Strong Ward Potion's and the golden apple's, and the most drinking more can reach. */
	public static int toughest() {
		return Alchemy.WARD[2];
	}

	/** The bar's points a set with {@code left} of {@code whole} gives: beta's own sum for a worn set, all twenty when new and one at the last. */
	public static int points(int left, int whole) {
		if (left <= 0) return 0;
		return Math.min(BAR, (BAR - 1) * left / Math.max(whole, left) + 1);
	}

	/** The blue's points, on either side: a client hears both numbers, one packet each, so for a tick it may have one new and one old, which this rides out. */
	public static int points(PlayerEntity player) {
		return wear == null ? 0 : points(wear.get(player), whole.get(player));
	}

	/** Whether there's a whole set of the toughest on, so another potion would do nothing. */
	public static boolean full(PlayerEntity player) {
		return wear.get(player) >= toughest();
	}

	/** What a hit counts as the player's armour: the worn armour's {@code armour} points and the blue, up to the bar's twenty. */
	public static int protection(PlayerEntity player, int armour) {
		return Math.min(BAR, armour + points(player));
	}

	/** Where the world runs: a hit's wear of {@code amount}, which the blue takes first. Returns what's left for each piece of worn armour. */
	public static int absorb(PlayerEntity player, int amount) {
		int had = wear.get(player);
		if (had <= 0 || amount <= 0) return amount;
		int taken = Math.min(had, amount);
		if (taken < had) wear.set(player, had - taken);
		else clear(player);
		return amount - taken;
	}

	/**
	 * Where the world runs: a set of {@code toughness} drunk. What's left of the blue adds to it, up to the toughest, and it goes on whole, so the bar is all blue again whatever was there; as with
	 * every potion, more is wasted past the most there can be.
	 */
	public static void put(PlayerEntity player, int toughness) {
		int set = Math.min(toughest(), wear.get(player) + toughness);
		wear.set(player, set);
		whole.set(player, set);
	}

	/** Where the world runs: the blue gone. */
	public static void clear(PlayerEntity player) {
		wear.set(player, null);
		whole.set(player, null);
	}
}
