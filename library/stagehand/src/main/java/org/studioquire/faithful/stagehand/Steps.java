package org.studioquire.faithful.stagehand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldProperties;
import org.studioquire.oldfaithful.core.api.Days;
import org.studioquire.faithful.stagehand.mixin.EntityAccessor;
import org.studioquire.oldfaithful.core.api.Server;

/** The commands. docs/stagehand.md says what each takes and gives back. */
final class Steps {
	private static final int TICKS_TO_SEE_IT_TOOK = 2;
	private static final int LONGEST_WAIT = 20 * 60 * 60;
	private static final int MOST_TICKS_FOR_A_CLIENT = 200;

	private Steps() {
	}

	private static Integer dimension(Words words) {
		String word = words.option("dim");
		return word == null ? null : Integer.valueOf(Move.dimension(word));
	}

	private static PlayerEntity alive(Run run) {
		PlayerEntity me = run.me();
		if (me.health <= 0 || me.dead) throw new Failure("dead", me.name + " is dead");
		return me;
	}

	private static void y(double y) {
		if (y < 0 || y > 127) throw new Failure("bad_coordinate", "y is from 0 to 127 here, not " + y);
	}

	static final class As extends Step {
		private final String name;

		As(Words words) {
			name = words.next("a player's name");
		}

		@Override
		boolean run(Run run) {
			run.as = name;
			data.put("player", run.me().name);
			return true;
		}
	}

	/** What's common to {@code tp}, {@code locate} and {@code wait until}: a kind of place and what's said about it. */
	private static Places kind(String word) {
		Places places = PlaceKinds.all().get(word.toLowerCase(Locale.ROOT));
		if (places == null) throw new Failure("unknown_place", "nothing answers for " + word + "; there's " + String.join(", ", PlaceKinds.all().keySet()));
		return places;
	}

	static final class Teleport extends Step {
		private final Words.Position position;
		private final Words.Coordinate surfaceX, surfaceZ;
		private final Places places;
		private final List<String> placeWords;
		private final Map<String, String> placeOptions;
		private final Integer dimension;
		private final boolean exact;
		private final Float yaw, pitch;
		private Move.Landed landed;
		private int chunkX, chunkZ, waited;
		private boolean groundCameLate;

		Teleport(Words words) {
			exact = words.flag("exact");
			String yawWord = words.option("yaw"), pitchWord = words.option("pitch");
			yaw = yawWord == null ? null : Float.valueOf(Words.decimal("yaw", yawWord, -360.0F, 360.0F));
			pitch = pitchWord == null ? null : Float.valueOf(Words.decimal("pitch", pitchWord, -90.0F, 90.0F));
			dimension = dimension(words);
			if (!words.has(0)) throw new Failure("missing", "tp wants a place: x y z, x z, @me or a kind of place, like spawn");
			if (Words.isCoordinate(words.get(0))) {
				places = null;
				placeWords = null;
				placeOptions = null;
				if (words.plain.size() == 2) {
					position = null;
					surfaceX = new Words.Coordinate(words.next("x"));
					surfaceZ = new Words.Coordinate(words.next("z"));
				} else {
					surfaceX = surfaceZ = null;
					position = words.position("x y z");
					if (!position.y.relative && !exact) y(position.y.value);
				}
			} else {
				position = null;
				surfaceX = surfaceZ = null;
				if (dimension != null) throw new Failure("extra_options", "a kind of place says its own dimension, so dim= is for coordinates");
				places = kind(words.next("a place"));
				placeWords = new ArrayList<>(words.plain);
				placeOptions = new LinkedHashMap<>(words.options);
				words.plain.clear();
				words.options.clear();
			}
		}

		@Override
		boolean run(Run run) {
			if (landed == null) {
				PlayerEntity me = run.me();
				if (!run.side.answered(me)) {
					if (++waited > MOST_TICKS_FOR_A_CLIENT) throw new Failure("client_silent", me.name + "'s client hasn't answered its last move, so another can't safely follow it");
					return false;
				}
				waited = 0;
				landed = Move.go(run.side, me, target(run, me));
				chunkX = MathHelper.floor(landed.x) >> 4;
				chunkZ = MathHelper.floor(landed.z) >> 4;
				data.putAll(landed.report);
			}
			if (run.side.paused()) return true;
			// On a server the client is a moment behind: the step ends when it's there, so that nothing that follows has to think about it.
			PlayerEntity me = run.me();
			boolean arrived = run.side.arrived(me, chunkX, chunkZ);
			if (!arrived) groundCameLate = true;
			if (!arrived || !run.side.answered(me)) {
				if (++waited > MOST_TICKS_FOR_A_CLIENT) {
					data.put("note", "their client hasn't caught up with this move yet");
					return true;
				}
				return false;
			}
			if (groundCameLate) {
				groundCameLate = false;
				run.side.correct(me, landed.x, landed.y, landed.z, landed.yaw, landed.pitch);
				return false;
			}
			return true;
		}

