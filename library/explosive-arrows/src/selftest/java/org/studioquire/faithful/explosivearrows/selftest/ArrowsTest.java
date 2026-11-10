package org.studioquire.faithful.explosivearrows.selftest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipeManager;
import net.minecraft.screen.ScreenHandler;
import org.studioquire.faithful.explosivearrows.ExplosiveArrow;
import org.studioquire.faithful.explosivearrows.ExplosiveArrows;
import org.studioquire.faithful.explosivearrows.selftest.server.Pvp;
import org.studioquire.faithful.harness.test.WorldTest;

/**
 * Where the world runs, each case at once in one tick: the arrow is flown by its own tick, called until it's spent, so nothing else moves meanwhile and the
 * test server's Peaceful never gets to take the zombies. Every burst is four blocks or more from the player but the one meant for them. The client's real
 * shots, its copy of the arrow and the pictures are {@code ArrowsClientTest}.
 */
public final class ArrowsTest extends WorldTest {
	private Item explosive;

	private ItemStack crafted(int side, Item... cells) {
		CraftingInventory grid = new CraftingInventory(new ScreenHandler() {
			@Override
			public boolean canUse(PlayerEntity player) {
				return true;
			}
		}, side, side);
		for (int i = 0; i < cells.length; i++) if (cells[i] != null) grid.setStack(i, new ItemStack(cells[i]));
		return CraftingRecipeManager.getInstance().craft(grid);
	}

	private boolean makesOne(ItemStack made) {
		return made != null && made.getItem() == explosive && made.count == 1;
	}

	private void recipes() {
		Item arrow = Item.ARROW, powder = Item.GUNPOWDER;
		check("an arrow and two gunpowder make one explosive arrow, in the inventory's grid", makesOne(crafted(2, powder, null, powder, arrow)));
		check("and in any cells of the bench", makesOne(crafted(3, null, null, arrow, null, powder, null, null, powder)));
		check("not one gunpowder to an arrow", crafted(2, arrow, powder) == null);
		check("nor three", crafted(2, arrow, powder, powder, powder) == null);
		check("nor two arrows to two gunpowder", crafted(2, arrow, arrow, powder, powder) == null);
		check("nor an arrow alone, or gunpowder alone", crafted(2, arrow) == null && crafted(2, powder, powder) == null);
		check("an explosive arrow stacks as an arrow does", explosive.getMaxCount() == Item.ARROW.getMaxCount());
	}

	private <T extends Entity> List<T> live(Class<T> kind) {
		List<T> found = new ArrayList<>();
		for (Object e : world.entities) {
			Entity entity = (Entity) e;
			if (kind.isInstance(entity) && !entity.dead && Math.abs(entity.x - x) < 16 && Math.abs(entity.z - z) < 16) found.add(kind.cast(entity));
		}
		return found;
	}

	private <T extends Entity> T placed(T entity, double dx, double dz) {
		entity.setPositionAndAngles(x + dx, y, z + dz, 0.0F, 0.0F);
		world.spawnEntity(entity);
		return entity;
	}

	/** One as a bow's leaves it, from an offset off the plot's corner, at a bow's speed and with none of its spread. */
	private ExplosiveArrow loosed(double dx, double dy, double dz, double vx, double vy, double vz, boolean shot) {
		ExplosiveArrow arrow = new ExplosiveArrow(world, x + dx, y + dy, z + dz);
		if (shot) arrow.owner = player;
		arrow.setVelocity(vx, vy, vz, 1.5F, 0.0F);
		world.spawnEntity(arrow);
		return arrow;
	}

	/** Its ticks, at once, until it's spent: twenty are far more than anything on the plot needs. */
	private boolean flown(ArrowEntity arrow) {
		for (int i = 0; i < 20 && !arrow.dead; i++) arrow.tick();
		return arrow.dead;
	}

