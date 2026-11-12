package org.studioquire.faithful.alchemy.selftest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.CraftingRecipeManager;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.MathHelper;
import org.studioquire.faithful.alchemy.Alchemy;
import org.studioquire.faithful.alchemy.Potion;
import org.studioquire.faithful.alchemy.Timed;
import org.studioquire.faithful.alchemy.Ward;
import org.studioquire.faithful.harness.test.TestHost;
import org.studioquire.faithful.harness.test.WorldTest;
import org.studioquire.faithful.waterbottles.WaterBottles;
import org.studioquire.oldfaithful.core.api.Attachment;
import org.studioquire.oldfaithful.core.api.ItemEntry;
import org.studioquire.oldfaithful.core.api.Names;

/**
 * Where the world runs: the recipes, each potion through its own {@code use}, the ward's sums through beta's own {@code damage}, the timed potions' count, and haste's two factors read through the
 * game's own methods. Real clicks, falls, what a client hears and the pictures are {@code AlchemyClientTest}.
 */
public final class AlchemyTest extends WorldTest {
	private static final Item GOLD = Item.GOLD_INGOT;
	/** Armour slots in {@code PlayerInventory.armor}: feet first. */
	private static final Item[] IRON_SET = {Item.IRON_BOOTS, Item.IRON_LEGGINGS, Item.IRON_CHESTPLATE, Item.IRON_HELMET};
	private static final int CHEST = 2, HEAD = 3;

	private Item rose, dandelion, lupin, featherGold, water, glass;
	private Item weak, plain, strong;
	private int difficultyBefore;

	private static Item[] items(List<ItemEntry<Potion>> family) {
		return family.stream().map(ItemEntry::get).toArray(Item[]::new);
	}

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

	private boolean makes(Item result, ItemStack made) {
		return made != null && made.getItem() == result && made.count == 1;
	}

	private ItemStack plus(Object thing) {
		return crafted(3, null, GOLD, null, GOLD, thing, GOLD, null, GOLD, null);
	}

	private void recipes() {
		check("a golden rose: four ingots in a plus around a rose", makes(rose, plus(Block.ROSE)));
		check("a golden dandelion around a dandelion", makes(dandelion, plus(Block.DANDELION)));
		check("a golden feather around a feather", makes(featherGold, plus(Item.FEATHER)));
		Block flower = Names.block(Alchemy.LUPIN);
		if (FabricLoader.getInstance().isModLoaded("blue-flowers")) check("a golden lupin around Blue Flowers' lupin", flower != null && makes(lupin, plus(flower)));
		else check("without Blue Flowers there's no lupin to gild, and the golden one is still an item", flower == null && lupin != null);
		check("not short of an ingot", crafted(3, null, GOLD, null, GOLD, Block.ROSE, null, null, GOLD, null) == null);
		check("the clock is still the clock", makes(Item.CLOCK, plus(Item.REDSTONE)));
		check("the plus doesn't fit the inventory's grid", crafted(2, GOLD, GOLD, Block.ROSE, GOLD) == null);

		check("a water bottle and a rose, at the bench, is a weak health potion", makes(weak, crafted(3, water, rose)));
		check("and in any cells", makes(weak, crafted(3, null, null, rose, null, null, null, water)));
		check("two roses, the plain one", makes(plain, crafted(3, rose, null, water, null, null, null, null, null, rose)));
		check("three roses, the strong one, in the inventory's grid", makes(strong, crafted(2, rose, rose, water, rose)));
		Object[][] families = {{dandelion, items(Alchemy.haste)}, {lupin, items(Alchemy.ward)}, {featherGold, items(Alchemy.feather)}};
		for (Object[] f : families) {
			Item gilded = (Item) f[0];
			Item[] sizes = (Item[]) f[1];
			check(gilded.getTranslationKey() + ": one, two and three with a bottle in the inventory's grid make the three sizes",
					makes(sizes[0], crafted(2, water, gilded)) && makes(sizes[1], crafted(2, gilded, water, gilded)) && makes(sizes[2], crafted(2, gilded, gilded, water, gilded)));
		}
		check("four roses make nothing", crafted(3, water, rose, rose, rose, rose) == null);
		check("nor two kinds together", crafted(2, water, rose, dandelion) == null);
		check("nor two bottles to a rose", crafted(3, water, water, rose) == null);
		check("nor an empty bottle", crafted(2, glass, rose) == null);
		check("nor a flower ungilded", crafted(2, water, Block.ROSE) == null && crafted(2, water, Item.FEATHER) == null);
	}