		private Move.Target target(Run run, PlayerEntity me) {
			int here = me.dimensionId;
			if (places != null) {
				Places.Place place = run.find(places, placeWords, placeOptions);
				if (!place.about.isEmpty()) data.put("place", place.about);
				data.put("asked", Move.coordinates(place.x, place.y, place.z));
				return new Move.Target(place.dimension, place.x, place.y, place.z, false, exact, yaw, pitch);
			}
			int to = dimension != null ? dimension : here;
			if (position == null) {
				double x = surfaceX.from(MathHelper.floor(me.x)), z = surfaceZ.from(MathHelper.floor(me.z));
				return new Move.Target(to, x, 64, z, true, false, yaw, pitch);
			}
			double[] at = position.at(run);
			if (!exact) y(at[1]);
			else if (at[1] < -64 || at[1] > 256) throw new Failure("bad_coordinate", "even with exact, y is from -64 to 256");
			data.put("asked", Move.coordinates(at[0], at[1], at[2]));
			return new Move.Target(to, at[0], at[1], at[2], false, exact, yaw, pitch);
		}
	}

	static final class Locate extends Step {
		private final Places places;
		private final List<String> placeWords;
		private final Map<String, String> placeOptions;

		Locate(Words words) {
			places = kind(words.next("a kind of place: " + String.join(", ", PlaceKinds.all().keySet())));
			placeWords = new ArrayList<>(words.plain);
			placeOptions = new LinkedHashMap<>(words.options);
			words.plain.clear();
			words.options.clear();
		}

		@Override
		boolean run(Run run) {
			Places.Place place = run.find(places, placeWords, placeOptions);
			data.put("kind", places.kind());
			data.putAll(Move.coordinates(place.x, place.y, place.z));
			data.put("dim", Move.dimension(place.dimension));
			PlayerEntity me = run.meOrNull();
			if (me != null && me.dimensionId == place.dimension) data.put("distance", Move.round(MathHelper.sqrt(me.getSquaredDistance(place.x + 0.5, place.y, place.z + 0.5))));
			if (!place.about.isEmpty()) data.put("place", place.about);
			return true;
		}
	}

	/** Puts it in the inventory as far as there's room, armour on the body if asked and that slot's free. How many went in. */
	private static int give(PlayerEntity me, Item item, int count, int damage, boolean wear) {
		int given = 0;
		if (wear && item instanceof ArmorItem && count > 0) {
			int slot = 3 - ((ArmorItem) item).equipmentSlot;
			if (me.inventory.armor[slot] == null) {
				me.inventory.armor[slot] = new ItemStack(item, 1, damage);
				given++;
			}
		}
		while (given < count) {
			int size = Math.min(count - given, Math.max(1, item.getMaxCount()));
			ItemStack stack = new ItemStack(item, size, damage);
			me.inventory.addStack(stack);
			given += size - stack.count;
			if (stack.count > 0) break;
		}
		return given;
	}

	static final class Give extends Step {
		private final Item item;
		private final int count, damage;
		private final boolean wear;

		Give(Words words) {
			wear = words.flag("wear");
			Named.Spec spec = new Named.Spec(words.next("an item's name, like iron_sword or wool@14"));
			item = Named.item(spec);
			damage = spec.meta;
			count = words.has(0) ? words.number("the count", 1, 36 * 64) : 1;
		}

		@Override
		boolean run(Run run) {
			PlayerEntity me = alive(run);
			int given = give(me, item, count, damage, wear);
			me.currentScreenHandler.sendContentUpdates();
			data.put("item", Named.name(item));
			data.put("given", given);
			if (given < count) data.put("no_room_for", count - given);
			return true;
		}
	}

	static final class Kit extends Step {
		private final String name;