	/** A zombie struck, a wolf a block and a half from it, another zombie three and a half off. */
	private void directHit() {
		ZombieEntity struck = placed(new ZombieEntity(world), 5.5, 0.5), clear = placed(new ZombieEntity(world), 5.5, 4.0);
		WolfEntity beside = placed(new WolfEntity(world), 5.5, 2.0);
		int struckWas = struck.health, besideWas = beside.health;
		player.health = 20;
		ExplosiveArrow arrow = loosed(2.5, 1.0, 0.5, 1, 0, 0, true);
		check("it bursts on the zombie in its way", flown(arrow));
		int took = struckWas - struck.health, wolfTook = besideWas - beside.health;
		check("a direct hit takes over half a zombie's health and leaves it standing: " + took + " of " + struckWas, took * 2 > struckWas && struck.health > 0);
		check("the wolf a block and a half off loses about a couple of hearts: " + wolfTook, wolfTook >= 3 && wolfTook <= 7);
		check("and turns on the shooter, as at an arrow's own hit: the blast is theirs", beside.isAngry());
		check("nothing three and a half blocks off is touched: " + clear.health, clear.health == clear.maxHealth);
		check("nothing's left of the arrow", live(ExplosiveArrow.class).isEmpty());
		check("the player, four blocks and more off, isn't hurt: " + player.health, player.health == 20);
		struck.markDead();
		clear.markDead();
		beside.markDead();
	}

	/** Into a glass wall, glass being what beta's blasts break easiest, with TNT beside where it lands and TNT out of reach. */
	private void blocksAndTnt() {
		for (int dy = 0; dy < 3; dy++) for (int dz = -1; dz <= 1; dz++) set(-6, dy, dz, Block.GLASS);
		set(-5, 1, 1, Block.TNT);
		set(-5, 1, 5, Block.TNT);
		ExplosiveArrow arrow = loosed(-2.0, 1.5, 0.5, -1, 0, 0, true);
		check("it bursts on the wall", flown(arrow));
		boolean glass = true;
		for (int dy = 0; dy < 3; dy++) for (int dz = -1; dz <= 1; dz++) glass &= block(-6, dy, dz) == Block.GLASS.id;
		check("the glass is untouched", glass);
		List<TntEntity> lit = live(TntEntity.class);
		TntEntity primed = null;
		for (TntEntity t : lit) if (Math.abs(t.x - (x - 4.5)) < 1 && Math.abs(t.y - (y + 1.5)) < 1.5 && Math.abs(t.z - (z + 1.5)) < 1) primed = t;
		check("the TNT beside it is lit, on a blast's short fuse: " + (primed == null ? "none" : primed.fuse + " ticks"), block(-5, 1, 1) == 0 && primed != null && primed.fuse >= 9 && primed.fuse < 30);
		check("and the TNT out of reach isn't", block(-5, 1, 5) == Block.TNT.id && lit.size() == 1);
		for (TntEntity t : lit) t.markDead();
		for (int dy = 0; dy < 3; dy++) for (int dz = -1; dz <= 1; dz++) set(-6, dy, dz, null);
		set(-5, 1, 5, null);
	}

	/** Straight down onto the floor a block and a half from the player, who shot it; on a server with PvP off for the moment, which mustn't spare them. */
	private void shooterHurt() {
		boolean pvpWas = true;
		if (!singlePlayer()) pvpWas = Pvp.set(player, false);
		player.health = 20;
		player.hearts = 0;
		ExplosiveArrow arrow = loosed(2.0, 2.5, 0.5, 0, -1, 0, true);
		check("it bursts on the floor", flown(arrow));
		check("the shooter, a block and a half off, is hurt by their own arrow" + (singlePlayer() ? "" : ", PvP off as it is") + ": " + (20 - player.health), player.health < 20 && player.health > 12);
		if (!singlePlayer()) Pvp.set(player, pvpWas);
		player.health = 20;
	}

	private int count(Item item) {
		int n = 0;
		for (ItemStack s : player.inventory.main) if (s != null && s.getItem() == item) n += s.count;
		return n;
	}

