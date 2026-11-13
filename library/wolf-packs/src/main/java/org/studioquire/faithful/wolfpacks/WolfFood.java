package org.studioquire.faithful.wolfpacks;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FoodItem;
import net.minecraft.item.Item;
import org.studioquire.oldfaithful.core.api.Attachment;
import org.studioquire.oldfaithful.core.api.Codec;
import org.studioquire.oldfaithful.core.api.Mod;

/**
 * What tamed wolves eat and what it heals them, where the world runs. Beta feeds a tamed wolf any meat, which in beta is
 * only pork, for a raw porkchop's worth whatever it was, and not at full health. Here fish too, raw or cooked, each for its own worth as it heals a player;
 * with Hearty Food loaded, as Hearty Food heals a player: half as much again, half a heart a second, never more pending than the wolf is missing, and not
 * eaten with a full bar on its way (Hearty Food's cake rule, beta's rule at full health stretched to fit).
 *
 * <p>A client on a server never guesses a feeding (beta's guesses pork, eating it on the spot): its rules can't tell it whether the server has this mod, and a
 * wrong guess would stay. The server's answer comes back as the hand's slot, and as the tail, which shows a wolf's health.
 */
public final class WolfFood {
	/** A full bar, in half-hearts, a wolf's as a player's. */
	public static final int FULL = 20;
	/** The hotbar's first slot in beta's player screen: crafting result, grid, armour and the inventory's three rows come first. */
	private static final int HOTBAR_SLOT = 36;

	/** Whether Hearty Food is here. The library's switches take effect at a restart, so once is enough. */
	static boolean hearty;
	/** Half-hearts of Hearty Food's healing still to come, as a player's {@code hearty-food:pending}; saved with the wolf. */
	public static Attachment<Entity, Integer> pending;

	private WolfFood() {
	}

	static void init(Mod mod) {
		hearty = FabricLoader.getInstance().isModLoaded("hearty-food");
		pending = mod.attachment("pending", Attachment.entity(Codec.INT)).persist().defaultValue(() -> 0);
	}

	public static boolean on() {
		return WolfPacks.feeding != null && WolfPacks.feeding.get();
	}

	public static boolean hearty() {
		return hearty;
	}

	/** Pork, as in beta, and any mod's meat; and fish, raw or cooked. */
	public static boolean eats(FoodItem food) {
		return food.isMeat() || food == Item.RAW_FISH || food == Item.COOKED_FISH;
	}

	/** Not at full health, nor with Hearty Food a full bar on its way. */
	public static boolean hungry(WolfEntity wolf) {
		return wolf.health + (hearty ? pending.get(wolf) : 0) < FULL;
	}

	/** It has eaten something that heals a player {@code restored} half-hearts. Without Hearty Food, {@code heal} is beta's own call, already wrapped round this. */
	public static void eat(WolfEntity wolf, int restored, java.util.function.IntConsumer heal) {
		if (hearty) HeartyWolves.eat(wolf, restored);
		else heal.accept(restored);
	}

	/** Each tick of a wolf where the world runs. Without Hearty Food, what's pending waits in the save for its return, as a player's does. */
	public static void tick(WolfEntity wolf) {
		if (hearty && !wolf.world.isRemote) HeartyWolves.tick(wolf);
	}

	/**
	 * Food refused where beta's client would have guessed it eaten (pork, the tail short of full), which nothing would correct, as the server's slot hasn't
	 * changed: the hand goes back to the client with the next slot updates. In single player the client has the world's own inventory, so it's nothing.
	 */
	@SuppressWarnings("unchecked")
	public static void sendHandBack(PlayerEntity player) {
		player.playerScreenHandler.trackedStacks.set(HOTBAR_SLOT + player.inventory.selectedSlot, null);
	}
}