		Kit(Words words) {
			String known = String.join(", ", Kits.ALL.keySet());
			name = words.next("a kit: " + known).toLowerCase(Locale.ROOT);
			if (!Kits.ALL.containsKey(name)) throw new Failure("unknown_kit", "no kit called " + name + "; there's " + known);
		}

		@Override
		boolean run(Run run) {
			PlayerEntity me = alive(run);
			List<Object> items = new ArrayList<>();
			int noRoom = 0;
			for (String entry : Kits.ALL.get(name)) {
				String[] parts = entry.split(" ");
				int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
				Item item = Named.item(new Named.Spec(parts[0]));
				int given = give(me, item, count, 0, true);
				noRoom += count - given;
				items.add(parts[0] + (given == 1 ? "" : " " + given));
			}
			me.currentScreenHandler.sendContentUpdates();
			data.put("given", items);
			if (noRoom > 0) data.put("no_room_for", noRoom);
			return true;
		}
	}

	static final class Heal extends Step {
		private final int amount;

		Heal(Words words) {
			amount = words.has(0) ? words.number("how much", 1, 20) : 20;
		}

		@Override
		boolean run(Run run) {
			PlayerEntity me = alive(run);
			me.health = Math.min(20, me.health + amount);
			me.fireTicks = 0;
			me.air = 300;
			((EntityAccessor) me).stagehand$fallDistance(0.0F);
			data.put("health", me.health);
			return true;
		}
	}

	static final class Health extends Step {
		private final int amount;

		Health(Words words) {
			amount = words.number("half-hearts", 1, 20);
		}

		@Override
		boolean run(Run run) {
			PlayerEntity me = alive(run);
			me.health = amount;
			data.put("health", me.health);
			return true;
		}
	}

	static final class Clear extends Step {
		private final boolean items, armor;

		Clear(Words words) {
			String what = words.has(0) ? words.next("").toLowerCase(Locale.ROOT) : "all";
			if (!what.equals("all") && !what.equals("items") && !what.equals("armor")) throw new Failure("bad_word", "clear takes all, items or armor, not " + what);
			items = !what.equals("armor");
			armor = !what.equals("items");
		}

		@Override
		boolean run(Run run) {
			PlayerEntity me = run.me();
			int stacks = 0;
			if (items) stacks += empty(me.inventory.main);
			if (armor) stacks += empty(me.inventory.armor);
			me.currentScreenHandler.sendContentUpdates();
			data.put("stacks_removed", stacks);
			return true;
		}

		private static int empty(ItemStack[] slots) {
			int stacks = 0;
			for (int i = 0; i < slots.length; i++) {
				if (slots[i] != null) stacks++;
				slots[i] = null;
			}
			return stacks;
		}
	}

	static final class Time extends Step {
		private static final Map<String, Integer> HOURS = new LinkedHashMap<>();

		static {
			HOURS.put("dawn", 0);
			HOURS.put("day", 1000);
			HOURS.put("noon", 6000);
			HOURS.put("dusk", 12000);
			HOURS.put("night", 14000);
			HOURS.put("midnight", 18000);
		}

		private final Integer hour;

		Time(Words words) {
			if (!words.has(0)) {
				hour = null;
				return;
			}
			String word = words.next("").toLowerCase(Locale.ROOT);
			Integer named = HOURS.get(word);
			hour = named != null ? named : Integer.valueOf(Words.number("the hour", word, 0, Days.NORMAL - 1));
		}

		/**
		 * Always forward. A world's clock is also the count its block ticks wait on, so turning it back would hold every repeater and flow for the difference; the next time it's this hour is the same
		 * to the sun.
		 */
		@Override
		boolean run(Run run) {
			if (hour != null) {
				for (World world : Server.worlds()) {
					long now = world.getTime();
					long next = now - now % Days.NORMAL + hour;
					if (next <= now) next += Days.NORMAL;
					run.side.setTime(world, next);
				}
			}
			World world = run.world(null);
			data.put("hour", Days.time(world) % Days.NORMAL);
			data.put("day", Days.time(world) / Days.NORMAL);
			return true;
		}
	}

	static final class Weather extends Step {
		private final String what;
		private final int ticks;
		private int waited;

		Weather(Words words) {
			what = words.has(0) ? words.next("").toLowerCase(Locale.ROOT) : null;
			if (what != null && !what.equals("clear") && !what.equals("rain") && !what.equals("thunder")) throw new Failure("bad_word", "weather takes clear, rain or thunder, not " + what);
			ticks = words.has(0) ? Words.ticks("how long", words.next(""), LONGEST_WAIT * 24) : 12000;
		}

