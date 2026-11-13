package org.studioquire.faithful.alchemy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.minecraft.entity.player.PlayerEntity;
import org.studioquire.oldfaithful.core.api.Attachment;
import org.studioquire.oldfaithful.core.api.Codec;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.Server;

/**
 * An effect that runs for a time: Haste or Feather, and any timed potion to come. Where the world runs a player has so many ticks {@link #left}, counted down in their own ticks in the world, so
 * not while they're away or the game is paused, and kept over a relog; gone at death, as the end of a life ends what was drunk in it. A second drink adds its time, up to the longest potion's.
 * <p>
 * A client hears {@link #shown}, the rows of liquid left in its bottle in the inventory, two minutes a row, which is enough for its own walking and mining too; and {@link #order}, where its bottle
 * stands in the row: they stand in the order they were drunk, a top-up keeping its place.
 */
public final class Timed {
	public static final int TICKS_A_MINUTE = 1200;
	/** The bottle's rows of liquid. */
	public static final int LEVELS = 6;
	/** The longest potion's time, and as long as it can run. */
	public static final int MOST = Alchemy.MINUTES[2] * TICKS_A_MINUTE;
	/** Ticks to a row of the bottle: two minutes. */
	public static final int STEP = MOST / LEVELS;

	private static final List<Timed> ALL = new ArrayList<>();

	/** Its row in {@code textures/gui/timers.png}: the order they were made in. */
	public final int index;
	public final Attachment<PlayerEntity, Integer> left, shown, order;

	Timed(Mod mod, String name) {
		index = ALL.size();
		ALL.add(this);
		left = mod.attachment(name, Attachment.player(Codec.INT)).persist().defaultValue(() -> 0);
		shown = mod.attachment(name + "_shown", Attachment.player(Codec.INT)).sync(Attachment.Sync.SELF).defaultValue(() -> 0);
		order = mod.attachment(name + "_order", Attachment.player(Codec.INT)).persist().sync(Attachment.Sync.SELF).defaultValue(() -> 0);
	}

	public static List<Timed> all() {
		return Collections.unmodifiableList(ALL);
	}

	/** The ones running for this player, in the order they were drunk; on either side. */
	public static List<Timed> running(PlayerEntity player, List<Timed> into) {
		into.clear();
		for (Timed t : ALL) if (t.running(player)) into.add(t);
		into.sort(Comparator.comparingInt((Timed t) -> t.order.get(player)).thenComparingInt(t -> t.index));
		return into;
	}

	/** Rows of liquid for so many ticks: the last stays until the last tick, as air's last bubble does. */
	public static int steps(int ticks) {
		return (ticks + STEP - 1) / STEP;
	}

	/** On either side. */
	public boolean running(PlayerEntity player) {
		return shown.get(player) > 0;
	}

	/** Its bottle full, as long as it can run to the row: another potion would add nothing it shows. */
	public boolean full(PlayerEntity player) {
		return steps(left.get(player)) >= LEVELS;
	}

	/** Where the world runs. Drunk with nothing running, it goes to the end of the row. */
	public void add(PlayerEntity player, int minutes) {
		if (!left.has(player)) {
			int last = 0;
			for (Timed t : ALL) if (t != this && t.left.has(player)) last = Math.max(last, t.order.get(player));
			order.set(player, last + 1);
		}
		set(player, Math.min(MOST, left.get(player) + minutes * TICKS_A_MINUTE));
	}

	void tick() {
		for (PlayerEntity player : Server.players()) {
			if (left.has(player)) set(player, left.get(player) - 1);
		}
	}

	private void set(PlayerEntity player, int ticks) {
		if (ticks <= 0) {
			left.set(player, null);
			shown.set(player, null);
			order.set(player, null);
			return;
		}
		left.set(player, ticks);
		int steps = steps(ticks);
		if (shown.get(player) != steps) shown.set(player, steps);
	}
}
