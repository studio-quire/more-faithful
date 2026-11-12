package org.studioquire.faithful.pumpkins.selftest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipeManager;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.world.gen.feature.PumpkinPatchFeature;
import org.studioquire.faithful.harness.test.WorldTest;
import org.studioquire.faithful.harness.test.server.ServerMove;
import org.studioquire.faithful.pumpkins.Pumpkins;
import org.studioquire.faithful.pumpkins.Spreading;
import org.studioquire.oldfaithful.core.impl.Rules;

/**
 * The pie's recipe and eating it, and pumpkins spreading on a field of grass laid round the plot: a lone pumpkin's random ticks, given straight to beta's own block (so
 * through the mod's), grow a patch that stops when full; it lands only on open grass or snow; a jack-o'-lantern and a pumpkin on stone never spread; with the rule off,
 * nothing does. Each random tick given here stands for 409.6 game ticks, a cell's share of beta's. Then a patch of beta's own beside the grown one, for the pictures.
 */
public final class PumpkinsTest extends WorldTest {
	/** The field, from the plot's middle: wide enough for a grown patch and a wild one, each with its counting reach clear of the other, and room for the camera. */
	private static final int WEST = -34, EAST = 26, NORTH = -18, SOUTH = 30, ROOM = 8;
	/** Where the grown patch starts and the wild one is made, from the plot's middle. */
	public static final int GROWN = -12, WILD = 12;
	/** Random ticks with nothing new before a patch counts as done: about 40 tries each. */
	private static final int QUIET = 3000, MOST = 30000, TICKS_A_TICK = 410;

	private Item pie;
	private final Random random = new Random(1107);
	private final List<int[]> grew = new ArrayList<>();

	private ItemStack crafted(int side, Object... cells) {
		CraftingInventory grid = new CraftingInventory(new ScreenHandler() {
			@Override
			public boolean canUse(PlayerEntity player) {
				return true;
			}
		}, side, side);
		for (int i = 0; i < cells.length; i++) {
			if (cells[i] instanceof Item) grid.setStack(i, new ItemStack((Item) cells[i]));
			else if (cells[i] instanceof Block) grid.setStack(i, new ItemStack((Block) cells[i]));
		}
		return CraftingRecipeManager.getInstance().craft(grid);
	}

	private boolean onePie(ItemStack made) {
		return made != null && made.getItem() == pie && made.count == 1;
	}

	private void recipe() {
		check("a pumpkin, sugar and an egg make a pie in the inventory's grid", onePie(crafted(2, Block.PUMPKIN, Item.SUGAR, Item.EGG)));
		check("in any cells at the bench", onePie(crafted(3, null, Item.EGG, null, null, null, null, Item.SUGAR, null, Block.PUMPKIN)));
		check("without the egg, nothing", crafted(2, Block.PUMPKIN, Item.SUGAR) == null);
		check("nor with a second pumpkin", crafted(2, Block.PUMPKIN, Item.SUGAR, Item.EGG, Block.PUMPKIN) == null);
		ItemStack lantern = crafted(2, Block.PUMPKIN, null, Block.TORCH);
		check("a pumpkin over a torch is still a jack-o'-lantern", lantern != null && lantern.itemId == Block.JACK_O_LANTERN.id);
	}

	/** What the game does with a right click that no block took. */
	private void click() {
		ItemStack held = player.inventory.getSelectedItem();
		ItemStack after = held.use(world, player);
		player.inventory.main[player.inventory.selectedSlot] = after.count == 0 ? null : after;
	}

	private void eating() {
		check("one to a slot, as beta's food is", pie.getMaxCount() == 1);
		hold(new ItemStack(pie));
		player.health = 4;
		click();
		if (FabricLoader.getInstance().isModLoaded("hearty-food")) {
			// Hearty Food heals a food like this over time instead, by its own rule, which its test checks.
			check("eaten with Hearty Food on, it's gone and heals nothing at once: " + player.health, player.health == 4 && player.inventory.getSelectedItem() == null);
		} else {
			check("eaten from 4 half-hearts it comes to 12, a cooked porkchop's 8 more, and it's gone: " + player.health,
					player.health == 4 + Pumpkins.PIE_HEALS && player.inventory.getSelectedItem() == null);
		}
		player.health = 20;
	}