		@Override
		boolean run(Run run) {
			if (what != null && waited == 0) {
				for (World world : Server.worlds()) {
					WorldProperties properties = world.getProperties();
					properties.setRaining(!what.equals("clear"));
					properties.setRainTime(ticks);
					properties.setThundering(what.equals("thunder"));
					properties.setThunderTime(ticks);
				}
			}
			// World Rules can hold the weather, each tick: that's the world's rule, and Stagehand doesn't fight it, but the script should know.
			if (what != null && !run.side.paused() && waited++ < TICKS_TO_SEE_IT_TOOK) return false;
			WorldProperties properties = run.world(null).getProperties();
			data.put("raining", properties.getRaining());
			data.put("thundering", properties.getThundering());
			if (what != null && properties.getRaining() == what.equals("clear")) data.put("note", "it didn't take: something holds the weather here, likely World Rules' rule");
			return true;
		}
	}

	static final class Spawn extends Step {
		private final String id;
		private final int count;
		private final Words.Position at;
		private final Integer dimension;
		private final boolean exact;
		private final List<Entity> spawned = new ArrayList<>();
		private int waited;

		Spawn(Words words) {
			exact = words.flag("exact");
			dimension = dimension(words);
			id = Named.entityId(words.next("what to spawn, like zombie"));
			int fromHere = words.plain.indexOf("at");
			count = words.has(0) && fromHere != 0 ? words.number("the count", 1, 64) : 1;
			if (words.has(0)) {
				String word = words.next("at");
				if (!word.equalsIgnoreCase("at")) throw new Failure("extra_words", "after the count comes: at x y z");
				at = words.position("x y z");
			} else {
				at = null;
				if (exact) throw new Failure("extra_words", "exact goes with: at x y z");
			}
		}

		@Override
		boolean run(Run run) {
			if (spawned.isEmpty()) start(run);
			if (!run.side.paused() && waited++ < TICKS_TO_SEE_IT_TOOK) return false;
			int alive = 0;
			for (Entity entity : spawned) {
				if (!entity.dead) alive++;
			}
			data.put("spawned", spawned.size());
			if (run.side.paused()) data.put("note", "the game is paused, so whether they'll last isn't known yet");
			else {
				data.put("alive", alive);
				if (alive < spawned.size()) data.put("note", "some went at once: this world doesn't keep them (peaceful, or a server's spawn-monsters or spawn-animals off)");
			}
			return true;
		}

		private void start(Run run) {
			World world = run.world(dimension);
			double x, y, z;
			if (at != null) {
				double[] cell = at.at(run);
				x = cell[0];
				y = cell[1];
				z = cell[2];
			} else {
				PlayerEntity me = run.me();
				double radians = me.yaw * Math.PI / 180.0;
				x = me.x - Math.sin(radians) * 4.0;
				y = me.boundingBox.minY;
				z = me.z + Math.cos(radians) * 4.0;
			}
			y(y);
			int cellX = MathHelper.floor(x), cellY = MathHelper.floor(y), cellZ = MathHelper.floor(z);
			Move.load(world, cellX, cellZ, 2);
			int[] cell = exact ? new int[] {cellX, cellY, cellZ} : Landing.near(world, cellX, cellY, cellZ);
			if (cell == null) throw new Failure("no_safe_place", "nowhere for it to stand near " + cellX + " " + cellY + " " + cellZ + "; \"exact\" puts it there anyway");
			String name = Named.entity(Named.makeEntity(id, world));
			for (int i = 0; i < count; i++) {
				Entity entity = Named.makeEntity(id, world);
				entity.setPositionAndAnglesKeepPrevAngles(cell[0] + 0.5, cell[1], cell[2] + 0.5, world.random.nextFloat() * 360.0F, 0.0F);
				if (!world.spawnEntity(entity)) throw new Failure("not_spawned", "the world refused it at " + cell[0] + " " + cell[1] + " " + cell[2]);
				spawned.add(entity);
			}
			data.put("entity", name);
			data.putAll(Move.coordinates(cell[0], cell[1], cell[2]));
		}
	}

	static final class SetBlock extends Step {
		private final Words.Position at;
		private final int block, meta;
		private final Integer dimension;