	/** Draws the bow once, as a right click does where the world runs, and says what it shot; the arrow goes at once. */
	private ArrowEntity draw() {
		List<ArrowEntity> before = live(ArrowEntity.class);
		ItemStack bow = player.inventory.main[0];
		player.inventory.selectedSlot = 0;
		bow.use(world, player);
		ArrowEntity shot = null;
		for (ArrowEntity a : live(ArrowEntity.class)) if (!before.contains(a)) shot = a;
		if (shot != null) shot.markDead();
		return shot;
	}

	private void bow() {
		Arrays.fill(player.inventory.main, null);
		player.inventory.main[0] = new ItemStack(Item.BOW);
		player.inventory.main[1] = new ItemStack(explosive, 2);
		player.inventory.main[2] = new ItemStack(Item.ARROW, 3);
		ArrowEntity shot = draw();
		check("the bow shoots the first arrows in the hotbar, explosive ones here", shot instanceof ExplosiveArrow && ((ExplosiveArrow) shot).owner == player
				&& count(explosive) == 1 && count(Item.ARROW) == 3);
		player.inventory.main[1] = new ItemStack(Item.ARROW, 3);
		player.inventory.main[2] = new ItemStack(explosive, 1);
		shot = draw();
		check("plain ones when they come first", shot != null && !(shot instanceof ExplosiveArrow) && count(Item.ARROW) == 2 && count(explosive) == 1);
		player.inventory.main[1] = null;
		player.inventory.main[2] = null;
		player.inventory.main[9] = new ItemStack(Item.ARROW, 2);
		player.inventory.main[8] = new ItemStack(explosive, 1);
		shot = draw();
		check("the hotbar's last slot comes before the inventory's first", shot instanceof ExplosiveArrow && count(explosive) == 0 && count(Item.ARROW) == 2);
		shot = draw();
		check("and with only plain ones left, plain ones", shot != null && !(shot instanceof ExplosiveArrow) && count(Item.ARROW) == 1);
		Arrays.fill(player.inventory.main, null);
		player.currentScreenHandler.sendContentUpdates();
	}

	/** A dispenser four blocks off facing away from the player, a glass wall beyond it, a redstone torch beside it for a moment. */
	private void dispenser() {
		set(4, 0, -3, Block.DISPENSER);
		// Placing it faced it by its neighbours; this faces it north, away from the player.
		world.setBlockMeta(x + 4, y, z - 3, 2);
		DispenserBlockEntity held = (DispenserBlockEntity) world.getBlockEntity(x + 4, y, z - 3);
		held.setStack(0, new ItemStack(explosive, 2));
		for (int dx = 3; dx <= 5; dx++) for (int dy = 0; dy < 3; dy++) set(dx, dy, -8, Block.GLASS);
		List<ExplosiveArrow> before = live(ExplosiveArrow.class);
		set(5, 0, -3, Block.LIT_REDSTONE_TORCH, 5);
		Block.DISPENSER.onTick(world, x + 4, y, z - 3, world.random);
		set(5, 0, -3, null);
		ExplosiveArrow shot = null;
		for (ExplosiveArrow a : live(ExplosiveArrow.class)) if (!before.contains(a)) shot = a;
		boolean dropped = false;
		for (ItemEntity item : live(ItemEntity.class)) dropped |= item.stack.getItem() == explosive;
		check("a powered dispenser shoots one as an arrow, not out as an item", shot != null && shot.owner == null && !dropped && held.getStack(0) != null && held.getStack(0).count == 1);
		if (shot != null) check("it bursts on the wall, which stands", flown(shot) && block(4, 1, -8) == Block.GLASS.id && block(4, 0, -8) == Block.GLASS.id);
		held.setStack(0, null);
		set(4, 0, -3, null);
		for (int dx = 3; dx <= 5; dx++) for (int dy = 0; dy < 3; dy++) set(dx, dy, -8, null);
	}

