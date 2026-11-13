package org.studioquire.faithful.golems.selftest.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.network.OtherPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.studioquire.faithful.golems.GolemEntity;
import org.studioquire.faithful.golems.Golems;
import org.studioquire.faithful.golems.selftest.Look;
import org.studioquire.faithful.harness.test.client.ClientTest;

/**
 * The golem's look, for someone to judge: beside a zombie and a player at noon, from the front, the side and the back, with a tool and without, walking, swinging, and against stone.
 * Only in a look run ({@link Look}), in single player, as {@code shots/golems/look-*.png} in the game's folder. Everyone pictured stands as put: nothing here walks, burns or falls.
 */
public final class GolemsLookTest extends ClientTest {
	/** How far apart the three stand, and how far back the camera is from them. */
	private static final double APART = 1.3, BACK = 2.7;
	/** Beta lights a mob's faces by where they point in the world, brightest to the south and north: the camera looks north, and whoever is pictured turns instead of it. */
	private static final float FRONT = 0.0F, QUARTER = -35.0F, SIDE = -90.0F, BEHIND = 180.0F, QUARTER_BEHIND = -145.0F;

	private double cx, cz;
	private int fx, fy, fz, difficulty;
	private Still golem;
	private LivingEntity zombie;
	private Walker player;
	private final List<Entity> cast = new ArrayList<>();

	/** A golem that stays where it's put, its swing, stride and bow whatever the picture wants. */
	private static final class Still extends GolemEntity {
		float swing;
		boolean drawing;

		Still(World world) {
			super(world);
		}

		@Override
		public void tickMovement() {
		}

		@Override
		public float getHandSwingProgress(float tickDelta) {
			return swing;
		}

		@Override
		public boolean drawsBow() {
			return drawing;
		}
	}

	/** A player to stand beside: beta's own model at its own size, in the skin every player had. */
	private static final class Walker extends OtherPlayerEntity {
		float stride;

		Walker(World world) {
			super(world, "");
		}

		@Override
		public void tickMovement() {
		}

		@Override
		public void tick() {
			super.tick();
			stride(this, stride);
		}
	}

