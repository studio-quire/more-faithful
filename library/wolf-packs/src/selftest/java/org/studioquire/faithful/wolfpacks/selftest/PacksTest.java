package org.studioquire.faithful.wolfpacks.selftest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.PigZombieEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.studioquire.faithful.harness.test.WorldTest;
import org.studioquire.faithful.wolfpacks.Packs;
import org.studioquire.faithful.wolfpacks.Watch;
import org.studioquire.faithful.wolfpacks.WolfFood;
import org.studioquire.faithful.wolfpacks.WolfPacks;
import org.studioquire.oldfaithful.core.impl.Rules;

/**
 * Where the world runs, with real ticks: wolves, pig zombies (which don't burn by day, unlike zombies) and creepers on the plot, the call to arms by the
 * player's own punch. The selftest server is Peaceful, which takes monsters on their first tick, so it's Normal for the test; single player's comes from
 * the options. The player is kept whole throughout, as the pig zombies go for them. Then a sitting wolf's watch ({@link #watch}).
 */
public final class PacksTest extends WorldTest {
	private final List<Entity> ours = new ArrayList<>();
	private int difficultyBefore;
	private WolfEntity[] wolves;
	private PigZombieEntity a, b, c;
	private WolfEntity onA;

	private <T extends Entity> T placed(T entity, double dx, double dz) {
		entity.setPositionAndAngles(x + dx + 0.5, y, z + dz + 0.5, 0.0F, 0.0F);
		world.spawnEntity(entity);
		ours.add(entity);
		return entity;
	}

	private WolfEntity wolf(double dx, double dz, boolean sitting) {
		WolfEntity wolf = new WolfEntity(world);
		wolf.setTamed(true);
		wolf.setOwnerName(player.name);
		wolf.setSitting(sitting);
		wolf.health = 20;
		return placed(wolf, dx, dz);
	}

	/** A pig zombie going for the player. */
	private PigZombieEntity after(double dx, double dz) {
		PigZombieEntity pig = placed(new PigZombieEntity(world), dx, dz);
		pig.setTarget(player);
		return pig;
	}

	private void keepWell() {
		player.health = 20;
	}

	private Set<Entity> targets() {
		Set<Entity> set = new HashSet<>();
		for (WolfEntity w : wolves) set.add(w.getTarget());
		return set;
	}

	private int on(Entity enemy) {
		int n = 0;
		for (WolfEntity w : wolves) if (w.getTarget() == enemy) n++;
		return n;
	}

	private String described() {
		StringBuilder s = new StringBuilder();
		for (WolfEntity w : wolves) {
			Entity t = w.getTarget();
			s.append(s.length() == 0 ? "" : ", ").append(t == a ? "A" : t == b ? "B" : t == c ? "C" : t == null ? "none" : t.getClass().getSimpleName());
		}
		return s.toString();
	}

	/** The pig zombies gone, and the wolves let go of them at once, as beta's call takes only wolves with no target. */
	private void clearMonsters() {
		for (Entity e : ours) if (!(e instanceof WolfEntity)) e.markDead();
		if (wolves != null) for (WolfEntity w : wolves) w.setTarget(null);
	}

	@Override
	protected void script() {
		clientScenes();
		if (restarted()) {
			check("the rules on after the restart, as the first life left them", WolfPacks.spreadOut.get() && WolfPacks.fightCreepers.get() && WolfPacks.feeding.get() && WolfPacks.guarding.get() && WolfPacks.watch.get());
			return;
		}
		difficultyBefore = world.difficulty;
		if (!singlePlayer()) world.difficulty = 2;
		check("monsters can live here (difficulty " + world.difficulty + ")", world.difficulty > 0);
		check("the rules on by default", WolfPacks.spreadOut.get() && WolfPacks.fightCreepers.get() && WolfPacks.feeding.get() && WolfPacks.guarding.get() && WolfPacks.watch.get());
		spreading();
		oneEnemy();
		ruleOff();
		guarding();
		creepers();
		watch();
		feeding();
		then(() -> {
			for (Entity e : ours) e.markDead();
			if (!singlePlayer()) world.difficulty = difficultyBefore;
		});
	}

