package org.studioquire.oldfaithful.core.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.studioquire.oldfaithful.core.impl.Attachments;

/**
 * Players, on both sides of beta's split: single player and a server run joining, death and respawn through separate code, and
 * these hooks hold for either. All of them run where the world runs, never on a client on a server.
 */
public final class Players {
	/** What a death would drop: the player's things, and anything their attachments let fall. */
	public static final class DeathDrops {
		public final PlayerEntity player;
		private final List<ItemStack> stacks = new ArrayList<>();
		private final List<ItemStack> destroyed = new ArrayList<>();

		DeathDrops(PlayerEntity player) {
			this.player = player;
		}

		/** What will fall, as the last listener left it. */
		public List<ItemStack> stacks() {
			return Collections.unmodifiableList(stacks);
		}

		/** This stack doesn't fall: it stays with the player and is on the new one after the respawn. */
		public void keep(ItemStack stack) {
			stacks.removeIf(s -> s == stack);
		}

		/** This stack doesn't fall and is gone. */
		public void destroy(ItemStack stack) {
			if (stacks.removeIf(s -> s == stack)) destroyed.add(stack);
		}

		/** Something else falls. */
		public void add(ItemStack stack) {
			stacks.add(stack);
		}

		boolean leavesInventory(ItemStack stack) {
			for (ItemStack s : stacks) {
				if (s == stack) return true;
			}
			for (ItemStack s : destroyed) {
				if (s == stack) return true;
			}
			return false;
		}
	}

	public static final class DimensionChange {
		public final PlayerEntity player;
		/** Beta's numbers: 0 the overworld, -1 the Nether. */
		public final int from, to;

		DimensionChange(PlayerEntity player, int from, int to) {
			this.player = player;
			this.from = from;
			this.to = to;
		}
	}

	private static final Listeners<Consumer<PlayerEntity>> JOIN = new Listeners<>("Players.onJoin");
	private static final Listeners<Consumer<PlayerEntity>> LEAVE = new Listeners<>("Players.onLeave");
	private static final Listeners<Predicate<PlayerEntity>> KEEP = new Listeners<>("Players.keepInventory");
	private static final Listeners<Consumer<DeathDrops>> DROPS = new Listeners<>("Players.onDrops");
	private static final Listeners<BiConsumer<PlayerEntity, PlayerEntity>> RESPAWN = new Listeners<>("Players.onRespawn");
	private static final Listeners<Consumer<DimensionChange>> DIMENSION = new Listeners<>("Players.onDimensionChange");
	private static final Set<String> PRESENT = new HashSet<>();

	private Players() {
	}

	/**
	 * Notice: a player is in the world, with their saved data read and their attachments in place. On a server, after their
	 * inventory is sent; in single player, when a world is opened.
	 */
	public static void onJoin(Consumer<PlayerEntity> listener) {
		JOIN.add(listener);
	}

	/**
	 * Notice: a player is leaving, before they're saved: a disconnect, a kick, a server stopping, or single player closing its
	 * world. Heard once for each join.
	 */
	public static void onLeave(Consumer<PlayerEntity> listener) {
		LEAVE.add(listener);
	}

	/** Veto: when any listener says so, this player keeps everything on death. Every listener is asked. */
	public static void keepInventory(Predicate<PlayerEntity> listener) {
		KEEP.add(listener);
	}

	/**
	 * Filter, in order, each seeing what the last left: what falls when a player dies. Not heard when the inventory is kept.
	 * {@link DeathDrops#keep} is how a stack stays with its owner.
	 */
	public static void onDrops(Consumer<DeathDrops> listener) {
		DROPS.add(listener);
	}

	/**
	 * Notice, with the dead player's object and the new one, when the new one is in the world: beta makes a new object for a
	 * respawn. The new one has what was kept and the attachments their policies carry. A death in the Nether is this hook alone,
	 * not a {@link #onDimensionChange}.
	 */
	public static void onRespawn(BiConsumer<PlayerEntity, PlayerEntity> listener) {
		RESPAWN.add(listener);
	}

	/** Notice: a living player has gone through a portal and is placed in the other world. The object stays the same. */
	public static void onDimensionChange(Consumer<DimensionChange> listener) {
		DIMENSION.add(listener);
	}

	/** The core's. */
	public static void joined(PlayerEntity player) {
		PRESENT.add(player.name);
		JOIN.each(l -> l.accept(player));
	}

	/** The core's. */
	public static void leaving(PlayerEntity player) {
		if (PRESENT.remove(player.name)) LEAVE.each(l -> l.accept(player));
	}

	/** The core's: whether this death drops nothing. */
	public static boolean keepsInventory(PlayerEntity player) {
		boolean[] keep = {false};
		KEEP.each(l -> keep[0] |= l.test(player));
		return keep[0];
	}

	/**
	 * The core's, in place of {@code dropInventory}. A client on a server only ever sees its own view of a death: that goes on as
	 * vanilla's, and the server's word comes with the respawn.
	 */
	public static void death(PlayerEntity player, Runnable vanillaDrop) {
		if (player.world.isRemote) {
			vanillaDrop.run();
			return;
		}
		if (keepsInventory(player)) return;
		List<ItemStack> fromAttachments = Attachments.deathDrops(player);
		if (DROPS.isEmpty() && fromAttachments.isEmpty()) {
			vanillaDrop.run();
			return;
		}
		PlayerInventory inventory = player.inventory;
		DeathDrops drops = new DeathDrops(player);
		for (ItemStack s : inventory.main) {
			if (s != null) drops.add(s);
		}
		for (ItemStack s : inventory.armor) {
			if (s != null) drops.add(s);
		}
		for (ItemStack s : fromAttachments) drops.add(s);
		DROPS.each(l -> l.accept(drops));
		clearLeaving(inventory.main, drops);
		clearLeaving(inventory.armor, drops);
		for (ItemStack s : drops.stacks()) player.dropItem(s, true);
	}

	private static void clearLeaving(ItemStack[] slots, DeathDrops drops) {
		for (int i = 0; i < slots.length; i++) {
			if (slots[i] != null && drops.leavesInventory(slots[i])) slots[i] = null;
		}
	}

	/**
	 * The core's: what's still on the dead player object goes to the new one, with the attachments their policies carry. Nothing
	 * is, unless it was kept: dropping empties the slots. Done whatever the rule says now, so a rule changed while dead can't
	 * lose anything.
	 */
	public static void carryOver(PlayerEntity dead, PlayerEntity reborn) {
		Attachments.carryOver(dead, reborn);
		if (KEEP.isEmpty() && DROPS.isEmpty()) return;
		PlayerInventory from = dead.inventory, to = reborn.inventory;
		System.arraycopy(from.main, 0, to.main, 0, to.main.length);
		System.arraycopy(from.armor, 0, to.armor, 0, to.armor.length);
		to.selectedSlot = from.selectedSlot;
	}

	/** The core's. */
	public static void respawned(PlayerEntity dead, PlayerEntity reborn) {
		RESPAWN.each(l -> l.accept(dead, reborn));
	}

	/** The core's. */
	public static void dimensionChanged(PlayerEntity player, int from, int to) {
		DimensionChange change = new DimensionChange(player, from, to);
		DIMENSION.each(l -> l.accept(change));
	}
}