	@Override
	protected void script() {
		if (!Look.asked() || restarted() || remote()) return;
		wait(10);
		then(() -> {
			fx = (int) Math.floor(mc.player.x);
			fy = (int) Math.floor(mc.player.y - 1.62);
			fz = (int) Math.floor(mc.player.z);
			cx = fx + 0.5;
			cz = fz + 0.5;
			difficulty = mc.world.difficulty;
			mc.world.difficulty = 1;
			mc.world.setTime(6000L);
			mc.options.hideHud = true;
			golem = new Still(mc.world);
			zombie = new ZombieEntity(mc.world) {
				@Override
				public void tickMovement() {
				}
			};
			player = new Walker(mc.world);
			for (Entity e : new Entity[] {golem, zombie, player}) join(e);
		});

		// A quarry's floor and wall first: the hardest place to tell pale iron from what's behind it.
		then(() -> stage(Block.STONE.id, true));
		three("stone-front", FRONT, Item.DIAMOND_PICKAXE, 40);
		three("stone-quarter", QUARTER, Item.IRON_PICKAXE, 20);
		crew("stone-crew", 20);

		then(() -> stage(Block.GRASS_BLOCK.id, false));
		three("front", FRONT, Item.DIAMOND_PICKAXE, 40);
		three("front-empty", FRONT, null, 20);
		three("quarter", QUARTER, Item.DIAMOND_PICKAXE, 20);
		three("quarter-empty", QUARTER, null, 20);
		three("side", SIDE, Item.DIAMOND_PICKAXE, 20);
		three("side-empty", SIDE, null, 20);
		three("back", BEHIND, Item.DIAMOND_PICKAXE, 20);
		three("back-empty", BEHIND, null, 20);
		three("quarter-back", QUARTER_BEHIND, Item.DIAMOND_PICKAXE, 20);
		three("quarter-back-empty", QUARTER_BEHIND, null, 20);
		three("quarter-back-left", -QUARTER_BEHIND, null, 20);
		then(() -> {
			// A golem walks at half a player's pace, so its stride is half as wide.
			stride(golem, 0.45F);
			player.stride = 0.9F;
		});
		three("walking", -70.0F, Item.DIAMOND_PICKAXE, 20);
		three("walking-empty", -70.0F, null, 20);
		then(() -> {
			stride(golem, 0.0F);
			player.stride = 0.0F;
			golem.swing = 0.22F;
		});
		three("swing", -70.0F, Item.DIAMOND_PICKAXE, 20);
		then(() -> golem.swing = 0.0F);
		// The bow: held at rest, then drawn along its look, level and down from a perch.
		three("bow", QUARTER, Item.BOW, 20);
		then(() -> golem.drawing = true);
		three("bow-drawn", QUARTER, Item.BOW, 20);
		aiming("bow-drawn-side", SIDE, 0.0F);
		aiming("bow-drawn-down", SIDE, 40.0F);
		aiming("bow-drawn-down-quarter", QUARTER, 40.0F);
		then(() -> golem.drawing = false);
		alone("close", QUARTER, Item.DIAMOND_PICKAXE, 20);
		alone("close-empty", 25.0F, null, 20);
		alone("close-side", SIDE, null, 20);
		alone("close-back", BEHIND, null, 20);
		alone("close-quarter-back", QUARTER_BEHIND, null, 20);
		alone("close-quarter-back-left", -QUARTER_BEHIND, null, 20);
		// Hurt: each stage of its cracks (below three-quarters of its health, a half, a quarter), front and back, beside the others and near.
		int[] hurt = {29, 19, 9};
		for (int i = 0; i < hurt.length; i++) {
			int health = hurt[i], stage = i + 1;
			then(() -> {
				golem.health = health;
				check("at " + health + " of " + GolemEntity.MAX_HEALTH + " it's drawn with the cracks of stage " + stage, golem.cracks() == stage);
			});
			three("cracks-" + stage, FRONT, Item.IRON_SWORD, 20);
			alone("cracks-" + stage + "-close", 25.0F, null, 20);
			alone("cracks-" + stage + "-close-back", BEHIND, null, 20);
		}
		then(() -> golem.health = GolemEntity.MAX_HEALTH);
		// The Boundary Pole on the grass between a fence and a torch, near; then four in the hand, with the hotbar showing, for its icon.
		then(() -> {
			park(golem);
			park(zombie);
			park(player);
			mc.world.setBlock(fx - 1, fy, fz - 1, Block.FENCE.id);
			mc.world.setBlock(fx, fy, fz - 1, Golems.pole.get().id);
			mc.world.setBlock(fx + 1, fy, fz - 1, Block.TORCH.id);
			check("a pole stands on the grass", mc.world.getBlockId(fx, fy, fz - 1) == Golems.pole.get().id);
		});
		view("look-pole", 0.75, 1.9, 0.35, 20);
		then(() -> {
			mc.options.hideHud = false;
			mc.player.inventory.main[mc.player.inventory.selectedSlot] = new ItemStack(Golems.pole.get(), 4);
		});
		view("look-pole-held", 0.75, 1.9, 0.35, 20);
		then(() -> {
			mc.options.hideHud = true;
			mc.player.inventory.main[mc.player.inventory.selectedSlot] = null;
		});
		// Its bits: a few hits' worth, then the burst as it comes loose, each pictured just after.
		then(() -> {
			for (int k = 0; k < 6; k++) mc.particleManager.addBlockBreakingParticles(fx, fy, fz - 1, 2 + k % 4);
		});
		view("look-pole-hit", 0.75, 1.9, 0.35, 2);
		then(() -> mc.particleManager.addBlockBreakParticles(fx, fy, fz - 1, Golems.pole.get().id, 0));
		view("look-pole-break", 0.75, 1.9, 0.35, 2);
		then(() -> {
			for (int i = -1; i <= 1; i++) mc.world.setBlock(fx + i, fy, fz - 1, 0);
		});
		// At midnight: a pole no golem uses, one a golem uses (lit, with a lit redstone torch's light) and a lit redstone torch, for the glow beside the torch it's matched to.
		// The lit one is a corner of a golem's rectangle whose other three poles are under the floor, out of sight, the golem handed a hoe in it and then parked.
		then(() -> {
			mc.world.setTime(18000L);
			mc.world.setBlock(fx - 2, fy, fz - 1, Golems.pole.get().id);
			mc.world.setBlock(fx, fy, fz - 1, Golems.pole.get().id);
			for (int[] c : new int[][] {{6, -1}, {0, -7}, {6, -7}}) mc.world.setBlock(fx + c[0], fy - 2, fz + c[1], Golems.pole.get().id);
			mc.world.setBlock(fx + 2, fy, fz - 1, Block.LIT_REDSTONE_TORCH.id);
			put(golem, fx + 3.5, fz - 3.5, FRONT);
			golem.setTool(null);
			mc.player.inventory.main[mc.player.inventory.selectedSlot] = new ItemStack(Item.IRON_HOE);
			golem.interact(mc.player);
			mc.player.inventory.main[mc.player.inventory.selectedSlot] = null;
			park(golem);
		});
		view("look-pole-night", 1.2, 2.4, 0.35, 40);
		view("look-pole-night-far", 2.2, 5.5, 0.2, 20);
		// A golem's four poles at night with lit redstone torches two blocks from two of them, as the user had them (2026-11-10, the near poles drawn darker): every lit pole
		// should look the same, smooth lighting on and off.
		then(() -> {
			for (int i = -2; i <= 2; i++) mc.world.setBlock(fx + i, fy, fz - 1, 0);
			for (int[] c : new int[][] {{6, -1}, {0, -7}, {6, -7}}) mc.world.setBlock(fx + c[0], fy - 2, fz + c[1], Block.DIRT.id);
			golem.interact(mc.player);
			mc.player.inventory.main[mc.player.inventory.selectedSlot] = null;
			for (int[] c : new int[][] {{-3, -3}, {3, -3}, {-3, -9}, {3, -9}}) mc.world.setBlock(fx + c[0], fy, fz + c[1], Golems.pole.get().id);
			mc.world.setBlock(fx - 2, fy, fz - 5, Block.LIT_REDSTONE_TORCH.id);
			mc.world.setBlock(fx + 3, fy, fz - 5, Block.LIT_REDSTONE_TORCH.id);
			put(golem, fx + 0.5, fz - 5.5, FRONT);
			mc.player.inventory.main[mc.player.inventory.selectedSlot] = new ItemStack(Item.IRON_HOE);
			golem.interact(mc.player);
			mc.player.inventory.main[mc.player.inventory.selectedSlot] = null;
			park(golem);
		});
		for (boolean smooth : new boolean[] {true, false}) {
			then(() -> {
				mc.options.ao = smooth;
				mc.worldRenderer.reload();
			});
			view("look-pole-torches-" + (smooth ? "smooth" : "flat"), 1.6, 2.0, 0.6, 40);
		}
		then(() -> {
			boolean lit = true;
			for (int[] c : new int[][] {{-3, -3}, {3, -3}, {-3, -9}, {3, -9}}) lit &= mc.world.getBlockId(fx + c[0], fy, fz + c[1]) == Golems.poleLit.get().id;
			check("a golem's four poles lit among the torches", lit);
			mc.options.ao = true;
			golem.interact(mc.player);
			mc.player.inventory.main[mc.player.inventory.selectedSlot] = null;
			for (int[] c : new int[][] {{-3, -3}, {3, -3}, {-3, -9}, {3, -9}, {-2, -5}, {3, -5}}) mc.world.setBlock(fx + c[0], fy, fz + c[1], 0);
			// The first scene's poles again, for the levels below.
			mc.world.setBlock(fx - 2, fy, fz - 1, Golems.pole.get().id);
			mc.world.setBlock(fx, fy, fz - 1, Golems.pole.get().id);
			for (int[] c : new int[][] {{6, -1}, {0, -7}, {6, -7}}) mc.world.setBlock(fx + c[0], fy - 2, fz + c[1], Golems.pole.get().id);
			mc.world.setBlock(fx + 2, fy, fz - 1, Block.LIT_REDSTONE_TORCH.id);
			put(golem, fx + 3.5, fz - 3.5, FRONT);
			mc.player.inventory.main[mc.player.inventory.selectedSlot] = new ItemStack(Item.IRON_HOE);
			golem.interact(mc.player);
			mc.player.inventory.main[mc.player.inventory.selectedSlot] = null;
			park(golem);
		});
		wait(2);
		// Its iron at other light levels, for choosing by eye beside the torch (7, the pole's own, is the ring alone lit; 15 the whole rod as a torch's stick). The chunk drawn again for each.
		for (int level : new int[] {7, 10, 13, 15}) {
			then(() -> {
				org.studioquire.faithful.golems.client.LitPoleShape.shaftLight = level;
				mc.worldRenderer.blockUpdate(fx, fy, fz - 1);
			});
			view("look-pole-night-shaft-" + level, 1.2, 2.4, 0.35, 30);
		}
		then(() -> {
			org.studioquire.faithful.golems.client.LitPoleShape.shaftLight = org.studioquire.faithful.golems.client.LitPoleShape.SHAFT_LIGHT;
			mc.worldRenderer.blockUpdate(fx, fy, fz - 1);
		});
		then(() -> {
			int pole = mc.world.getBrightness(net.minecraft.world.LightType.BLOCK, fx, fy, fz - 1), torch = mc.world.getBrightness(net.minecraft.world.LightType.BLOCK, fx + 2, fy, fz - 1);
			check("the pole a golem uses was lit, with a lit redstone torch's light (" + pole + " and the torch's " + torch + ")", mc.world.getBlockId(fx, fy, fz - 1) == Golems.poleLit.get().id && pole == 7 && torch == 7);
			check("and the one no golem uses wasn't", mc.world.getBlockId(fx - 2, fy, fz - 1) == Golems.pole.get().id);
			golem.interact(mc.player);
			mc.player.inventory.main[mc.player.inventory.selectedSlot] = null;
			for (int i = -2; i <= 2; i++) mc.world.setBlock(fx + i, fy, fz - 1, 0);
			for (int[] c : new int[][] {{6, -1}, {0, -7}, {6, -7}}) mc.world.setBlock(fx + c[0], fy - 2, fz + c[1], Block.DIRT.id);
			mc.world.setTime(6000L);
		});

		then(() -> {
			for (Entity e : cast) mc.world.remove(e);
			cast.clear();
			mc.world.difficulty = difficulty;
			mc.options.hideHud = false;
			check("the look was pictured", true);
		});
	}

