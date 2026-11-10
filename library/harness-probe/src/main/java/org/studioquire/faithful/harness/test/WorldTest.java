package org.studioquire.faithful.harness.test;

import java.util.HashMap;
import java.util.Map;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * A mod's test where the world runs: single player's game, or the server with a real client joined. One class for both places, under the entrypoint {@code faithful:test} in the selftest's
 * {@code fabric.mod.json}. It's common code, so it mustn't reach the client (`libraryStatic` reads it). It runs in a fresh world, then again in the same world after a save and a restart,
 * with {@link #restarted()} true and the player back on the same spot. Before each: the player's inventory is emptied, the hotbar's first slot is the one held, and they stand at {@link #x}, {@link #y}, {@link #z} on a stone floor
 * reaching {@link #PLOT} cells each way, with the air above it clear. That plot is the test's: other mods' tests have their own.
 */
public abstract class WorldTest extends Script {
	public static final int PLOT = 8;

	public interface Scene {
		void run(String[] args);
	}

	protected World world;
	protected PlayerEntity player;
	/** The cell the player stands in: the floor is at {@code y - 1}. */
	protected int x, y, z;
	private boolean restarted;
	private final Map<String, Scene> scenes = new HashMap<>();

	final void place(World world, PlayerEntity player, int x, int y, int z, boolean restarted) {
		this.world = world;
		this.player = player;
		this.x = x;
		this.y = y;
		this.z = z;
		this.restarted = restarted;
		scenes.clear();
	}

	/** True in the world's second life, after a save and a restart. What the first left is there. */
	protected final boolean restarted() {
		return restarted;
	}

	/** Single player's game rather than a server. */
	protected final boolean singlePlayer() {
		return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
	}

	/** Something the mod's {@code ClientTest} can ask for by name, on whichever side runs the world; it hears back once the scene's steps are over. Declared in {@code script()}. */
	protected final void scene(String name, Scene scene) {
		scenes.put(name, scene);
	}

	final boolean scene(String name, String[] args) {
		Scene scene = scenes.get(name);
		if (scene == null) return false;
		run(() -> scene.run(args));
		return true;
	}

	/** Places a block by its offset from the middle of the plot. Null clears the cell. */
	protected final void set(int dx, int dy, int dz, Block block) {
		world.setBlock(x + dx, y + dy, z + dz, block == null ? 0 : block.id);
	}

	protected final void set(int dx, int dy, int dz, Block block, int meta) {
		world.setBlock(x + dx, y + dy, z + dz, block == null ? 0 : block.id, meta);
	}

	protected final int block(int dx, int dy, int dz) {
		return world.getBlockId(x + dx, y + dy, z + dz);
	}

	protected final int meta(int dx, int dy, int dz) {
		return world.getBlockMeta(x + dx, y + dy, z + dz);
	}

	/** Into the player's hand, replacing what's there. */
	protected final void hold(ItemStack stack) {
		player.inventory.main[player.inventory.selectedSlot] = stack;
		player.currentScreenHandler.sendContentUpdates();
	}
}