	private void clearInventory() {
		Arrays.fill(player.inventory.main, null);
		Arrays.fill(player.inventory.armor, null);
		player.currentScreenHandler.sendContentUpdates();
	}

	/** What the game does with a right click that no block took. */
	private void click() {
		ItemStack held = player.inventory.getSelectedItem();
		ItemStack after = held.use(world, player);
		player.inventory.main[player.inventory.selectedSlot] = after.count == 0 ? null : after;
	}

	private boolean holding(Item item, int count) {
		ItemStack held = player.inventory.getSelectedItem();
		return held != null && held.getItem() == item && held.count == count;
	}

	private int count(Item item) {
		int n = 0;
		for (ItemStack s : player.inventory.main) if (s != null && s.getItem() == item) n += s.count;
		return n;
	}

	private void drink(Item potion) {
		hold(new ItemStack(potion));
		click();
	}

	private void heals(Item potion, int from, int to) {
		clearInventory();
		player.health = from;
		drink(potion);
		check(potion.getTranslationKey() + " from " + from + " half-hearts comes to " + to + " at once, leaving the bottle: " + player.health, player.health == to && holding(glass, 1));
	}

	private void health() {
		check("a potion is one to a slot, a golden thing stacks", weak.getMaxCount() == 1 && plain.getMaxCount() == 1 && strong.getMaxCount() == 1 && rose.getMaxCount() == 64 && featherGold.getMaxCount() == 64);
		heals(weak, 2, 8);
		heals(plain, 2, 14);
		heals(strong, 1, 20);
		heals(weak, 19, 20);

		clearInventory();
		ItemStack potion = new ItemStack(strong);
		hold(potion);
		player.health = 20;
		click();
		check("at full health it isn't drunk", player.inventory.getSelectedItem() == potion && holding(strong, 1) && player.health == 20);

		// What a give command can make; nothing in play gets here.
		clearInventory();
		hold(new ItemStack(weak, 3));
		player.health = 4;
		click();
		check("three in one slot: one is drunk and one bottle comes back", holding(weak, 2) && count(glass) == 1 && player.health == 10);
		player.health = 20;
		clearInventory();
	}

	private void resetWard() {
		Ward.clear(player);
		clearInventory();
		player.health = 20;
		player.hearts = 0;
	}

	private int wear() {
		return Ward.wear.get(player);
	}

	private int whole() {
		return Ward.whole.get(player);
	}

	/** A hit of 5 from nothing in particular, out of the half second of protection after the last. With a protection of 0, 5, 10, 15 or 20 points, beta's carried remainder doesn't come into it. */
	private boolean hit() {
		player.hearts = 0;
		return player.damage(null, 5);
	}

	/** A server's player takes no hits for three seconds after joining: a hit of 1 tried each tick until one lands, and made good. */
	private void outOfJoinProtection() {
		then(this::resetWard);
		await("hits landing", 100, () -> {
			player.hearts = 0;
			if (!player.damage(null, 1)) return false;
			player.health = 20;
			player.hearts = 0;
			return true;
		});
	}

	/** Each size worn alone against a zombie's hits on Normal, through beta's own damage: what Alchemy.WARD's tuning is for, said in the log for whoever tunes it. */
	private void zombieHits() {
		StringBuilder said = new StringBuilder("each ward alone against hits of 5 (a zombie's on Normal):");
		for (int size = 0; size < 3; size++) {
			resetWard();
			Ward.put(player, Alchemy.WARD[size]);
			int hits = 0, taken = 0;
			while (Ward.wear.has(player) && hits < 100) {
				player.health = 20;
				hit();
				taken += 20 - player.health;
				hits++;
			}
			said.append(String.format(" %d lasts %d hits, saving %d of their %d half-hearts;", Alchemy.WARD[size], hits, 5 * hits - taken, 5 * hits));
		}
		note(said.toString());
		resetWard();
	}