	/** A flat floor with nothing on it, and for the quarry a wall behind where they stand. */
	private void stage(int floor, boolean wall) {
		for (int i = -12; i <= 12; i++) {
			for (int j = -12; j <= 12; j++) {
				mc.world.setBlock(fx + i, fy - 2, fz + j, floor == Block.STONE.id ? floor : Block.DIRT.id);
				mc.world.setBlock(fx + i, fy - 1, fz + j, floor);
				for (int k = 0; k < 8; k++) mc.world.setBlock(fx + i, fy + k, fz + j, wall && j == -3 && k < 5 ? Block.STONE.id : 0);
			}
		}
	}

	private void join(Entity e) {
		e.setPositionAndAngles(cx, fy, cz, 0.0F, 0.0F);
		mc.world.spawnEntity(e);
		cast.add(e);
	}

	/** The zombie, the golem and the player in a row, all turned the same way. */
	private void three(String name, float yaw, Item tool, int ticks) {
		then(() -> {
			golem.setTool(tool == null ? null : new ItemStack(tool));
			put(zombie, cx - APART, cz, yaw);
			put(golem, cx, cz, yaw);
			put(player, cx + APART, cz, yaw);
		});
		view("look-" + name, 1.2, BACK, 0.95, ticks);
	}

	/** The golem by itself, near. */
	private void alone(String name, float yaw, Item tool, int ticks) {
		then(() -> {
			golem.setTool(tool == null ? null : new ItemStack(tool));
			park(zombie);
			put(golem, cx, cz, yaw);
			park(player);
		});
		view("look-" + name, 1.15, 1.75, 0.85, ticks);
	}