	private void leaveForRestart() {
		Arrays.fill(player.inventory.main, null);
		player.inventory.main[0] = new ItemStack(explosive, 7);
		player.currentScreenHandler.sendContentUpdates();
	}

	private void scenes() {
		// The bow and both arrows in the hotbar, explosive ones first, and a stone wall seven blocks west for the shot.
		scene("shoot", a -> {
			Arrays.fill(player.inventory.main, null);
			player.inventory.main[0] = new ItemStack(Item.BOW);
			player.inventory.main[1] = new ItemStack(explosive, 3);
			player.inventory.main[2] = new ItemStack(Item.ARROW, 5);
			player.inventory.selectedSlot = 0;
			player.currentScreenHandler.sendContentUpdates();
			for (int dy = 0; dy < 4; dy++) for (int dz = -2; dz <= 2; dz++) set(-7, dy, dz, Block.STONE);
			player.health = 20;
		});
		scene("shot", a -> {
			check("where the world runs: one explosive arrow gone from the hotbar, the plain ones there", count(explosive) == 2 && count(Item.ARROW) == 5);
			check("and spent on the wall, which stands", live(ExplosiveArrow.class).isEmpty() && block(-7, 1, 0) == Block.STONE.id && block(-7, 2, 0) == Block.STONE.id);
			check("the shooter, six blocks off, unhurt", player.health == 20);
			for (int dy = 0; dy < 4; dy++) for (int dz = -2; dz <= 2; dz++) set(-7, dy, dz, null);
		});
		scene("row", a -> {
			Arrays.fill(player.inventory.main, null);
			player.inventory.main[0] = new ItemStack(Item.BOW);
			player.inventory.main[1] = new ItemStack(Item.ARROW, 12);
			player.inventory.main[2] = new ItemStack(explosive, 12);
			player.inventory.main[3] = new ItemStack(Item.GUNPOWDER, 5);
			player.inventory.main[9] = new ItemStack(explosive, 64);
			player.inventory.main[10] = new ItemStack(Item.ARROW, 64);
			player.inventory.selectedSlot = 2;
			player.currentScreenHandler.sendContentUpdates();
			world.setTime(6000);
		});
		// A burst in the open 24 blocks east and 24 up, some 34 from the player: past beta's 16 for every puff, within the 64 they're drawn to here.
		scene("far burst", a -> {
			// Beta moves an entity only with every chunk within 32 blocks of it loaded; away from the spawn, the test server's view of 3 chunks round the
			// player needn't reach so far east, and the arrow would hang there.
			for (int cx = x + 24 - 32 >> 4; cx <= x + 24 + 32 >> 4; cx++) for (int cz = z - 32 >> 4; cz <= z + 32 >> 4; cz++) world.getChunkSource().loadChunk(cx, cz);
			set(24, 23, 0, Block.STONE);
			loosed(24.5, 25.5, 0.5, 0, -1, 0, true);
		});
		scene("far burst over", a -> {
			check("the far arrow burst on its block", live(ExplosiveArrow.class).isEmpty() && block(24, 23, 0) == Block.STONE.id);
			set(24, 23, 0, null);
		});
		scene("leave", a -> leaveForRestart());
	}

	@Override
	protected void script() {
		explosive = ExplosiveArrows.explosiveArrow.get();
		scenes();
		if (restarted()) {
			check("explosive arrows came through the restart by name", player.inventory.main[0] != null && player.inventory.main[0].getItem() == explosive && player.inventory.main[0].count == 7);
			return;
		}
		recipes();
		then(this::directHit);
		then(this::blocksAndTnt);
		// A server spares a player for three seconds after they join, whoever hurts them: until a scratch lands.
		if (!singlePlayer()) await("the player past the join's protection", 100, () -> {
			if (!player.damage(null, 1)) return false;
			player.health = 20;
			player.hearts = 0;
			return true;
		});
		then(this::shooterHurt);
		then(this::bow);
		then(this::dispenser);
		then(this::leaveForRestart);
	}
}