	private void ward() {
		check("a set's points are beta's sum for a worn set: all twenty whole, one at the last, half way at half",
				Ward.points(12, 12) == 20 && Ward.points(45, 45) == 20 && Ward.points(1, 45) == 1 && Ward.points(6, 12) == 10 && Ward.points(0, 12) == 0);

		resetWard();
		Ward.put(player, Alchemy.WARD[0]);
		check("a fresh ward fills the bar: a hit with no armour takes 5 of 25 parts of it", hit() && player.health == 19);
		check("and wears the set by the hit's 5, the bar dropping as a worn set's: " + wear() + " of " + whole() + ", " + Ward.points(player) + " points",
				wear() == Alchemy.WARD[0] - 5 && whole() == Alchemy.WARD[0] && Ward.points(player) == 19 * (Alchemy.WARD[0] - 5) / Alchemy.WARD[0] + 1);

		resetWard();
		player.inventory.armor[CHEST] = new ItemStack(Item.IRON_CHESTPLATE);
		Ward.put(player, Alchemy.WARD[0]);
		check("over an iron chestplate, protection stops at the bar's 20", hit() && player.health == 19);
		check("and the blue takes the wear, the chestplate none", player.inventory.armor[CHEST].getDamage2() == 0 && wear() == Alchemy.WARD[0] - 5);
		check("worn, the blue and the chestplate's 8 still fill the bar", hit() && player.health == 18 && player.inventory.armor[CHEST].getDamage2() == 0);
		int left = wear();
		hit();
		check("a hit bigger than what's left of the blue: the blue goes and the chestplate takes the rest", !Ward.wear.has(player) && !Ward.whole.has(player) && player.inventory.armor[CHEST].getDamage2() == 5 - left);
		hit();
		check("then the chestplate wears as in beta", player.inventory.armor[CHEST].getDamage2() == 5 - left + 5);

		resetWard();
		for (int slot = 0; slot < 4; slot++) player.inventory.armor[slot] = new ItemStack(IRON_SET[slot]);
		Ward.put(player, Alchemy.WARD[1]);
		hit();
		boolean spared = true;
		for (ItemStack piece : player.inventory.armor) spared &= piece.getDamage2() == 0;
		check("over a whole iron set none of the set wears while there's blue", spared && player.health == 19 && wear() == Alchemy.WARD[1] - 5);

		resetWard();
		Item[] wards = items(Alchemy.ward);
		drink(wards[0]);
		check("a weak ward potion is a set of the weak's toughness, leaving the bottle", wear() == Alchemy.WARD[0] && whole() == Alchemy.WARD[0] && holding(glass, 1));
		hit();
		drink(wards[0]);
		int more = Alchemy.WARD[0] - 5 + Alchemy.WARD[0];
		check("another over a worn one: what was left adds to it, and the bar is whole again: " + wear() + " of " + whole(), wear() == more && whole() == more && Ward.points(player) == Ward.BAR);
		drink(wards[1]);
		drink(wards[2]);
		check("and up to the strong one's, the rest wasted", wear() == Ward.toughest() && whole() == Ward.toughest() && Ward.full(player));
		ItemStack again = new ItemStack(wards[0]);
		hold(again);
		click();
		check("with the toughest set whole a potion isn't drunk", player.inventory.getSelectedItem() == again);
		hit();
		click();
		check("once it's taken a hit, it is", holding(glass, 1) && wear() == Ward.toughest());

		resetWard();
		player.health = 2;
		drink(Item.GOLDEN_APPLE);
		check("a golden apple heals the whole bar at once and gives the strong one's ward", player.health == 20 && wear() == Ward.toughest() && whole() == Ward.toughest() && player.inventory.getSelectedItem() == null);
		drink(Item.GOLDEN_APPLE);
		check("and with all that already there it's still eaten, as beta eats it", wear() == Ward.toughest() && player.inventory.getSelectedItem() == null);
		Ward.clear(player);
		drink(Item.APPLE);
		check("no other food gives one", !Ward.wear.has(player));

		NbtCompound nbt = new NbtCompound();
		Ward.wear.set(player, 30);
		Ward.whole.set(player, 40);
		player.write(nbt);
		Ward.clear(player);
		player.read(nbt);
		check("the blue is in the player's save", wear() == 30 && whole() == 40);
		resetWard();
		zombieHits();
	}