	/** Three wolves just south of the player, three pig zombies six blocks off on three sides; the player punches the one to the north. */
	private void spreading() {
		then(() -> {
			wolves = new WolfEntity[] {wolf(-1, 1, false), wolf(0, 1.5, false), wolf(1, 1, false)};
			a = after(0, -6);
			b = after(-6, 3);
			c = after(6, 3);
		});
		wait(2);
		then(() -> {
			keepWell();
			b.setTarget(player);
			c.setTarget(player);
			player.attack(a);
			check("three wolves, three monsters after the player: at the call each wolf takes a different one (" + described() + ")",
					targets().equals(new HashSet<>(Arrays.asList(a, b, c))));
			check("the one punched among them", on(a) == 1);
			for (WolfEntity w : wolves) if (w.getTarget() == a) onA = w;
			check("and the pack is in a fight", Packs.fighting(player));
			a.markDead();
		});
		until("when its monster dies, a wolf takes the next with no call", 40, () -> {
			keepWell();
			Entity t = onA == null ? null : onA.getTarget();
			return t == b || t == c;
		});
		then(() -> check("the next it took was one of the two left, which now have two wolves and one (" + described() + ")",
				on(b) + on(c) == 3 && Math.abs(on(b) - on(c)) == 1));
	}

	/** One pig zombie: every wolf goes for it, as in beta. */
	private void oneEnemy() {
		then(this::clearMonsters);
		wait(2);
		then(() -> {
			keepWell();
			a = after(0, -6);
			b = c = null;
		});
		wait(2);
		then(() -> {
			keepWell();
			for (WolfEntity w : wolves) w.setTarget(null);
			player.attack(a);
			check("with one monster, the whole pack goes for it at the call (" + described() + ")", on(a) == 3);
		});
	}

	/** Packs Spread Out off: beta's call, every wolf on the punched one though others are after the player. */
	private void ruleOff() {
		then(() -> {
			clearMonsters();
			check("the rule turns off", Rules.change(WolfPacks.spreadOut, "false") && !WolfPacks.spreadOut.get());
		});
		wait(2);
		then(() -> {
			keepWell();
			a = after(0, -6);
			b = after(-6, 3);
			c = after(6, 3);
		});
		wait(2);
		then(() -> {
			keepWell();
			for (WolfEntity w : wolves) w.setTarget(null);
			b.setTarget(player);
			c.setTarget(player);
			player.attack(a);
			check("off, every wolf goes for the one punched, as in beta (" + described() + ")", on(a) == 3);
			check("the rule turns back on", Rules.change(WolfPacks.spreadOut, "true") && WolfPacks.spreadOut.get());
			clearMonsters();
			for (WolfEntity w : wolves) w.markDead();
		});
		wait(2);
	}

	private WolfEntity seated;
	private PigZombieEntity coming;
	private SkeletonEntity archer;
	private SpiderEntity idle;
	private double takenAt;

	/** A monster that can't strike for now, so no hit calls the pack and none knocks the player off their spot. */
	private <T extends net.minecraft.entity.LivingEntity> T harmless(T monster, double dx, double dz) {
		besidePlayer(monster, dx, dz).attackCooldown = 1000;
		return monster;
	}

	/** Placed by the player wherever they stand now, as an earlier push can have moved them off the plot's middle, which a 4-block reach notices. */
	private <T extends Entity> T besidePlayer(T entity, double dx, double dz) {
		return placed(entity, player.x - x - 0.5 + dx, player.z - z - 0.5 + dz);
	}