		SetBlock(Words words) {
			dimension = dimension(words);
			at = words.position("x y z");
			Named.Spec spec = new Named.Spec(words.next("a block's name, like stone or wool@14"));
			block = Named.block(spec);
			meta = spec.meta;
			if (!at.y.relative) y(at.y.value);
		}

		@Override
		boolean run(Run run) {
			World world = run.world(dimension);
			double[] cell = at.at(run);
			y(cell[1]);
			int x = MathHelper.floor(cell[0]), y = MathHelper.floor(cell[1]), z = MathHelper.floor(cell[2]);
			Move.load(world, x, z, 1);
			world.setBlock(x, y, z, block, meta);
			if (world.getBlockId(x, y, z) != block) throw new Failure("not_placed", "the cell didn't take it");
			data.putAll(Move.coordinates(x, y, z));
			data.put("block", Landing.blockAt(world, x, y, z));
			return true;
		}
	}

	static final class Fill extends Step {
		private static final int MOST_CELLS = 65536, CELLS_A_TICK = 2048;

		private final Words.Position from, to;
		private final int block, meta;
		private final Integer dimension;
		private int[] low, high;
		private int x, y, z, changed, cells, chunkX = Integer.MAX_VALUE, chunkZ = Integer.MAX_VALUE, worldDimension;

		Fill(Words words) {
			dimension = dimension(words);
			from = words.position("the first corner, x y z");
			to = words.position("the other corner, x y z");
			Named.Spec spec = new Named.Spec(words.next("a block's name, like stone or air"));
			block = Named.block(spec);
			meta = spec.meta;
			if (!from.relative() && !to.relative()) box(new double[] {from.x.value, from.y.value, from.z.value}, new double[] {to.x.value, to.y.value, to.z.value});
		}

		private void box(double[] a, double[] b) {
			y(a[1]);
			y(b[1]);
			low = new int[3];
			high = new int[3];
			long volume = 1;
			for (int i = 0; i < 3; i++) {
				low[i] = MathHelper.floor(Math.min(a[i], b[i]));
				high[i] = MathHelper.floor(Math.max(a[i], b[i]));
				volume *= high[i] - low[i] + 1L;
				if (volume > MOST_CELLS) break;
			}
			if (volume > MOST_CELLS) throw new Failure("too_big", "a fill is at most " + MOST_CELLS + " cells, so it can't hold up the game; split this one");
			x = low[0];
			y = low[1];
			z = low[2];
		}

		/** A slice each tick, so a big fill never stops the game. In y, then z, then x: a chunk at a time. */
		@Override
		boolean run(Run run) {
			World world = run.world(dimension);
			if (low == null) box(from.at(run), to.at(run));
			if (cells == 0) worldDimension = world.dimension.id;
			else if (world.dimension.id != worldDimension) throw new Failure("world_changed", "they changed dimension in the middle of this fill");
			for (int budget = 0; budget < CELLS_A_TICK; budget++) {
				if (x >> 4 != chunkX || z >> 4 != chunkZ) {
					chunkX = x >> 4;
					chunkZ = z >> 4;
					Move.load(world, x, z, 1);
				}
				if (world.setBlock(x, y, z, block, meta)) changed++;
				cells++;
				if (++y > high[1]) {
					y = low[1];
					if (++z > high[2]) {
						z = low[2];
						if (++x > high[0]) {
							data.put("cells", cells);
							data.put("changed", changed);
							data.put("block", Landing.blockAt(world, high[0], high[1], high[2]));
							return true;
						}
					}
				}
			}
			return false;
		}
	}

	/** Goes to {@code @me}, in lines a client's chat packet can take: past 119 characters a client hangs up on it. */
	static final class Say extends Step {
		private static final int LONGEST = 100;
		private final List<String> lines = new ArrayList<>();

		Say(String text) {
			if (text.isEmpty()) throw new Failure("missing", "say wants something to say");
			StringBuilder clean = new StringBuilder();
			for (int i = 0; i < text.length(); i++) {
				char c = text.charAt(i);
				clean.append(c < 0x20 ? ' ' : c);
			}
			String rest = clean.toString();
			while (rest.length() > LONGEST) {
				int cut = rest.lastIndexOf(' ', LONGEST);
				if (cut <= 0) cut = LONGEST;
				lines.add(rest.substring(0, cut));
				rest = rest.substring(cut).trim();
			}
			if (!rest.isEmpty()) lines.add(rest);
		}