	/** Grass over dirt, open air over it, and tall grass and flowers about as a meadow has them; water or lava at the edge turns to stone so nothing floods in. */
	private void field() {
		Random meadow = new Random(31);
		for (int dx = WEST - 1; dx <= EAST + 1; dx++) {
			for (int dz = NORTH - 1; dz <= SOUTH + 1; dz++) {
				boolean rim = dx < WEST || dx > EAST || dz < NORTH || dz > SOUTH;
				for (int dy = -2; dy <= ROOM; dy++) {
					if (rim) {
						if (world.getMaterial(x + dx, y + dy, z + dz).isFluid()) set(dx, dy, dz, Block.STONE);
						continue;
					}
					set(dx, dy, dz, dy == -2 ? Block.DIRT : dy == -1 ? Block.GRASS_BLOCK : null);
				}
				if (rim) continue;
				int r = meadow.nextInt(100);
				if (r < 12) set(dx, 0, dz, Block.GRASS, 1);
				else if (r < 14) set(dx, 0, dz, Block.DANDELION);
				else if (r < 15) set(dx, 0, dz, Block.ROSE);
			}
		}
	}

	/** Clears a square round a spot down to its grass, pumpkins, snow and all, and lays fresh grass under it. */
	private void clear(int cx, int cz, int r) {
		for (int dx = cx - r; dx <= cx + r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				set(dx, -1, cz + dz, Block.GRASS_BLOCK);
				for (int dy = 0; dy <= 2; dy++) set(dx, dy, cz + dz, null);
			}
		}
	}

	private List<int[]> pumpkins(int cx, int cz, int r) {
		List<int[]> found = new ArrayList<>();
		for (int dx = cx - r; dx <= cx + r; dx++) {
			for (int dz = cz - r; dz <= cz + r; dz++) {
				for (int dy = -4; dy <= 4; dy++) if (block(dx, dy, dz) == Block.PUMPKIN.id) found.add(new int[] {x + dx, y + dy, z + dz});
			}
		}
		return found;
	}

	/**
	 * A random tick to each block here, round after round until {@code quiet} rounds pass with nothing new or {@code rounds} are done; with {@code follow}, what grows gets
	 * ticks from the next round on, as it would in the world. The round of the last growth, or -1.
	 */
	private int tick(List<int[]> cells, int rounds, int quiet, boolean follow) {
		int last = -1;
		for (int round = 0; round < rounds && round - last <= quiet; round++) {
			int before = grew.size();
			for (int i = 0, n = cells.size(); i < n; i++) {
				int[] c = cells.get(i);
				int id = world.getBlockId(c[0], c[1], c[2]);
				if (id != 0) Block.BLOCKS[id].onTick(world, c[0], c[1], c[2], random);
			}
			if (follow) for (int i = before; i < grew.size(); i++) cells.add(grew.get(i));
			if (grew.size() > before) last = round;
		}
		return last;
	}

	private static int around(List<int[]> all, int[] c) {
		int n = 0;
		for (int[] o : all) if (Math.abs(o[0] - c[0]) <= 2 * Spreading.REACH && Math.abs(o[2] - c[2]) <= 2 * Spreading.REACH && Math.abs(o[1] - c[1]) <= 2 * Spreading.RISE) n++;
		return n;
	}

	/** Where a new pumpkin may land, cell by cell: the rule itself. */
	private void landing() {
		int cx = WILD, cz = 0;
		Object[][] cases = {{Block.GRASS_BLOCK, null, true, "open air over grass"}, {Block.GRASS_BLOCK, Block.SNOW, true, "a snow layer over grass"},
				{Block.GRASS_BLOCK, Block.GRASS, false, "tall grass"}, {Block.GRASS_BLOCK, Block.DANDELION, false, "a flower"}, {Block.DIRT, null, false, "open air over dirt"},
				{Block.STONE, null, false, "open air over stone"}, {Block.SAND, null, false, "open air over sand"}};
		for (Object[] c : cases) {
			clear(cx, cz, 1);
			set(cx, -1, cz, (Block) c[0]);
			if (c[1] != null) set(cx, 0, cz, (Block) c[1], c[1] == Block.GRASS ? 1 : 0);
			int at = Spreading.landing(world, x + cx, y, z + cz);
			check("a pumpkin " + ((boolean) c[2] ? "may" : "may not") + " land on " + c[3], (boolean) c[2] == (at == y));
		}
		clear(cx, cz, 1);
	}

	/** A jack-o'-lantern on grass and a pumpkin on stone, with open grass round both, given many chances. */
	private void neither() {
		clear(WILD, 0, 8);
		set(WILD, 0, 0, Block.JACK_O_LANTERN, 2);
		set(WILD, -1, 5, Block.STONE);
		set(WILD, 0, 5, Block.PUMPKIN, 2);
		List<int[]> cells = new ArrayList<>();
		cells.add(new int[] {x + WILD, y, z});
		cells.add(new int[] {x + WILD, y, z + 5});
		int before = grew.size();
		tick(cells, QUIET, QUIET, false);
		check("a jack-o'-lantern on grass and a pumpkin on stone never spread, in " + QUIET + " random ticks each, about " + (int) (QUIET * 409.6 / 24000) + " in-game days",
				grew.size() == before);
	}

	/** Snow on every open cell round a pumpkin: the ones it sets take the snow's place. */
	private void snow() {
		clear(WILD, 0, 8);
		for (int dx = WILD - 7; dx <= WILD + 7; dx++) {
			for (int dz = -7; dz <= 7; dz++) if (dx != WILD || dz != 0) set(dx, 0, dz, Block.SNOW);
		}
		set(WILD, 0, 0, Block.PUMPKIN, 2);
		List<int[]> cells = new ArrayList<>();
		cells.add(new int[] {x + WILD, y, z});
		int before = grew.size();
		// The first pumpkin only, so its patch stays inside the square cleared after.
		tick(cells, MOST, QUIET, false);
		int onSnow = 0, all = grew.size() - before;
		for (int i = before; i < grew.size(); i++) if (grew.get(i)[3] == Block.SNOW.id) onSnow++;
		check("under snow, a pumpkin still spreads, each one in a snow layer's place: " + onSnow + " of " + all, all > 0 && onSnow == all);
		clear(WILD, 0, 8);
	}

	private void ruleOff() {
		Rules.change(Pumpkins.spreading, "false");
		set(GROWN, -1, 0, Block.DIRT);
		set(GROWN, 0, 0, Block.PUMPKIN, 0);
		List<int[]> cells = new ArrayList<>();
		cells.add(new int[] {x + GROWN, y, z});
		tick(cells, QUIET, QUIET, false);
		check("with the rule off, a pumpkin on open ground sets none in " + QUIET + " random ticks", grew.isEmpty());
	}

	private void grow() {
		List<int[]> meadow = new ArrayList<>();
		for (int dx = GROWN - 7; dx <= GROWN + 7; dx++) {
			for (int dz = -7; dz <= 7; dz++) {
				int id = block(dx, 0, dz);
				if (id == Block.GRASS.id || id == Block.DANDELION.id || id == Block.ROSE.id) meadow.add(new int[] {dx, dz, id});
			}
		}
		List<int[]> cells = new ArrayList<>();
		cells.add(new int[] {x + GROWN, y, z});
		int before = grew.size();
		int last = tick(cells, MOST, QUIET, true);
		List<int[]> patch = pumpkins(GROWN, 0, 3 * Spreading.REACH);
		int unfull = 0, wrong = 0, trampled = 0, minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = minX, maxZ = maxX;
		for (int[] p : patch) {
			if (around(patch, p) < Spreading.FULL) unfull++;
			minX = Math.min(minX, p[0]);
			maxX = Math.max(maxX, p[0]);
			minZ = Math.min(minZ, p[2]);
			maxZ = Math.max(maxZ, p[2]);
		}
		for (int i = before; i < grew.size(); i++) {
			int[] g = grew.get(i);
			if (g[3] != 0 && g[3] != Block.SNOW.id || world.getBlockId(g[0], g[1] - 1, g[2]) != Block.GRASS_BLOCK.id) wrong++;
		}
		for (int[] m : meadow) if (block(m[0], 0, m[1]) != m[2]) trampled++;
		note("from one pumpkin: " + patch.size() + " pumpkins, " + (maxX - minX + 1) + " by " + (maxZ - minZ + 1) + " cells; the last set at random tick " + last + ", about "
				+ last * 409.6 / 24000 + " in-game days, " + meadow.size() + " tufts and flowers in its reach");
		check("a lone pumpkin grows to a full patch: " + around(patch, new int[] {x + GROWN, y, z}) + " within twice the reach of it", around(patch, new int[] {x + GROWN, y, z}) >= Spreading.FULL);
		check("and stops: every pumpkin in it has " + Spreading.FULL + " near it (" + unfull + " not), and " + QUIET + " random ticks more set nothing", unfull == 0 && last >= 0 && last + QUIET < MOST);
		check("every one on open grass, or snow, over grass: " + wrong + " not", wrong == 0);
		check("tall grass and flowers never taken: " + trampled + " of " + meadow.size() + " gone", trampled == 0);
	}

	/**
	 * Beta's own patch, as its generator makes one starting near the ground, for the pictures beside the grown one: the first seed's from 1 that makes one of 4 to 8, the
	 * middle of what beta's hold.
	 */
	private void wild() {
		for (int seed = 1; seed < 50; seed++) {
			clear(WILD, 0, 8);
			Random field = new Random(31 * 7);
			for (int dx = WILD - 8; dx <= WILD + 8; dx++) {
				for (int dz = -8; dz <= 8; dz++) {
					int r = field.nextInt(100);
					if (r < 12) set(dx, 0, dz, Block.GRASS, 1);
					else if (r < 14) set(dx, 0, dz, Block.DANDELION);
					else if (r < 15) set(dx, 0, dz, Block.ROSE);
				}
			}
			Random patch = new Random(seed);
			new PumpkinPatchFeature().generate(world, patch, x + WILD, y + patch.nextInt(7) - 3, z);
			int n = pumpkins(WILD, 0, 8).size();
			if (n >= 4 && n <= 8) {
				note("beta's own patch beside it: " + n + " pumpkins (its seed " + seed + ")");
				return;
			}
		}
		note("no patch of beta's of 4 to 8 in 49 seeds");
	}

	@Override
	protected void script() {
		pie = Pumpkins.pie.get();
		// On a server the harness has loaded only 24 blocks round the plot's middle when a test starts, a server reads and writes an unloaded chunk as empty, and a
		// pumpkin won't spread unless all within twice its reach is loaded. So load the field, its rim and that reach round it: a plot near a chunk's edge otherwise
		// loses the field's east side (found in the library's run of every mod).
		if (!singlePlayer()) {
			int margin = 2 * Spreading.REACH + 1;
			ServerMove.load(world, x + (WEST + EAST) / 2, z + (NORTH + SOUTH) / 2, Math.max(EAST - WEST, SOUTH - NORTH) / 2 + margin);
		}
		if (restarted()) {
			ItemStack kept = player.inventory.main[1];
			check("a pie came back as a pie", kept != null && kept.getItem() == pie);
			return;
		}
		then(() -> {
			recipe();
			eating();
			player.inventory.main[1] = new ItemStack(pie);
			player.currentScreenHandler.sendContentUpdates();
		});
		then(() -> {
			Spreading.grew = grew::add;
			field();
		});
		then(this::ruleOff);
		then(() -> {
			check("the rule turns on", Rules.change(Pumpkins.spreading, "true") && Pumpkins.spreading.get());
			landing();
			neither();
			snow();
			grow();
			wild();
		});
		then(() -> Spreading.grew = null);
	}
}