	private void timed() {
		check("a row of the bottle is 2 minutes, and its six the longest potion's 12", Timed.STEP == 2400 && Timed.MOST == 14400 && Timed.steps(4 * 1200) == 2 && Timed.steps(Timed.MOST) == 6 && Timed.steps(1) == 1);
		Timed haste = Alchemy.hasted, feather = Alchemy.feathered;
		Item[] hastes = items(Alchemy.haste), feathers = items(Alchemy.feather);
		clearTimers();
		clearInventory();
		drink(feathers[0]);
		drink(hastes[0]);
		check("a weak haste potion runs 4 minutes: two rows of its bottle", haste.left.get(player) == 4 * 1200 && haste.shown.get(player) == 2 && haste.running(player) && holding(glass, 1));
		check("drunk after a feather, it stands after it: " + feather.order.get(player) + ", " + haste.order.get(player), Timed.running(player, new ArrayList<>()).equals(Arrays.asList(feather, haste)));
		drink(hastes[1]);
		check("a plain one adds its 8", haste.left.get(player) == 12 * 1200 && haste.full(player));
		ItemStack again = new ItemStack(hastes[2]);
		hold(again);
		click();
		check("and with 12 minutes left a potion isn't drunk", player.inventory.getSelectedItem() == again);
		check("the feather's time is its own", feather.left.get(player) == 4 * 1200);
		drink(feathers[0]);
		check("a top-up keeps its place", feather.left.get(player) == 8 * 1200 && Timed.running(player, new ArrayList<>()).equals(Arrays.asList(feather, haste)));
		feather.left.set(player, 1);

		NbtCompound nbt = new NbtCompound();
		player.write(nbt);
		haste.left.set(player, null);
		player.read(nbt);
		check("what's left is in the player's save", haste.left.get(player) == 12 * 1200);

		int[] before = {0};
		then(() -> {
			check("the feather ended, with what the client hears", !feather.left.has(player) && !feather.shown.has(player) && !feather.order.has(player));
			drink(feathers[0]);
			check("and drunk again it stands after the haste", Timed.running(player, new ArrayList<>()).equals(Arrays.asList(haste, feather)));
			clearTimers(feather);
			before[0] = haste.left.get(player);
		});
		wait(40);
		then(() -> check("and it counts down a tick a tick: " + (before[0] - haste.left.get(player)), Math.abs(before[0] - haste.left.get(player) - 40) <= 1));
		then(() -> haste.left.set(player, 3));
		until("at the end it's gone, what the client hears too", 10, () -> !haste.left.has(player) && !haste.shown.has(player) && !haste.order.has(player) && !haste.running(player));
		then(this::clearInventory);
	}

	/** The pace of mining and walking, as the game's own methods give them, with haste and without. */
	private void hasteFactors() {
		then(() -> {
			Timed haste = Alchemy.hasted;
			hold(new ItemStack(Item.IRON_PICKAXE));
			float pick = Block.STONE.getHardness(player);
			hold(null);
			float hand = Block.STONE.getHardness(player);
			float walk = walked();
			haste.add(player, 4);
			hold(new ItemStack(Item.IRON_PICKAXE));
			float pickHasted = Block.STONE.getHardness(player);
			hold(null);
			float handHasted = Block.STONE.getHardness(player);
			float walkHasted = walked();
			check("haste mines stone 20% faster with a pick (" + pick + " to " + pickHasted + ") and by hand, which beta reckons apart (" + hand + " to " + handHasted + ")",
					Math.abs(pickHasted / pick - Alchemy.HASTE) < 1e-4 && Math.abs(handHasted / hand - Alchemy.HASTE) < 1e-4);
			check("and walks 20% faster (" + walk + " to " + walkHasted + ")", Math.abs(walkHasted / walk - Alchemy.HASTE) < 1e-3);
			clearTimers(haste);
		});
	}

