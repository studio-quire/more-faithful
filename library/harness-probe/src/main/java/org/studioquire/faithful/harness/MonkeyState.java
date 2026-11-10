package org.studioquire.faithful.harness;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityRegistry;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.world.World;
import org.studioquire.oldfaithful.core.api.Stacks;

/**
 * What a player and the pad around them look like from one side: lines, built by the same code on the client and the server, so the difference between the two sides'
 * lines is a desync and nothing else. Also what each side thinks the player is holding in all, by item.
 */
public final class MonkeyState {
	public static final int RADIUS = 5, HEIGHT = 5;
	private static final int ENTITY_RADIUS = 12;
	/** Vanilla's blocks whose meta the server moves without telling anyone: liquids, fire, leaves, farmland, and what grows. */
	private static final java.util.Set<Integer> SILENT_META = new java.util.HashSet<>(java.util.Arrays.asList(8, 9, 10, 11, 18, 51, 59, 60, 81, 83));

	public final List<String> lines = new ArrayList<>();
	/** Items in the player's inventory, on the cursor and in an open screen, by name: only this mod's kind of count goes up by a dupe. */
	public final Map<String, Integer> totals = new TreeMap<>();
	/** Whether the open screen makes things, so its totals may fairly change. */
	public boolean makes;

	private MonkeyState() {
	}

	public static String stack(ItemStack s) {
		if (s == null || s.count == 0) return "-";
		NbtCompound data = Stacks.peek(s);
		return ModIds.item(s.itemId) + " x" + s.count + " :" + s.getDamage() + (data == null ? "" : " " + ModIds.canon(data));
	}

	private void count(ItemStack s) {
		if (s != null && s.count > 0) totals.merge(ModIds.item(s.itemId), s.count, Integer::sum);
	}

	/**
	 * Still and flowing are one liquid here. Beta swaps them with no block update, so a client holds still water where the server's has gone flowing, until something else changes the cell. Water
	 * against air is still a ghost.
	 */
	private static int sameLiquid(int id) {
		return id == 8 ? 9 : id == 10 ? 11 : id;
	}

	public static MonkeyState of(World world, PlayerEntity player, int x, int y, int z) {
		MonkeyState state = new MonkeyState();
		List<String> lines = state.lines;
		for (int dx = -RADIUS; dx <= RADIUS; dx++) {
			for (int dz = -RADIUS; dz <= RADIUS; dz++) {
				for (int dy = 0; dy < HEIGHT; dy++) {
					int id = sameLiquid(world.getBlockId(x + dx, y + dy, z + dz));
					if (id != 0) lines.add("cell " + dx + " " + dy + " " + dz + " " + ModIds.block(id) + " " + (SILENT_META.contains(id) ? "" : world.getBlockMeta(x + dx, y + dy, z + dz)));
				}
			}
		}
		for (int i = 0; i < player.inventory.main.length; i++) {
			ItemStack s = player.inventory.main[i];
			if (s != null && s.count > 0) lines.add("inv " + i + " " + stack(s));
		}
		lines.add("cursor " + stack(player.inventory.getCursorStack()));
		state.count(player.inventory.getCursorStack());
		ScreenHandler handler = player.currentScreenHandler;
		if (handler != player.playerScreenHandler) {
			lines.add("open " + handler.syncId + " " + handler.slots.size());
			for (int i = 0; i < handler.slots.size(); i++) {
				Slot slot = (Slot) handler.slots.get(i);
				ItemStack s = slot.getStack();
				lines.add("slot " + i + " " + stack(s));
				state.count(s);
				if (!slot.canInsert(new ItemStack(net.minecraft.block.Block.DIRT))) state.makes = true;
			}
		} else {
			for (ItemStack s : player.inventory.main) state.count(s);
		}
		Map<String, Integer> kinds = new TreeMap<>();
		for (Object o : world.entities) {
			Entity e = (Entity) o;
			if (e instanceof PlayerEntity || Math.abs(e.x - x) > ENTITY_RADIUS || Math.abs(e.z - z) > ENTITY_RADIUS || Math.abs(e.y - y) > ENTITY_RADIUS) continue;
			String kind = e instanceof ItemEntity ? "item " + stack(((ItemEntity) e).stack) : String.valueOf(EntityRegistry.getId(e));
			kinds.merge(kind, 1, Integer::sum);
		}
		kinds.forEach((k, v) -> lines.add("entity " + k + " x" + v));
		Collections.sort(lines);
		return state;
	}

	public void write(DataOutputStream out) throws IOException {
		out.writeBoolean(makes);
		out.writeInt(lines.size());
		for (String line : lines) out.writeUTF(line);
		out.writeInt(totals.size());
		for (Map.Entry<String, Integer> e : totals.entrySet()) {
			out.writeUTF(e.getKey());
			out.writeInt(e.getValue());
		}
	}

	public static MonkeyState read(DataInputStream in) throws IOException {
		MonkeyState state = new MonkeyState();
		state.makes = in.readBoolean();
		for (int n = in.readInt(); n > 0; n--) state.lines.add(in.readUTF());
		for (int n = in.readInt(); n > 0; n--) state.totals.put(in.readUTF(), in.readInt());
		return state;
	}
}
