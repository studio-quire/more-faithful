package org.studioquire.faithful.wolfpacks.selftest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.PigZombieEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.studioquire.faithful.harness.test.WorldTest;
import org.studioquire.faithful.wolfpacks.Packs;
import org.studioquire.faithful.wolfpacks.WolfFood;
import org.studioquire.faithful.wolfpacks.WolfPacks;
import org.studioquire.oldfaithful.core.impl.Rules;

/**
 * Where the world runs, with real ticks: wolves, pig zombies (which don't burn by day, unlike zombies) and creepers on the plot, the call to arms by the
 * player's own punch. The selftest server is Peaceful, which takes monsters on their first tick, so it's Normal for the test; single player's comes from
 * the options. The player is kept whole throughout, as the pig zombies go for them.
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
			check("the rules on after the restart, as the first life left them", WolfPacks.spreadOut.get() && WolfPacks.fightCreepers.get() && WolfPacks.feeding.get() && WolfPacks.guarding.get());
			return;
		}
		difficultyBefore = world.difficulty;
		if (!singlePlayer()) world.difficulty = 2;
		check("monsters can live here (difficulty " + world.difficulty + ")", world.difficulty > 0);
		check("the rules on by default", WolfPacks.spreadOut.get() && WolfPacks.fightCreepers.get() && WolfPacks.feeding.get() && WolfPacks.guarding.get());
		spreading();
		oneEnemy();
		ruleOff();
		guarding();
		creepers();
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
			check("the sitting wolf stays seated, with nothing to do", seated.isInSittingPose() && seated.getTarget() == null);
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
			clearMonsters();
			for (WolfEntity w : wolves) w.markDead();
			seated.markDead();
		});
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
