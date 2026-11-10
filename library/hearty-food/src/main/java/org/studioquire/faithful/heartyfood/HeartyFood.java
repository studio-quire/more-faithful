package org.studioquire.faithful.heartyfood;

import net.minecraft.entity.player.PlayerEntity;
import org.studioquire.oldfaithful.core.api.Attachment;
import org.studioquire.oldfaithful.core.api.Codec;
import org.studioquire.oldfaithful.core.api.FaithfulMod;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.Server;
import org.studioquire.oldfaithful.core.api.Ticks;

/**
 * Food heals half as much again, over time. Where the world runs, a player has so many half-hearts {@link #pending}; a client only ever hears {@link #endsAt}.
 * <p>
 * What's pending never exceeds the player's missing health, held to that every tick. A hit doesn't touch it, so the end point falls with the hit. Nor does other healing, so the end point rises with
 * that, until the bar is full and what's left of the food has nowhere to go. Either way health plus pending is where you'll end up, and that's what the ghost hearts show.
 * None of it is content: a peer without the mod goes on as beta, with whatever healing the server does.
 */
public final class HeartyFood implements FaithfulMod {
	/** A full bar, in half-hearts. Beta's healing stops there too. */
	public static final int FULL = 20;
	/** Ticks to a half-heart. */
	public static final int PACE = 20;

	public static Attachment<PlayerEntity, Integer> pending, waited, endsAt;

	@Override
	public void init(Mod mod) {
		pending = mod.attachment("pending", Attachment.player(Codec.INT)).persist().defaultValue(() -> 0);
		waited = mod.attachment("waited", Attachment.player(Codec.INT)).defaultValue(() -> 0);
		// The end point rather than the amount, so ghost hearts stand still while hearts fill: health and an amount come in separate packets, which may land a tick apart.
		endsAt = mod.attachment("ends_at", Attachment.player(Codec.INT)).sync(Attachment.Sync.SELF).defaultValue(() -> 0);
		Ticks.onServer(HeartyFood::tick);
	}

	/** What a food beta says heals {@code restored} half-hearts heals now: half as much again, rounding up. */
	public static int healing(int restored) {
		return (restored * 3 + 1) / 2;
	}

	/** A food that fills the whole bar or more heals at once, as in beta: the golden apple, and any mod's like it. */
	public static boolean isWhole(int restored) {
		return restored >= FULL;
	}

	/** Where the world runs: {@code player} has just eaten something beta would have healed them {@code restored} for. Past their missing health it's wasted, as in beta. */
	public static void eat(PlayerEntity player, int restored) {
		if (player.health > 0) {
			if (isWhole(restored)) {
				player.heal(restored);
				clear(player);
			} else {
				int due = pending.get(player);
				int added = Math.max(0, Math.min(healing(restored), FULL - player.health - due));
				if (added > 0) {
					pending.set(player, due + added);
					show(player);
				}
			}
		}
	}

	private static void tick() {
		for (PlayerEntity player : Server.players()) {
			if (pending.has(player)) tick(player);
		}
	}

	private static void tick(PlayerEntity player) {
		if (player.health <= 0) {
			clear(player);
			return;
		}
		int due = Math.min(pending.get(player), FULL - player.health);
		int time = waited.get(player) + 1;
		if (due > 0 && time >= PACE) {
			healOne(player);
			due--;
			time = 0;
		}
		if (due <= 0) {
			clear(player);
			return;
		}
		pending.set(player, due);
		waited.set(player, time);
		show(player);
	}

	/** {@code heal} also ends the half second of protection that follows a hit: once for beta's one bite, but every second of a fight would make healing cost you hits. */
	private static void healOne(PlayerEntity player) {
		int protection = player.hearts;
		player.heal(1);
		if (protection > player.hearts) player.hearts = protection;
	}

	private static void show(PlayerEntity player) {
		int end = player.health + pending.get(player);
		if (endsAt.get(player) != end) endsAt.set(player, end);
	}

	private static void clear(PlayerEntity player) {
		pending.set(player, null);
		waited.set(player, null);
		endsAt.set(player, null);
	}
}