	/** How far one tick of walking forward from standing pushes the player, put back where they stood. */
	private float walked() {
		double px = player.x, py = player.y, pz = player.z;
		boolean ground = player.onGround;
		player.velocityX = player.velocityY = player.velocityZ = 0;
		player.onGround = true;
		player.travel(0.0F, 1.0F);
		float pushed = MathHelper.sqrt(player.velocityX * player.velocityX + player.velocityZ * player.velocityZ);
		player.setPosition(px, py, pz);
		player.onGround = ground;
		player.velocityX = player.velocityY = player.velocityZ = 0;
		return pushed;
	}

	@SuppressWarnings("unchecked")
	private static Attachment<PlayerEntity, Integer> hearty(String field) throws ReflectiveOperationException {
		return (Attachment<PlayerEntity, Integer>) Class.forName("org.studioquire.faithful.heartyfood.HeartyFood").getField(field).get(null);
	}

	private void eat(Item food) {
		hold(new ItemStack(food));
		click();
	}

	/**
	 * Beside Hearty Food, which this mod doesn't depend on, so by reflection, on whichever side of the merge: a potion is at once and doesn't go through its healing, and the ghost hearts stay where
	 * health and what's pending really end up. The timed part needs no Peaceful, whose own healing counts in it.
	 */
	private void besideHeartyFood() throws ReflectiveOperationException {
		Attachment<PlayerEntity, Integer> pending = hearty("pending"), endsAt = hearty("endsAt");
		then(() -> {
			if (!singlePlayer()) world.difficulty = 1;
			clearInventory();
			player.health = 2;
			eat(Item.BREAD);
			check("bread: nothing at once, eight half-hearts coming", player.health == 2 && pending.get(player) == 8 && endsAt.get(player) == 10);
			hold(new ItemStack(weak));
			click();
			check("a potion over it is at once, by its own six, and takes nothing from what's coming: " + player.health + ", " + pending.get(player), player.health == 8 && pending.get(player) == 8);
		});
		until("and the end point rises by that: ghost hearts at 16", 3, () -> endsAt.get(player) == 16);
		then(() -> {
			hold(new ItemStack(strong));
			click();
			check("a strong one fills the bar", player.health == 20);
		});
		until("and nothing's left of the food, so no ghost hearts", 3, () -> !pending.has(player) && !endsAt.has(player));
		then(() -> {
			player.health = 18;
			eat(Item.BREAD);
			check("two half-hearts short, with a full bar on its way", pending.get(player) == 2 && endsAt.get(player) == 20);
			hold(new ItemStack(weak));
			click();
			check("isn't a full bar: a potion still drinks", player.health == 20 && holding(glass, 1));
		});
		until("and what was coming goes", 3, () -> !pending.has(player) && !endsAt.has(player));
		then(() -> {
			player.health = 4;
			eat(Item.GOLDEN_APPLE);
			check("a golden apple still heals at once with Hearty Food, and wards", player.health == 20 && !pending.has(player) && Ward.wear.get(player) == Ward.toughest());
			Ward.clear(player);
			if (!singlePlayer()) world.difficulty = difficultyBefore;
			clearInventory();
		});
	}

	/**
	 * One player goes through every mod's test, so the blue and the haste only reach this test across a restart with no other mod's test in the run: left on for another's, they'd change its hits,
	 * falls and mining. The player's save has them either way (checked above).
	 */
	public static boolean aloneInRun() {
		return TestHost.mods().size() == 1;
	}

	private void leaveForRestart() {
		clearInventory();
		player.health = 20;
		player.inventory.main[0] = new ItemStack(weak);
		player.inventory.main[1] = new ItemStack(plain);
		player.inventory.main[2] = new ItemStack(strong);
		player.inventory.main[3] = new ItemStack(rose, 5);
		player.inventory.main[4] = new ItemStack(items(Alchemy.ward)[1]);
		player.inventory.main[5] = new ItemStack(featherGold, 2);
		player.currentScreenHandler.sendContentUpdates();
		if (aloneInRun()) {
			Ward.wear.set(player, 30);
			Ward.whole.set(player, 40);
			Alchemy.hasted.add(player, 8);
		}
	}

