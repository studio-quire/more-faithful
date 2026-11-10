package org.studioquire.oldfaithful.core.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntSupplier;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerListener;
import org.studioquire.oldfaithful.core.impl.content.ContentNet;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;
import org.studioquire.oldfaithful.core.impl.content.ListenerAccess;

/**
 * A mod's own inventory screens. Declare one with {@link Content#screen}, give it a screen on the client with {@code ContainerScreens.register}, and open it with {@link #open} from a block's
 * {@code onUse} or anywhere else the world runs: there it's opened, and on a remote client the call does nothing, as vanilla's {@code onUse} leaves the job to the server.
 * <p>
 * The same factory builds the handler everywhere, and what it's handed differs:
 * <ul>
 * <li>in single player and on a server, the real inventory, the one given to {@link #open};</li>
 * <li>on a remote client, an empty one of the same name and size, which vanilla's slot packets fill. There's no block entity behind it, so test with {@code instanceof} rather than cast, and for
 * anything else a handler needs at open, use {@code data}.</li>
 * </ul>
 * A handler's slots must be the same on both sides: changing them changes what crosses the wire, so it's a new {@code "network"} version of the mod.
 * <p>
 * Numbers a screen shows (a gauge, progress) go through {@link Tracked}. {@link Slots} is a plain inventory for common code: vanilla's {@code SimpleInventory} exists on the client only.
 */
public final class Containers {
	/** {@code data} is what {@link #open} was given, empty if nothing; never null. */
	public interface Factory {
		ScreenHandler create(PlayerEntity player, Inventory inventory, NbtCompound data);
	}

	/**
	 * A handler's numbers, for its screen. Vanilla's way splits three ways: on a server they're sent from {@code sendContentUpdates}, on a remote client they arrive in {@code setProperty},
	 * and in single player nothing fires at all, so the screen has to read the block entity. This is that split once, and whole ints, where vanilla's packet carries a short:
	 * <pre>{@code
	 * private final Containers.Tracked tracked = new Containers.Tracked(this, 0);
	 * private final Containers.Tracked.Value progress;
	 *
	 * MyHandler(PlayerEntity player, Inventory inventory) {
	 *     MyEntity entity = inventory instanceof MyEntity ? (MyEntity) inventory : null;   // null on a remote client
	 *     progress = tracked.add(entity == null ? null : entity::progress);
	 * }
	 * public int progress() { return progress.get(); }                                    // the screen reads this, on every side
	 *
	 * @Override public void sendContentUpdates() { super.sendContentUpdates(); tracked.send(); }
	 * @Environment(EnvType.CLIENT) @Override public void setProperty(int id, int value) { tracked.receive(id, value); }
	 * }</pre>
	 * Add the values in the same order on both sides. A value takes two property ids, from {@code firstId} up, so a handler with vanilla's properties starts past them (a furnace's use 0 to 2) and
	 * {@link #receive} says false for what isn't its own.
	 */
	public static final class Tracked {
		public static final class Value {
			private final IntSupplier source;
			private boolean sent;
			private int last, low, received;

			private Value(IntSupplier source) {
				this.source = source;
			}

			/** The number now: read straight from its source where there is one, otherwise the last that arrived, 0 until then. */
			public int get() {
				return source != null ? source.getAsInt() : received;
			}
		}

		private final ScreenHandler handler;
		private final int firstId;
		private final List<Value> values = new ArrayList<>();

		public Tracked(ScreenHandler handler, int firstId) {
			this.handler = handler;
			this.firstId = firstId;
		}

		/** @param source where the world runs, how to read it; null on a remote client, where the value is only what's received */
		public Value add(IntSupplier source) {
			Value value = new Value(source);
			values.add(value);
			return value;
		}