		@Override
		boolean run(Run run) {
			PlayerEntity me = run.me();
			for (String line : lines) me.sendMessage(line);
			data.put("lines", lines.size());
			return true;
		}
	}

	/** Ticks of the world, so none go by while single player is paused. */
	static final class Wait extends Step {
		private final int ticks;
		private int waited;

		Wait(Words words) {
			ticks = Words.ticks("how long", words.next("how long: 40, 5s or 2m"), LONGEST_WAIT);
		}

		@Override
		boolean run(Run run) {
			if (run.side.paused()) return false;
			return waited++ >= ticks;
		}
	}

	/** {@code wait until near x y z} or {@code wait until <kind of place> ...}: until {@code @me} is within {@code radius}, or the {@code timeout} ends the script. */
	static final class WaitUntil extends Step {
		private final Words.Position at;
		private final Places places;
		private final List<String> placeWords;
		private final Map<String, String> placeOptions;
		private final Integer dimension;
		private final double radius;
		private final int timeout;
		private double[] target;
		private int targetDimension;
		private int waited;

		WaitUntil(Words words) {
			words.next("until");
			String radiusWord = words.option("radius"), timeoutWord = words.option("timeout");
			radius = radiusWord == null ? 4.0 : Words.decimal("radius", radiusWord, 0.0F, 100000.0F);
			timeout = timeoutWord == null ? 20 * 60 * 10 : Words.ticks("timeout", timeoutWord, LONGEST_WAIT);
			String what = words.next("near x y z, or a kind of place");
			if (what.equalsIgnoreCase("near")) {
				dimension = dimension(words);
				at = words.position("x y z");
				places = null;
				placeWords = null;
				placeOptions = null;
			} else {
				dimension = null;
				at = null;
				places = kind(what);
				placeWords = new ArrayList<>(words.plain);
				placeOptions = new LinkedHashMap<>(words.options);
				words.plain.clear();
				words.options.clear();
			}
		}

		@Override
		boolean run(Run run) {
			if (target == null) {
				PlayerEntity me = run.me();
				if (places != null) {
					Places.Place place = run.find(places, placeWords, placeOptions);
					target = new double[] {place.x + 0.5, place.y, place.z + 0.5};
					targetDimension = place.dimension;
				} else {
					double[] cell = at.at(run);
					target = new double[] {cell[0] + 0.5, cell[1], cell[2] + 0.5};
					targetDimension = dimension != null ? dimension : me.dimensionId;
				}
				data.putAll(Move.coordinates(target[0], target[1], target[2]));
				data.put("dim", Move.dimension(targetDimension));
			}
			if (run.side.paused()) return false;
			PlayerEntity me = run.meOrNull();
			if (me != null && me.dimensionId == targetDimension) {
				double dx = me.x - target[0], dy = me.boundingBox.minY - target[1], dz = me.z - target[2];
				if (dx * dx + dy * dy + dz * dz <= radius * radius) {
					data.put("waited_ticks", waited);
					return true;
				}
			}
			if (++waited > timeout) throw new Failure("timed_out", "not within " + radius + " after " + timeout + " ticks");
			return false;
		}
	}

	/** Where {@code @me} is and what they have, what's around them, the world's hour and weather. */
	static final class Status extends Step {
		private static final int MOST_MOBS = 40;
		private final int radius;

		Status(Words words) {
			String word = words.option("radius");
			radius = word == null ? 32 : Words.number("radius", word, 1, 128);
		}

		@Override
		boolean run(Run run) {
			data.put("where", run.side.where());
			data.put("paused", run.side.paused());
			data.put("players", Run.names());
			World world = run.world(null);
			data.put("hour", Days.time(world) % Days.NORMAL);
			data.put("day", Days.time(world) / Days.NORMAL);
			data.put("raining", world.getProperties().getRaining());
			data.put("thundering", world.getProperties().getThundering());
			data.put("difficulty", world.difficulty);
			data.put("seed", String.valueOf(world.getSeed()));
			PlayerEntity me = run.meOrNull();
			if (me == null) return true;
			int cellX = MathHelper.floor(me.x), feet = MathHelper.floor(me.boundingBox.minY), cellZ = MathHelper.floor(me.z);
			data.put("player", me.name);
			data.put("x", Move.round(me.x));
			data.put("y", Move.round(me.boundingBox.minY));
			data.put("z", Move.round(me.z));
			data.put("dim", Move.dimension(me.dimensionId));
			data.put("yaw", Move.round(me.yaw));
			data.put("pitch", Move.round(me.pitch));
			data.put("health", me.health);
			data.put("alive", me.health > 0 && !me.dead);
			data.put("on_ground", me.onGround);
			data.put("biome", world.method_1781().getBiome(cellX, cellZ).name);
			data.put("standing_on", Landing.blockAt(world, cellX, feet - 1, cellZ));
			data.put("in", Landing.blockAt(world, cellX, feet, cellZ));
			data.put("selected_slot", me.inventory.selectedSlot);
			data.put("armor", armor(me));
			data.put("inventory", inventory(me));
			data.put("nearby", nearby(world, me));
			return true;
		}