	/**
	 * Two standing wolves and a sitting one by the player, no punch and no hit: a pig zombie after the player from 7 blocks left alone until it's within 4; a
	 * skeleton 7 blocks off left alone until it shoots, then taken though its arrow is taken out of the air (so no hit calls the pack); a spider 3 blocks off left alone while it's after no one, and taken once
	 * it's after the player; and with the rule off, that spider left alone. Each with the last fight over first.
	 */
	private void guarding() {
		// The last fight's 3 seconds over, or it would take anything after the player, as a fight goes on.
		wait(70);
		then(() -> {
			keepWell();
			check("no fight going on to begin with", !Packs.fighting(player));
			// Guarding by itself: a sitting wolf's own watch would take these monsters from its seat.
			Rules.change(WolfPacks.watch, "false");
			wolves = new WolfEntity[] {wolf(-1, 1, false), wolf(1, 1, false)};
			seated = wolf(0, 2, true);
			coming = besidePlayer(new PigZombieEntity(world), 0, -7);
			coming.setTarget(player);
		});
		wait(8);
		then(() -> check("a monster after you further than 4 blocks off is left alone while it comes (" + String.format("%.1f", Packs.feetApart(coming, player)) + " blocks)",
				on(coming) == 0 && Packs.feetApart(coming, player) > 4));
		until("once it's within 4 blocks a wolf goes for it", 300, () -> {
			keepWell();
			takenAt = Packs.feetApart(coming, player);
			return on(coming) > 0;
		});
		then(() -> {
			check("taken " + String.format("%.1f", takenAt) + " blocks off, before it could strike", takenAt <= 4.5 && takenAt > 2);
			check("the sitting wolf stays seated, with nothing to do (Sitting Wolves Watch off)", seated.isInSittingPose() && seated.getTarget() == null);
			clearMonsters();
		});
		wait(70);
		then(() -> {
			keepWell();
			archer = besidePlayer(new SkeletonEntity(world), 0, 7);
			archer.setTarget(player);
			archer.attackCooldown = 1000;
		});
		wait(12);
		then(() -> {
			check("a skeleton after you 7 blocks off that hasn't shot is left alone", on(archer) == 0);
			archer.attackCooldown = 0;
		});
		until("it shoots at you", 40, () -> {
			for (Object o : world.entities) {
				if (o instanceof net.minecraft.entity.projectile.ArrowEntity && ((net.minecraft.entity.projectile.ArrowEntity) o).owner == archer) {
					// Taken out of the air at once, so it can't hit and call the pack beta's way: a miss.
					((Entity) o).markDead();
					archer.attackCooldown = 1000;
					return true;
				}
			}
			return false;
		});
		until("a skeleton that has shot at you, though it missed, gets a wolf from 7 blocks", 20, () -> on(archer) > 0);
		then(() -> {
			check("and you weren't hit", player.health == 20);
			clearMonsters();
		});
		wait(70);
		then(() -> idle = harmless(new SpiderEntity(world), 0, -3));
		wait(12);
		then(() -> {
			check("a spider 3 blocks off that hasn't noticed you is left alone" + (idle.getTarget() == null ? "" : " (it noticed you: dark here)"), idle.getTarget() != null || on(idle) == 0);
			idle.setTarget(player);
		});
		until("once it's after you, a wolf goes for it", 30, () -> {
			keepWell();
			return on(idle) > 0;
		});
		then(() -> {
			clearMonsters();
			check("the rule turns off", Rules.change(WolfPacks.guarding, "false") && !WolfPacks.guarding.get());
		});
		wait(70);
		then(() -> {
			idle = harmless(new SpiderEntity(world), 0, -3);
			idle.setTarget(player);
		});
		wait(12);
		then(() -> {
			check("off, a spider after you 3 blocks away is left alone until it strikes or you do, as in beta", on(idle) == 0);
			check("the rule turns back on", Rules.change(WolfPacks.guarding, "true") && WolfPacks.guarding.get());
			Rules.change(WolfPacks.watch, "true");
			clearMonsters();
			for (WolfEntity w : wolves) w.markDead();
			seated.markDead();
		});
	}

	private WolfEntity sentry;
	private double[] spot;
	private PigZombieEntity calm, far, high, boxed, lure;
	private int growlsBefore;
	private double strayed;

	/** On the floor of the plot's cell or, for the top of a pillar, {@code dy} above it. */
	private <T extends Entity> T stood(T entity, int dx, int dy, int dz) {
		entity.setPositionAndAngles(x + dx + 0.5, y + dy, z + dz + 0.5, 0.0F, 0.0F);
		world.spawnEntity(entity);
		ours.add(entity);
		return entity;
	}

	/** A pig zombie after the player that can't strike for now: a monster a wolf on watch minds, and one that doesn't burn by day. */
	private PigZombieEntity hostile(int dx, int dy, int dz) {
		PigZombieEntity pig = stood(new PigZombieEntity(world), dx, dy, dz);
		pig.setTarget(player);
		pig.attackCooldown = 1000;
		return pig;
	}

	private void pillar(int dx, int dz, Block block) {
		for (int dy = 0; dy < 4; dy++) set(dx, dy, dz, block);
	}