	private ItemStack named(String what, int count) {
		switch (what) {
			case "weak": return new ItemStack(weak, count);
			case "plain": return new ItemStack(plain, count);
			case "strong": return new ItemStack(strong, count);
			case "water": return new ItemStack(water, count);
			case "rose": return new ItemStack(rose, count);
			case "gold": return new ItemStack(GOLD, count);
			case "real rose": return new ItemStack(Block.ROSE, count);
			case "ward": return new ItemStack(items(Alchemy.ward)[0], count);
			case "haste": return new ItemStack(items(Alchemy.haste)[0], count);
			case "chestplate": return new ItemStack(Item.IRON_CHESTPLATE, count);
			default: throw new IllegalArgumentException(what);
		}
	}

	private void clearTimers(Timed... which) {
		for (Timed t : which.length > 0 ? Arrays.asList(which) : Timed.all()) {
			t.left.set(player, null);
			t.shown.set(player, null);
			t.order.set(player, null);
		}
	}

	/** The top of a row, so it holds still a while (the next tick tells the client), and its place in the row of bottles. */
	private void timedTo(Timed effect, int steps, int order) {
		clearTimers(effect);
		if (steps <= 0) return;
		effect.left.set(player, steps * Timed.STEP - 1);
		effect.order.set(player, order);
	}

	private void scenes() {
		// The test server is Peaceful, which heals by itself; the client's tests are timed. In single player the options set it each tick, and aren't Peaceful.
		scene("no peaceful", a -> {
			if (singlePlayer()) return;
			difficultyBefore = world.difficulty;
			world.difficulty = 1;
		});
		scene("peaceful again", a -> {
			if (!singlePlayer()) world.difficulty = difficultyBefore;
		});
		// Into the hotbar's slots from the first, the rest of the inventory emptied: name, count, name, count...
		scene("kit", a -> {
			Arrays.fill(player.inventory.main, null);
			for (int i = 0; i * 2 < a.length; i++) player.inventory.main[i] = named(a[i * 2], Integer.parseInt(a[i * 2 + 1]));
			player.inventory.selectedSlot = 0;
			player.currentScreenHandler.sendContentUpdates();
		});
		scene("health", a -> {
			player.health = Integer.parseInt(a[0]);
			player.hearts = 0;
		});
		scene("bench", a -> set(-2, 0, 0, Block.CRAFTING_TABLE));
		scene("expect", a -> {
			Item item = a[0].equals("glass") ? glass : named(a[0], 1).getItem();
			check("where the world runs: " + a[1] + " of " + a[0] + " (" + count(item) + ")", count(item) == Integer.parseInt(a[1]));
		});
		scene("expect health", a -> {
			int least = Integer.parseInt(a[0]), most = a.length > 1 ? Integer.parseInt(a[1]) : least;
			check("where the world runs: health " + a[0] + (most > least ? " to " + most : "") + " (" + player.health + ")", player.health >= least && player.health <= most);
		});
		scene("eat bread", a -> {
			player.health = Integer.parseInt(a[0]);
			player.hearts = 0;
			ItemStack inHand = player.inventory.getSelectedItem();
			eat(Item.BREAD);
			hold(inHand);
		});
		// Armour none, helmet, chest or set; the blue's wear left and whole; haste's and the feather's rows of liquid, and their places in the row of bottles.
		scene("effects", a -> {
			Arrays.fill(player.inventory.armor, null);
			if (a[0].equals("helmet")) player.inventory.armor[HEAD] = new ItemStack(Item.IRON_HELMET);
			if (a[0].equals("chest")) player.inventory.armor[CHEST] = new ItemStack(Item.IRON_CHESTPLATE);
			if (a[0].equals("set")) for (int slot = 0; slot < 4; slot++) player.inventory.armor[slot] = new ItemStack(IRON_SET[slot]);
			player.currentScreenHandler.sendContentUpdates();
			int wear = Integer.parseInt(a[1]), whole = Integer.parseInt(a[2]);
			Ward.wear.set(player, wear > 0 ? wear : null);
			Ward.whole.set(player, wear > 0 ? whole : null);
			timedTo(Alchemy.hasted, Integer.parseInt(a[3]), a.length > 5 ? Integer.parseInt(a[5]) : 1);
			timedTo(Alchemy.feathered, Integer.parseInt(a[4]), a.length > 6 ? Integer.parseInt(a[6]) : 2);
		});
		scene("expect ward", a -> check("where the world runs: the blue's wear " + a[0] + " (" + Ward.wear.get(player) + ")", Ward.wear.get(player) == Integer.parseInt(a[0])));
		scene("expect haste", a -> check("where the world runs: haste running for " + Alchemy.hasted.left.get(player) + " ticks, at least " + a[0], Alchemy.hasted.left.get(player) >= Integer.parseInt(a[0])));
		scene("feather", a -> {
			clearTimers(Alchemy.feathered);
			if (a[0].equals("on")) Alchemy.feathered.add(player, 4);
		});
		scene("row", a -> {
			clearInventory();
			player.health = 20;
			ItemStack[] row = {new ItemStack(Block.ROSE), new ItemStack(rose), new ItemStack(Block.DANDELION), new ItemStack(dandelion), new ItemStack(lupin), new ItemStack(Item.FEATHER),
					new ItemStack(featherGold), new ItemStack(water), new ItemStack(glass)};
			for (int i = 0; i < row.length; i++) player.inventory.main[i] = row[i];
			Item[][] families = {items(Alchemy.health), items(Alchemy.haste), items(Alchemy.ward), items(Alchemy.feather)};
			int slot = 9;
			for (Item[] family : families) for (Item potion : family) player.inventory.main[slot++] = new ItemStack(potion);
			Block flower = Names.block(Alchemy.LUPIN);
			if (flower != null) player.inventory.main[slot] = new ItemStack(flower);
			player.currentScreenHandler.sendContentUpdates();
			world.setTime(6000);
		});
		scene("leave", a -> leaveForRestart());
	}