	/** The golem by itself, near, drawing a bow with its head this far down. */
	private void aiming(String name, float yaw, float pitch) {
		then(() -> {
			golem.setTool(new ItemStack(Item.BOW));
			park(zombie);
			put(golem, cx, cz, yaw);
			golem.pitch = golem.prevPitch = pitch;
			park(player);
		});
		view("look-" + name, 1.15, 1.75, 0.85, 20);
	}

	/** Four golems, a tool each: the pickaxe it takes now, and the three it's meant to take. */
	private void crew(String name, int ticks) {
		Item[] tools = {Item.IRON_PICKAXE, Item.IRON_AXE, Item.IRON_HOE, Item.IRON_SWORD};
		List<Entity> extras = new ArrayList<>();
		then(() -> {
			park(zombie);
			park(player);
			for (int i = 0; i < tools.length; i++) {
				Still g = i == 0 ? golem : new Still(mc.world);
				if (i > 0) {
					join(g);
					extras.add(g);
				}
				g.setTool(new ItemStack(tools[i]));
				put(g, cx + (i - 1.5) * 1.15, cz, -25.0F);
			}
		});
		view("look-" + name, 1.2, BACK, 0.9, ticks);
		then(() -> {
			for (Entity e : extras) mc.world.remove(e);
			cast.removeAll(extras);
		});
	}