	private WolfEntity seated(int dx, int dz) {
		sentry = wolf(dx, dz, true);
		spot = new double[] {sentry.x, sentry.boundingBox.minY, sentry.z};
		return sentry;
	}

	private double fromSpot(WolfEntity wolf) {
		double dx = wolf.x - spot[0], dz = wolf.z - spot[2];
		return Math.sqrt(dx * dx + dz * dz);
	}

	private boolean stillSeated() {
		return sentry.isInSittingPose() && sentry.getTarget() == null && !Watch.onDuty(sentry);
	}

	/** Its monster gone, the wolf walks back onto the spot it left and sits. */
	private void backOnItsSpot(String what) {
		until(what, 300, () -> {
			keepWell();
			sentry.health = 20;
			return sentry.isInSittingPose() && !Watch.onDuty(sentry) && fromSpot(sentry) < 0.5;
		});
	}

	/**
	 * A sitting wolf 6 blocks west of the player, and pig zombies after the player for monsters. Left alone: a calm pig zombie 4 blocks off, a spider at
	 * peace by day, a monster on a pillar 12 blocks away, one shut in glass 3 blocks off. One on a pillar 6 blocks off, seen and out of reach: growled at
	 * from the seat. The glass taken away: gone for, and once it's dead the wolf back on its spot and sitting. One past tall grass, which beta's sight stops at: gone for. Lured across the plot after a monster that keeps
	 * ahead of it: let go past 16 blocks, and back. A creeper: bitten, it turns on the wolf; with Wolves Fight Creepers off, left alone. Hit on its
	 * spot: it fights and goes back. Written to NBT off its spot and read back as a new wolf: it still goes back. The rule off: beta's sitting wolf.
	 */
	private void watch() {
		wait(70);
		then(() -> {
			keepWell();
			seated(-6, 0);
			calm = stood(new PigZombieEntity(world), -6, 0, 4);
			idle = stood(new SpiderEntity(world), -6, 0, -4);
			idle.attackCooldown = 1000;
			pillar(5, -3, Block.STONE);
			far = hostile(5, 4, -3);
			for (int[] side : new int[][] {{-4, 0}, {-2, 0}, {-3, -1}, {-3, 1}}) for (int dy = 0; dy < 2; dy++) set(side[0], dy, side[1], Block.GLASS);
			boxed = hostile(-3, 0, 0);
			growlsBefore = Watch.growls();
		});
		wait(25);
		then(() -> {
			boolean atPeace = idle.getTarget() == null && idle.getBrightnessAtEyes(1.0F) >= 0.5F;
			check("a sitting wolf leaves alone a calm pig zombie 4 blocks off, a monster " + String.format("%.1f", sentry.getDistance(far)) + " blocks away, one behind glass 3 blocks off"
					+ (atPeace ? " and a spider at peace by day" : " (the spider wasn't at peace: dark here)") + ", and doesn't growl",
					(stillSeated() || !atPeace && sentry.getTarget() == idle) && Watch.growls() == growlsBefore && !Watch.alarmed(sentry));
			calm.markDead();
			idle.markDead();
			if (sentry.getTarget() == idle) {
				sentry.markDead();
				seated(-6, 0);
			}
			pillar(-6, -5, Block.STONE);
			high = hostile(-6, 4, -5);
		});
		until("one on a pillar 6.4 blocks off, which it sees and has no way to: it growls", 60, () -> {
			keepWell();
			return Watch.growls() > growlsBefore;
		});
		then(() -> {
			check("and stays sitting, roused", stillSeated() && Watch.alarmed(sentry));
			high.markDead();
			pillar(-6, -5, null);
			set(-4, 0, 0, null);
			set(-4, 1, 0, null);
		});
		until("the glass gone, it sees the monster 3 blocks off and goes for it", 30, () -> {
			keepWell();
			return sentry.getTarget() == boxed && !sentry.isInSittingPose();
		});
		then(() -> {
			check("off its seat it's on watch still, its spot the one it left",
					Watch.onDuty(sentry) && Watch.spot(sentry) != null && Math.abs(Watch.spot(sentry)[0] - spot[0]) + Math.abs(Watch.spot(sentry)[2] - spot[2]) < 0.01);
			for (int[] side : new int[][] {{-2, 0}, {-3, -1}, {-3, 1}}) for (int dy = 0; dy < 2; dy++) set(side[0], dy, side[1], null);
		});
		wait(15);
		then(() -> {
			note("on its monster, " + String.format("%.1f", fromSpot(sentry)) + " blocks from its spot");
			boxed.markDead();
		});
		backOnItsSpot("its monster dead, it walks back to its spot and sits");
		then(() -> {
			note("back within " + String.format("%.2f", fromSpot(sentry)) + " of its spot");
			set(-5, -1, 0, Block.DIRT);
			set(-5, 0, 0, Block.GRASS, 1);
			lure = hostile(-1, 0, 0);
			check("tall grass beside the wolf is in the line from its eyes to a monster 5 blocks off, as beta's mobs see",
					block(-5, 0, 0) == Block.GRASS.id && !sentry.canSee(lure));
		});
		until("and it sees the monster all the same, and goes for it", 30, () -> {
			keepWell();
			return sentry.getTarget() == lure;
		});
		then(() -> {
			lure.markDead();
			set(-5, 0, 0, null);
			set(-5, -1, 0, Block.STONE);
		});
		backOnItsSpot("and comes back from that one too");

		// The leash: from one corner of the plot toward the other, after a monster kept 6 blocks ahead of it.
		then(() -> {
			sentry.markDead();
			seated(-7, -7);
			lure = hostile(-4, 0, -7);
		});
		until("a wolf in the corner goes for a monster 3 blocks off", 30, () -> {
			keepWell();
			return sentry.getTarget() == lure;
		});
		until("the monster keeping 6 blocks ahead of it across the plot, it follows and then lets it go", 300, () -> {
			keepWell();
			strayed = fromSpot(sentry);
			if (sentry.getTarget() == null && Watch.onDuty(sentry)) return true;
			lure.setPositionAndAngles(Math.min(sentry.x + 4.2, x + 7.5), y, Math.min(sentry.z + 4.2, z + 7.5), 0.0F, 0.0F);
			return false;
		});
		then(() -> {
			check("let go " + String.format("%.1f", strayed) + " blocks from its spot, just past 16", strayed > 15.5 && strayed < 18.5);
			lure.markDead();
		});
		backOnItsSpot("and it walks back to its corner and sits");
		then(() -> {
			far.markDead();
			pillar(5, -3, null);
			sentry.markDead();
		});

		// Creepers, each gone before it's near enough to anyone to light its fuse.
		wait(2);
		then(() -> {
			keepWell();
			seated(-6, 0);
			creeper = stood(new CreeperEntity(world), -6, 0, -5);
			creeper.setTarget(player);
		});
		until("a sitting wolf goes for a creeper it sees 5 blocks off", 30, () -> sentry.getTarget() == creeper);
		then(() -> {
			creeper.damage(sentry, 4);
			check("bitten by a wolf on watch, the creeper turns on the wolf, as beta's, though it was after the player", creeper.getTarget() == sentry);
			creeper.markDead();
		});
		backOnItsSpot("and the wolf goes back to its spot");
		then(() -> {
			check("Wolves Fight Creepers turns off", Rules.change(WolfPacks.fightCreepers, "false") && !WolfPacks.fightCreepers.get());
			creeper = stood(new CreeperEntity(world), -6, 0, -5);
			creeper.setTarget(player);
		});
		wait(25);
		then(() -> {
			check("off, a sitting wolf leaves a creeper alone, and doesn't growl at it", stillSeated() && !Watch.alarmed(sentry));
			creeper.markDead();
			check("it turns back on", Rules.change(WolfPacks.fightCreepers, "true") && WolfPacks.fightCreepers.get());
			// Hit on its spot by something it wasn't minding.
			calm = stood(new PigZombieEntity(world), -6, 0, 2);
			sentry.damage(calm, 1);
			check("hit on its spot, it stands and turns on what hit it, as in beta, on watch still", !sentry.isInSittingPose() && sentry.getTarget() == calm && Watch.onDuty(sentry));
			calm.markDead();
		});
		backOnItsSpot("and that fight over, it goes back to its spot too");

		// Saved off its spot: the wolf written and read back as beta saves a chunk's, put down 6 blocks from its spot.
		then(() -> lure = hostile(-6, 0, 3));
		until("it goes for another", 30, () -> {
			keepWell();
			return sentry.getTarget() == lure;
		});
		then(() -> {
			lure.markDead();
			NbtCompound saved = new NbtCompound();
			sentry.write(saved);
			sentry.markDead();
			WolfEntity loaded = new WolfEntity(world);
			loaded.read(saved);
			loaded.setPositionAndAngles(x + 0.5, y, z + 3.5, 0.0F, 0.0F);
			world.spawnEntity(loaded);
			ours.add(loaded);
			sentry = loaded;
			check("a wolf saved off its spot and loaded again is on watch still, standing " + String.format("%.1f", fromSpot(sentry)) + " blocks from it",
					Watch.onDuty(sentry) && !sentry.isInSittingPose() && fromSpot(sentry) > 5);
		});
		backOnItsSpot("and walks back to the spot it left before the save");

		then(() -> {
			check("the rule turns off", Rules.change(WolfPacks.watch, "false") && !WolfPacks.watch.get());
			lure = hostile(-6, 0, 4);
		});
		wait(25);
		then(() -> {
			check("off, a sitting wolf leaves a monster 4 blocks off alone, as in beta", sentry.isInSittingPose() && sentry.getTarget() == null);
			check("the rule turns back on", Rules.change(WolfPacks.watch, "true") && WolfPacks.watch.get());
			lure.markDead();
			sentry.markDead();
		});
		wait(2);
	}