		private static Map<String, Object> armor(PlayerEntity me) {
			String[] names = {"feet", "legs", "chest", "head"};
			Map<String, Object> out = new LinkedHashMap<>();
			for (int i = 3; i >= 0; i--) {
				ItemStack stack = me.inventory.armor[i];
				if (stack != null) out.put(names[i], stack(stack));
			}
			return out;
		}

		private static List<Object> inventory(PlayerEntity me) {
			List<Object> out = new ArrayList<>();
			for (int i = 0; i < me.inventory.main.length; i++) {
				ItemStack stack = me.inventory.main[i];
				if (stack == null) continue;
				Map<String, Object> entry = stack(stack);
				entry.put("slot", i);
				out.add(entry);
			}
			return out;
		}

		private static Map<String, Object> stack(ItemStack stack) {
			Map<String, Object> out = new LinkedHashMap<>();
			Item item = stack.getItem();
			out.put("item", item == null ? "item#" + stack.itemId : Named.name(item));
			out.put("count", stack.count);
			if (stack.getDamage() != 0) out.put("damage", stack.getDamage());
			return out;
		}

		private List<Object> nearby(World world, PlayerEntity me) {
			@SuppressWarnings("unchecked")
			List<LivingEntity> found = new ArrayList<>((List<LivingEntity>) world.collectEntitiesByClass(LivingEntity.class, Box.create(me.x - radius, me.y - radius, me.z - radius, me.x + radius, me.y + radius, me.z + radius)));
			found.remove(me);
			found.removeIf(e -> e.dead);
			Collections.sort(found, Comparator.comparingDouble(e -> e.getSquaredDistance(me.x, me.y, me.z)));
			List<Object> out = new ArrayList<>();
			for (LivingEntity entity : found) {
				if (out.size() >= MOST_MOBS) break;
				Map<String, Object> entry = new LinkedHashMap<>();
				entry.put("type", entity instanceof PlayerEntity ? "player" : Named.entity(entity));
				if (entity instanceof PlayerEntity) entry.put("name", ((PlayerEntity) entity).name);
				entry.put("x", Move.round(entity.x));
				entry.put("y", Move.round(entity.boundingBox.minY));
				entry.put("z", Move.round(entity.z));
				entry.put("distance", Move.round(MathHelper.sqrt(entity.getSquaredDistance(me.x, me.y, me.z))));
				entry.put("health", entity.health);
				out.add(entry);
			}
			return out;
		}
	}

	static final class Cancel extends Step {
		private final String id;

		Cancel(Words words) {
			id = words.has(0) ? words.next("") : null;
		}

		@Override
		boolean run(Run run) {
			data.put("cancelled", Stagehand.cancel(id, run));
			return true;
		}
	}

	/** What a script can name. */
	static final class Lists extends Step {
		private final String what;

		Lists(Words words) {
			what = words.next("items, blocks, kits, places or entities").toLowerCase(Locale.ROOT);
			if (!java.util.Arrays.asList("items", "blocks", "kits", "places", "entities").contains(what)) throw new Failure("bad_word", "list takes items, blocks, kits, places or entities, not " + what);
		}

		@Override
		boolean run(Run run) {
			switch (what) {
				case "items": data.put("items", Named.itemNames()); break;
				case "blocks": data.put("blocks", Named.blockNames()); break;
				case "kits": data.put("kits", new TreeMap<>(Kits.ALL)); break;
				case "places": data.put("places", new ArrayList<>(PlaceKinds.all().keySet())); break;
				default:
					List<String> entities = new ArrayList<>();
					for (String id : Named.ENTITIES) entities.add(id.toLowerCase(Locale.ROOT));
					data.put("entities", entities);
			}
			return true;
		}
	}
}