	/** Out of the picture: high over the camera's head, in open air, since a zombie left inside a hill suffocates there. */
	private void park(LivingEntity e) {
		put(e, cx, cz + 6.0, FRONT);
		e.setPosition(e.x, fy + 40.0, e.z);
		e.prevY = e.lastTickY = e.y;
	}

	private void put(LivingEntity e, double x, double z, float yaw) {
		e.setPositionAndAngles(x, fy, z, yaw, 0.0F);
		e.prevY = e.lastTickY = e.y;
		e.prevX = e.lastTickX = x;
		e.prevZ = e.lastTickZ = z;
		e.prevYaw = yaw;
		e.bodyYaw = e.lastBodyYaw = yaw;
	}

	/** Mid-step, as if walking at this much of a full stride. */
	private static void stride(LivingEntity e, float stride) {
		e.lastWalkAnimationSpeed = e.walkAnimationSpeed = stride;
		e.walkAnimationProgress = stride * 0.5F;
	}

	/** The player held in the air as the camera, south of the middle and looking north at it, until the land has drawn; then a picture. Heights are above the floor. */
	private void view(String name, double eye, double back, double at, int ticks) {
		float pitch = (float) (Math.atan2(eye - at, back) * 180.0 / Math.PI);
		int[] n = {0};
		await("the view to fill in", ticks + 20, () -> {
			mc.player.setPositionAndAngles(cx, fy + eye, cz + back, 180.0F, pitch);
			mc.player.velocityX = mc.player.velocityY = mc.player.velocityZ = 0.0;
			return ++n[0] >= ticks;
		});
		picture(name);
	}
}