	private WolfEntity fed;
	private long fedAt;

	private static boolean hearty() {
		return FabricLoader.getInstance().isModLoaded("hearty-food");
	}

	private int pending(WolfEntity wolf) {
		return WolfFood.pending.get(wolf);
	}

	/** Two of {@code item} in hand and beta's own right click on the wolf, where the world runs: whether one went. */
	private boolean offer(WolfEntity wolf, Item item) {
		hold(new ItemStack(item, 2));
		wolf.interact(player);
		ItemStack left = player.inventory.getSelectedItem();
		return left != null && left.count == 1;
	}

	/** A sitting tamed wolf on 2 hearts, fed each food in turn; then full; then the rule off. With Hearty Food loaded, its healing over time instead. */
	private void feeding() {
		then(() -> {
			keepWell();
			fed = wolf(3, 3, true);
			fed.health = 4;
			if (!hearty()) {
				check("raw fish heals a wolf its own worth, 1 heart", offer(fed, Item.RAW_FISH) && fed.health == 6);
				check("cooked fish 2.5 hearts", offer(fed, Item.COOKED_FISH) && fed.health == 11);
				check("raw pork 1.5", offer(fed, Item.RAW_PORKCHOP) && fed.health == 14);
				check("cooked pork 4, up to a full bar", offer(fed, Item.COOKED_PORKCHOP) && fed.health == 20);
				check("at full health it eats nothing, as in beta, and the owner's click doesn't stand it up", !offer(fed, Item.COOKED_FISH) && fed.health == 20 && fed.isInSittingPose());
				fed.health = 10;
				check("bread isn't wolf food, and the click stands it up, as beta's", !offer(fed, Item.BREAD) && fed.health == 10 && !fed.isInSittingPose());
				hold(null);
				fed.interact(player);
				check("an empty hand sits it down, as beta's", fed.isInSittingPose());
			} else {
				boolean ate = offer(fed, Item.RAW_FISH);
				check("with Hearty Food, raw fish heals over time, half as much again: 1.5 hearts pending (" + pending(fed) + ")", ate && fed.health == 4 && pending(fed) == 3);
				ate = offer(fed, Item.COOKED_FISH);
				check("cooked fish adds 4 hearts to it (" + pending(fed) + ")", ate && fed.health == 4 && pending(fed) == 11);
				ate = offer(fed, Item.COOKED_PORKCHOP);
				check("cooked pork, 6 hearts, adds only the 2.5 still missing (" + pending(fed) + ")", ate && pending(fed) == 16);
				check("with a full bar on its way it eats nothing, as Hearty Food's cake, and stays sitting", !offer(fed, Item.RAW_PORKCHOP) && pending(fed) == 16 && fed.isInSittingPose());
				fedAt = fed.age;
			}
			fed.setSitting(true);
		});
		if (hearty()) {
			until("and heals half a heart a second", 80, () -> fed.health >= 6);
			then(() -> {
				long took = fed.age - fedAt;
				check("two half-hearts in " + took + " ticks, the rest still to come (" + fed.health + " + " + pending(fed) + ")", took >= 38 && took <= 42 && fed.health + pending(fed) == 20);
				fed.health = 20;
			});
			wait(2);
			then(() -> check("a full wolf has nothing pending", !WolfFood.pending.has(fed)));
		}
		then(() -> {
			fed.health = 4;
			check("the rule turns off", Rules.change(WolfPacks.feeding, "false") && !WolfPacks.feeding.get());
		});
		// Beta's feeding goes by the tail, which shows the health the wolf had at its last tick.
		wait(2);
		then(() -> {
			check("off, cooked pork heals a raw porkchop's worth at once, as in beta", offer(fed, Item.COOKED_PORKCHOP) && fed.health == 7 && pending(fed) == 0);
			fed.setSitting(true);
			check("and fish isn't eaten, and the click stands it up, as beta's", !offer(fed, Item.RAW_FISH) && fed.health == 7 && !fed.isInSittingPose());
			check("the rule turns back on", Rules.change(WolfPacks.feeding, "true") && WolfPacks.feeding.get());
			hold(null);
			fed.markDead();
		});
	}