		/** From {@code sendContentUpdates}, after {@code super}'s: what's changed goes to the player watching. The first call with someone watching sends everything. */
		public void send() {
			List<?> listeners = ((ListenerAccess) handler).faithful$listeners();
			if (listeners.isEmpty()) return;
			for (int i = 0; i < values.size(); i++) {
				Value v = values.get(i);
				if (v.source == null) continue;
				int now = v.source.getAsInt();
				if (v.sent && now == v.last) continue;
				v.sent = true;
				v.last = now;
				int id = firstId + i * 2;
				for (Object listener : listeners) {
					((ScreenHandlerListener) listener).onPropertyUpdate(handler, id, (short) now);
					((ScreenHandlerListener) listener).onPropertyUpdate(handler, id + 1, (short) (now >> 16));
				}
			}
		}

		/** From {@code setProperty}. False if this id isn't one of these. A value changes when its high half lands, so a screen never shows half of one. */
		public boolean receive(int id, int value) {
			int at = id - firstId;
			if (at < 0 || at >= values.size() * 2) return false;
			Value v = values.get(at / 2);
			if (at % 2 == 0) v.low = value & 0xFFFF;
			else v.received = value << 16 | v.low;
			return true;
		}
	}

	/** A plain inventory, on both sides, that saves itself. Vanilla's rules: 64 to a stack, and a player within 8 blocks of the block entity it belongs to. Extend it to add rules of your own. */
	public static class Slots implements Inventory {
		private final String name;
		private final BlockEntity owner;
		private final ItemStack[] stacks;

		/** @param owner the block entity it lives in, which is told of changes and says who's near; null for none, and then anyone may use it */
		public Slots(String name, int size, BlockEntity owner) {
			this.name = name;
			this.owner = owner;
			this.stacks = new ItemStack[size];
		}

		@Override
		public int size() {
			return stacks.length;
		}

		@Override
		public ItemStack getStack(int slot) {
			return stacks[slot];
		}

		@Override
		public ItemStack removeStack(int slot, int amount) {
			ItemStack stack = stacks[slot];
			if (stack == null) return null;
			ItemStack taken;
			if (stack.count <= amount) {
				taken = stack;
				stacks[slot] = null;
			} else {
				taken = stack.split(amount);
			}
			markDirty();
			return taken;
		}

		@Override
		public void setStack(int slot, ItemStack stack) {
			stacks[slot] = stack;
			if (stack != null && stack.count > getMaxCountPerStack()) stack.count = getMaxCountPerStack();
			markDirty();
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public int getMaxCountPerStack() {
			return 64;
		}

		@Override
		public void markDirty() {
			if (owner != null) owner.markDirty();
		}

		@Override
		public boolean canPlayerUse(PlayerEntity player) {
			if (owner == null) return true;
			return owner.world.getBlockEntity(owner.x, owner.y, owner.z) == owner && player.getSquaredDistance(owner.x + 0.5, owner.y + 0.5, owner.z + 0.5) <= 64.0;
		}

		/** Under {@code key}, as a chest's list is. */
		public void write(NbtCompound nbt, String key) {
			NbtList list = new NbtList();
			for (int i = 0; i < stacks.length; i++) {
				if (stacks[i] == null) continue;
				NbtCompound entry = new NbtCompound();
				entry.putByte("Slot", (byte) i);
				stacks[i].writeNbt(entry);
				list.add(entry);
			}
			nbt.put(key, list);
		}

		public void read(NbtCompound nbt, String key) {
			Arrays.fill(stacks, null);
			NbtList list = nbt.getList(key);
			for (int i = 0; i < list.size(); i++) {
				NbtCompound entry = (NbtCompound) list.get(i);
				int slot = entry.getByte("Slot") & 255;
				if (slot < stacks.length) stacks[slot] = new ItemStack(entry);
			}
		}
	}

	private Containers() {
	}

	public static void open(PlayerEntity player, String name, Inventory inventory) {
		open(player, name, inventory, new NbtCompound());
	}

	/** @param name a full name, {@code my-mod:my_screen}, though {@link Content#screen} takes the short one; {@code data} up to 32 KB */
	public static void open(PlayerEntity player, String name, Inventory inventory, NbtCompound data) {
		if (ContentRegistry.screen(name) == null) throw new IllegalArgumentException("No screen is declared as " + name + (name.indexOf(':') < 0 ? ": this takes the full name, my-mod:" + name : ""));
		ContentNet.side().openContainer(player, name, inventory, data);
	}
}