	@Override
	protected void script() {
		rose = Alchemy.goldenRose.get();
		dandelion = Alchemy.goldenDandelion.get();
		lupin = Alchemy.goldenLupin.get();
		featherGold = Alchemy.goldenFeather.get();
		weak = Alchemy.weakPotion.get();
		plain = Alchemy.healthPotion.get();
		strong = Alchemy.strongPotion.get();
		water = WaterBottles.waterBottle.get();
		glass = WaterBottles.glassBottle.get();
		scenes();
		if (restarted()) {
			check("the potions and the golden things came through the restart by name", player.inventory.main[0] != null && player.inventory.main[0].getItem() == weak
					&& player.inventory.main[1] != null && player.inventory.main[1].getItem() == plain
					&& player.inventory.main[2] != null && player.inventory.main[2].getItem() == strong
					&& player.inventory.main[3] != null && player.inventory.main[3].getItem() == rose && player.inventory.main[3].count == 5
					&& player.inventory.main[4] != null && player.inventory.main[4].getItem() == items(Alchemy.ward)[1]
					&& player.inventory.main[5] != null && player.inventory.main[5].getItem() == featherGold && player.inventory.main[5].count == 2);
			if (aloneInRun()) {
				check("and the blue and the haste with them: " + Ward.wear.get(player) + " of " + Ward.whole.get(player) + ", " + Alchemy.hasted.left.get(player),
						Ward.wear.get(player) == 30 && Ward.whole.get(player) == 40 && Alchemy.hasted.left.get(player) > 0 && Alchemy.hasted.left.get(player) <= 8 * Timed.TICKS_A_MINUTE);
			} else {
				note("another mod's test is in this run, so no blue or haste was left on for the restart");
			}
			return;
		}
		recipes();
		health();
		outOfJoinProtection();
		then(this::ward);
		timed();
		hasteFactors();
		if (FabricLoader.getInstance().isModLoaded("hearty-food")) {
			try {
				besideHeartyFood();
			} catch (ReflectiveOperationException e) {
				check("Hearty Food has what this test reads: " + e, false);
			}
		}
		then(this::leaveForRestart);
	}
}