	private WolfEntity clientWolf;

	/** For {@code FeedingClientTest}: a real right click with fish, from the client. */
	private void clientScenes() {
		scene("feeder", a -> {
			// Beside the player wherever they stand now: a knock in an earlier test can have moved them off the plot's middle.
			clientWolf = wolf(player.x - x - 0.5 + 2, player.z - z - 0.5, true);
			clientWolf.health = 10;
			hold(new ItemStack(Item.RAW_FISH, 3));
		});
		scene("fed", a -> {
			boolean healed = hearty() ? clientWolf.health + pending(clientWolf) == 13 : clientWolf.health == 12;
			check("where the world runs, the fish was eaten and heals the wolf (" + clientWolf.health + " + " + pending(clientWolf) + ")", healed && player.inventory.getSelectedItem() != null && player.inventory.getSelectedItem().count == 2);
			clientWolf.markDead();
			hold(null);
		});
	}


	private CreeperEntity creeper;

	/**
	 * Two standing wolves; creepers after the player, 6 blocks off, out of guarding's reach and their fuse's: the player's punch sends a wolf, and a bite
	 * leaves the creeper going for the player; one 3.5 blocks off is taken by guarding; with Wolves Fight Creepers off, beta's: the punch sends none, and a
	 * bitten creeper turns on the wolf. Each creeper gone before it can light its fuse.
	 */
	private void creepers() {
		wait(70);
		then(() -> {
			keepWell();
			wolves = new WolfEntity[] {wolf(-1, 1, false), wolf(1, 1, false)};
			creeper = besidePlayer(new CreeperEntity(world), 0, -6);
			creeper.setTarget(player);
			player.attack(creeper);
			check("the player punches a creeper and a wolf goes for it (" + described() + ")", on(creeper) > 0);
			WolfEntity biter = null;
			for (WolfEntity w : wolves) if (w.getTarget() == creeper) biter = w;
			creeper.damage(biter, 4);
			check("bitten, it keeps going for the player, not the wolf", creeper.getTarget() == player);
			clearMonsters();
		});
		wait(70);
		then(() -> {
			keepWell();
			creeper = besidePlayer(new CreeperEntity(world), 0, -3.5);
			creeper.setTarget(player);
		});
		until("a creeper after you within 4 blocks gets a wolf", 15, () -> on(creeper) > 0);
		then(() -> {
			clearMonsters();
			check("the rule turns off", Rules.change(WolfPacks.fightCreepers, "false") && !WolfPacks.fightCreepers.get());
		});
		wait(70);
		then(() -> {
			keepWell();
			creeper = besidePlayer(new CreeperEntity(world), 0, -6);
			creeper.setTarget(player);
			player.attack(creeper);
			check("off, the punch sends no wolf, as in beta", on(creeper) == 0);
			creeper.damage(wolves[0], 4);
			check("and a bitten creeper turns on the wolf, as in beta", creeper.getTarget() == wolves[0]);
			clearMonsters();
			check("the rule turns back on", Rules.change(WolfPacks.fightCreepers, "true") && WolfPacks.fightCreepers.get());
			for (WolfEntity w : wolves) w.markDead();
		});
	}
}
