package org.studioquire.faithful.stagehand.selftest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.studioquire.faithful.harness.test.WorldTest;
import org.studioquire.faithful.stagehand.selftest.server.NetherSwitch;
import org.studioquire.oldfaithful.core.api.Chunks;
import org.studioquire.oldfaithful.core.api.Names;
import org.studioquire.oldfaithful.core.api.Server;
import org.studioquire.oldfaithful.core.api.Worldgen;

/** The real folder, the real commands, where the world runs. A file in, a result out, and what the game itself then says happened. */
public final class StagehandTest extends WorldTest {
	private static final int FAR = 3000;

	private void script(String id, String text, Consumer<Map<String, Object>> after) {
		then(() -> Scripts.send(id, text));
		await(id + " to end", 600, () -> Scripts.over(id));
		then(() -> after.accept(Scripts.result(id)));
	}

	/**
	 * A script that starts from the middle of the plot, in the same tick. A plot by the sea can have water come across it and carry the player off between scripts. Its first step isn't counted, so what
	 * the checks read is the script as given.
	 */
	private void fromHome(String id, String text, Consumer<Map<String, Object>> after) {
		script(id, home() + "\n" + text, r -> {
			Scripts.steps(r).remove(0);
			after.accept(r);
		});
	}

	private double number(Map<String, Object> data, String key) {
		Object value = data.get(key);
		return value instanceof Number ? ((Number) value).doubleValue() : Double.NaN;
	}

	private int count(Item item) {
		int total = 0;
		for (ItemStack stack : player.inventory.main) {
			if (stack != null && stack.itemId == item.id) total += stack.count;
		}
		return total;
	}

	private boolean empty() {
		for (ItemStack stack : player.inventory.main) {
			if (stack != null) return false;
		}
		for (ItemStack stack : player.inventory.armor) {
			if (stack != null) return false;
		}
		return true;
	}

	private String home() {
		return "tp " + x + " " + y + " " + z;
	}

	private boolean atHome() {
		return Math.abs(player.x - (x + 0.5)) < 0.01 && Math.abs(player.boundingBox.minY - y) < 0.01 && Math.abs(player.z - (z + 0.5)) < 0.01;
	}

	private boolean portalNear(World in) {
		int cx = MathHelper.floor(player.x), cy = MathHelper.floor(player.boundingBox.minY), cz = MathHelper.floor(player.z);
		for (int dx = -6; dx <= 6; dx++) {
			for (int dy = -6; dy <= 6; dy++) {
				for (int dz = -6; dz <= 6; dz++) {
					if (in.getBlockId(cx + dx, cy + dy, cz + dz) == Block.NETHER_PORTAL.id) return true;
				}
			}
		}
		return false;
	}

	@Override
	protected void script() {
		scenes();
		if (restarted()) {
			script("after-restart", "status", r -> check("a script runs after a restart", "done".equals(r.get("state"))));
			then(() -> {
				Map<String, Object> left = Scripts.result("left-running");
				check("a script left running when the world closed says so: " + (left == null ? null : left.get("state")), left != null && ("stopped".equals(left.get("state")) || "lost".equals(left.get("state"))));
				int far = x + 2000;
				world.getChunkSource().loadChunk(far >> 4, z >> 4);
				check("a block set in a chunk nobody had loaded lasted", world.getBlockId(far, 70, z) == Block.GLASS.id);
				world.setBlock(far, 70, z, 0);
			});
			return;
		}
		// Once something has told a world about the folder it stays, so this can only be asked first.
		check("nothing's there until someone drops a script", !Files.exists(Scripts.folder()));
		badInput();
		items();
		health();
		teleports();
		places();
		clockAndSky();
		mobs();
		blocks();
		waits();
		nether();
		then(() -> Scripts.send("left-running", "wait 30m\n"));
		await("the script left running to start", 200, () -> Scripts.result("left-running") != null);
	}

	private void badInput() {
		List<String> lines = Arrays.asList("give diamond", "fly away", "tp", "give nosuchitem", "give slab@4", "give dye@16", "give stone -4", "give stone 99999", "setblock 0 999 0 stone", "setblock 0 64 0 nosuchblock",
				"setblock 0 64 0 piston@6", "setblock 0 64 0 stone@99", "fill 0 0 0 1000 100 1000 stone", "tp NaN 64 0", "tp 1e9 64 0", "tp 99999999 64 0", "tp 0 500 0", "time banana", "weather sideways", "wait forever",
				"spawn notamob", "kit nosuchkit", "tp nosuchplace", "locate nosuchplace", "clear something", "list everything", "status radius=9999", "give stone 1 bogus=3");
		script("bad", String.join("\n", lines) + "\n", r -> {
			List<?> errors = (List<?>) r.get("errors");
			check("every bad line is said back, and only those: " + (errors == null ? 0 : errors.size()), errors != null && errors.size() == lines.size() - 1);
			check("the script with bad lines ran none of its good ones", "failed".equals(r.get("state")) && Scripts.steps(r).isEmpty() && empty());
		});
		then(() -> Scripts.sendBytes("garbage", new byte[] {(byte) 0xFF, (byte) 0xFE, 0, 1, 2, (byte) 0xC0, 10}, 0));
		await("garbage to end", 200, () -> Scripts.over("garbage"));
		then(() -> check("bytes that aren't text are refused", "not_text".equals(Scripts.errorCode(Scripts.result("garbage")))));
		then(() -> Scripts.sendBytes("huge", new byte[70000], 0));
		await("huge to end", 200, () -> Scripts.over("huge"));
		then(() -> check("a huge file is refused unread", "too_big".equals(Scripts.errorCode(Scripts.result("huge")))));
		then(() -> Scripts.sendBytes("stale", "give diamond\n".getBytes(StandardCharsets.UTF_8), 11 * 60 * 1000));
		await("stale to end", 200, () -> Scripts.over("stale"));
		then(() -> check("a script given up on long ago isn't run", "expired".equals(Scripts.result("stale").get("state")) && empty()));
		script("empty", "# only a comment\n\n", r -> check("nothing in a script is an error", "empty".equals(Scripts.errorCode(r))));
		script("late", "give diamond\nas NobodyAtAll\ngive diamond\n", r -> {
			check("trouble only found running stops there: " + r.get("not_run"), "failed".equals(r.get("state")) && "no_player".equals(Scripts.errorCode(r)) && Scripts.steps(r).size() == 2);
			check("what came before it stands, and what comes after doesn't run", count(Item.DIAMOND) == 1);
		});
		script("relative-y", "clear\nsetblock ~ ~999 ~ stone\n", r -> check("a coordinate worked out too high is caught at the time", "bad_coordinate".equals(Scripts.errorCode(r))));
	}

	private void items() {
		script("give", "clear\ngive iron_sword\ngive wool@14 3\ngive stone 200\ngive minecraft:iron_helmet 1 wear\nstatus\n", r -> {
			check("give: things by name, damage and count", count(Item.IRON_SWORD) == 1 && count(Item.ITEMS[Block.STONE.id]) == 200 && count(Item.ITEMS[Block.WOOL.id]) == 3);
			check("give: wear", player.inventory.armor[3] != null && player.inventory.armor[3].itemId == Item.IRON_HELMET.id);
			List<?> inventory = (List<?>) Scripts.data(r, 5).get("inventory");
			check("status reads the inventory by name", String.valueOf(inventory).contains("iron_sword") && String.valueOf(inventory).contains("wool"));
		});
		script("full", "clear\ngive diamond_sword 40\n", r -> {
			check("a tool doesn't stack, and what doesn't fit is said: " + Scripts.data(r, 1), number(Scripts.data(r, 1), "given") == 36 && number(Scripts.data(r, 1), "no_room_for") == 4);
		});
		script("kits", "clear\nkit wood\nclear\nkit stone\nclear\nkit diamond\nclear\nkit builder\nclear\nkit iron\n", r -> {
			check("every kit gives what it says", "done".equals(r.get("state")));
			check("a kit puts armour on and the rest away", player.inventory.armor[3] != null && player.inventory.armor[0] != null && player.inventory.armor[0].itemId == Item.IRON_BOOTS.id && count(Item.IRON_SWORD) == 1 && count(Item.ARROW) == 64);
		});
		for (String mod : Arrays.asList("fixture-machine:stamp", "fixture-machine:lamp", "water-bottles:water_bottle")) {
			if (!FabricLoader.getInstance().isModLoaded(mod.substring(0, mod.indexOf(':')))) continue;
			script("give-mod", "clear\ngive " + mod + " 1\nstatus\n", r -> check("a mod's by its name: " + mod, count(Names.item(mod)) == 1 && String.valueOf(Scripts.data(r, 2).get("inventory")).contains(mod)));
		}
		script("cleared", "clear\n", r -> check("clear takes it all", empty()));
	}

	private void health() {
		// The steps read the health as they run. By the time the game's own is read here, a tick or two on, Peaceful may have healed a half-heart (beta does each second of a player's age),
		// and a test server has no monsters, so it's on Peaceful.
		script("health", "health 5\nheal 3\n", r -> {
			check("health and a little healing: " + Scripts.data(r, 0).get("health") + " then " + Scripts.data(r, 1).get("health"),
					number(Scripts.data(r, 0), "health") == 5 && number(Scripts.data(r, 1), "health") == 8);
			check("and the game has it: " + player.health + " on difficulty " + world.difficulty, player.health == 8 || world.difficulty == 0 && player.health > 8);
		});
		script("heal", "heal\n", r -> check("heal", player.health == 20));
	}

	private void teleports() {
		fromHome("near", "tp ~3 ~ ~2\nstatus\n" + home() + "\n", r -> {
			check("a short hop lands where it's asked: " + Scripts.data(r, 0), number(Scripts.data(r, 0), "x") == x + 3.5 && number(Scripts.data(r, 0), "z") == z + 2.5 && Boolean.FALSE.equals(Scripts.data(r, 0).get("moved_for_safety")));
			check("and status agrees", number(Scripts.data(r, 1), "x") == x + 3.5 && "stone".equals(Scripts.data(r, 1).get("standing_on")));
			check("and home", atHome());
		});
		fromHome("into-floor", "tp ~ ~-1 ~\n", r -> {
			check("into the floor comes out on top of it: " + Scripts.data(r, 0), number(Scripts.data(r, 0), "y") == y && Boolean.TRUE.equals(Scripts.data(r, 0).get("moved_for_safety")));
		});
		fromHome("beside-lava", "setblock ~3 ~-1 ~ lava\ntp ~3 ~ ~\nsetblock " + (x + 3) + " " + (y - 1) + " " + z + " stone\n" + home() + "\n", r -> {
			Map<String, Object> landed = Scripts.data(r, 1);
			double away = Math.max(Math.abs(number(landed, "x") - (x + 3.5)), Math.abs(number(landed, "z") - (z + 0.5)));
			check("never over lava or beside it: " + landed, Boolean.TRUE.equals(landed.get("moved_for_safety")) && (away > 1.5 || number(landed, "y") > y + 1));
		});
		fromHome("exact", "tp ~ ~20 ~ exact\nstatus\n" + home() + "\n", r -> check("exact goes there regardless: " + Scripts.data(r, 1).get("y"), number(Scripts.data(r, 1), "y") >= y + 19.5 && atHome()));
		fromHome("yaw", "tp ~ ~ ~ yaw=90 pitch=10\nstatus\ntp ~ ~ ~ yaw=0 pitch=0\n", r -> check("facing", number(Scripts.data(r, 1), "yaw") == 90.0 && number(Scripts.data(r, 1), "pitch") == 10.0));
		// New land, three thousand blocks out, and back. On a server the client's half of this is in the scenes.
		fromHome("far", "tp ~" + FAR + " ~\nstatus\n", r -> {
			Map<String, Object> landed = Scripts.data(r, 0);
			check("far, on chunks that weren't there: " + landed, Math.abs(number(landed, "x") - (x + FAR)) <= 17 && !"air".equals(landed.get("standing_on")));
			check("loaded and solid underfoot where they are", Chunks.isLoaded(player.world, MathHelper.floor(player.x) >> 4, MathHelper.floor(player.z) >> 4));
			check("not inside anything", player.world.getEntityCollisions(player, player.boundingBox.contract(0.01, 0.01, 0.01)).isEmpty());
			int cx = MathHelper.floor(player.x), cz = MathHelper.floor(player.z);
			boolean open = true;
			for (int above = MathHelper.floor(player.boundingBox.minY) + 2; above < 128; above++) {
				net.minecraft.block.material.Material over = player.world.getMaterial(cx, above, cz);
				if (over != net.minecraft.block.material.Material.LEAVES && (over.isFluid() || over.blocksMovement())) open = false;
			}
			check("on the surface, not in a cave under it: y " + player.boundingBox.minY, open);
		});
		script("far-back", home() + "\n", r -> check("back from far", atHome()));
	}

	private void places() {
		fromHome("places", "tp test-place floor=3\nlocate test-place floor=2\nlocate spawn\nlist places\n" + home() + "\n", r -> {
			check("a mod's kind of place, through its entrypoint: " + Scripts.data(r, 0), number(Scripts.data(r, 0), "x") == x + 3.5 && String.valueOf(Scripts.data(r, 0).get("place")).contains("floor"));
			check("locate answers with the options, from where they stand then", number(Scripts.data(r, 1), "x") == x + 5);
			check("the spawn needs no mod", "overworld".equals(Scripts.data(r, 2).get("dim")));
			check("the list has it", String.valueOf(Scripts.data(r, 3).get("places")).contains("test-place"));
		});
		script("place-none", "locate test-place none\n", r -> check("a mod saying there's none", "no_such_place".equals(Scripts.errorCode(r))));
		script("place-bad", "tp test-place bad\n", r -> check("a mod refusing a word", "bad_place".equals(Scripts.errorCode(r))));
		script("place-floor", "tp test-place floor=x\n", r -> check("a number that isn't", "bad_place".equals(Scripts.errorCode(r))));
	}

	private void clockAndSky() {
		long[] before = new long[1];
		then(() -> before[0] = world.getTime());
		script("time", "time noon\n", r -> {
			long hour = world.getTime() % 24000;
			check("time: noon by the clock, the ticks since allowed: " + hour, hour >= 6000 && hour < 6600);
			check("time: never backward", world.getTime() >= before[0]);
		});
		script("time-back", "time dawn\ntime 1234\n", r -> {
			long hour = world.getTime() % 24000;
			check("time: dawn then a number: " + hour, hour >= 1234 && hour < 1834);
			check("time: still never backward", world.getTime() >= before[0]);
		});
		script("rain", "weather rain\n", r -> check("rain", world.getProperties().getRaining() && !world.getProperties().getThundering() && Boolean.TRUE.equals(Scripts.data(r, 0).get("raining"))));
		script("thunder", "weather thunder 30s\n", r -> check("thunder, for a while", world.getProperties().getThundering() && world.getProperties().getRainTime() <= 600));
		script("clear", "weather clear\n", r -> check("clear", !world.getProperties().getRaining() && !world.getProperties().getThundering()));
	}

	private void mobs() {
		fromHome("spawn", "spawn pig 2 at ~3 ~ ~3\nspawn zombie\nstatus radius=16\n", r -> {
			check("spawn at a place: " + Scripts.data(r, 0), number(Scripts.data(r, 0), "spawned") == 2 && number(Scripts.data(r, 0), "alive") == 2 && "Pig".equals(Scripts.data(r, 0).get("entity")));
			check("spawn in front, whatever the world keeps: " + Scripts.data(r, 1), number(Scripts.data(r, 1), "spawned") == 1);
			check("status sees them", String.valueOf(Scripts.data(r, 2).get("nearby")).contains("Pig"));
		});
		then(() -> {
			@SuppressWarnings("unchecked")
			List<Entity> around = player.world.collectEntitiesByClass(LivingEntity.class, Box.create(x - 16, y - 8, z - 16, x + 16, y + 8, z + 16));
			for (Entity entity : around) {
				if (!(entity instanceof PlayerEntity)) entity.markDead();
			}
		});
		wait(2);
	}

	private void blocks() {
		String a = (x + 2) + " " + y + " " + (z + 2), b = (x + 3) + " " + (y + 1) + " " + (z + 4);
		fromHome("blocks", "setblock ~2 ~ ~ glass\nsetblock ~-2 ~ ~ wool@14\nfill " + a + " " + b + " cobblestone\n", r -> {
			check("setblock", block(2, 0, 0) == Block.GLASS.id);
			check("setblock with its meta", block(-2, 0, 0) == Block.WOOL.id && meta(-2, 0, 0) == 14);
			check("fill: a box of them", number(Scripts.data(r, 2), "cells") == 12 && block(3, 1, 4) == Block.COBBLESTONE.id && block(2, 0, 2) == Block.COBBLESTONE.id);
		});
		fromHome("fill-big", "fill ~-8 ~3 ~-8 ~8 ~12 ~8 glass\n", r -> check("a fill bigger than a tick's share, all of it", number(Scripts.data(r, 0), "cells") == 17 * 17 * 10 && block(8, 12, 8) == Block.GLASS.id && block(-8, 3, -8) == Block.GLASS.id));
		fromHome("fill-away", "fill ~-8 ~3 ~-8 ~8 ~12 ~8 air\nfill " + a + " " + b + " air\nsetblock ~2 ~ ~ air\nsetblock ~-2 ~ ~ air\n", r -> check("and away with air", block(8, 12, 8) == 0 && block(2, 0, 2) == 0 && block(2, 0, 0) == 0));
		script("unloaded", "setblock " + (x + 2000) + " 70 " + z + " glass\n", r -> {
			check("a chunk nobody had loaded takes it: " + r.get("state"), "done".equals(r.get("state")));
			check("and reads back", world.getBlockId(x + 2000, 70, z) == Block.GLASS.id);
		});
		if (FabricLoader.getInstance().isModLoaded("fixture-machine")) {
			fromHome("mod-block", "setblock ~2 ~ ~ fixture-machine:lamp\n", r -> check("a mod's block by name", block(2, 0, 0) == Names.block("fixture-machine:lamp").id));
			fromHome("mod-block-away", "setblock ~2 ~ ~ air\n", r -> check("gone again", block(2, 0, 0) == 0));
		}
	}

	private void waits() {
		// The inbox looks once a second, real time: with faster game time (-Ptest.speed) the wait stays as long in seconds.
		int ticks = 30 * 50 / Integer.getInteger("oldfaithful.test.tickMs", 50);
		then(() -> Scripts.send("slow", "wait " + ticks + "\ngive apple\nwait until near ~ ~ ~ radius=2\n"));
		await("slow to start", 200, () -> Scripts.result("slow") != null);
		script("meanwhile", "status\n", r -> check("another script runs while one waits", "done".equals(r.get("state")) && !Scripts.over("slow")));
		await("slow to end", 200, () -> Scripts.over("slow"));
		then(() -> check("the waits end and the script goes on", "done".equals(Scripts.result("slow").get("state")) && count(Item.APPLE) == 1));
		script("never", "wait until near 9000 64 9000 timeout=1s\ngive apple\n", r -> check("a wait that times out stops the script", "timed_out".equals(Scripts.errorCode(r)) && count(Item.APPLE) == 1));
		then(() -> Scripts.send("long", "wait 30m\ngive apple\n"));
		await("long to start", 200, () -> Scripts.result("long") != null);
		script("cancel", "cancel long\n", r -> check("cancel says how many: " + Scripts.data(r, 0), number(Scripts.data(r, 0), "cancelled") == 1));
		await("long to end", 200, () -> Scripts.over("long"));
		then(() -> check("cancelled, and it didn't go on", "cancelled".equals(Scripts.result("long").get("state")) && count(Item.APPLE) == 1));
		StringBuilder longLine = new StringBuilder("say"), oneWord = new StringBuilder("say ");
		for (int i = 0; i < 60; i++) longLine.append(" word").append(i);
		for (int i = 0; i < 300; i++) oneWord.append('x');
		script("say", longLine + "\nsay short\n" + oneWord + "\nclear\n", r -> check("say takes what a chat line can't: " + Scripts.data(r, 0), number(Scripts.data(r, 0), "lines") >= 4 && number(Scripts.data(r, 2), "lines") >= 3));
	}

	private void nether() {
		if (!singlePlayer()) {
			script("nether-off", "tp 8 64 8 dim=nether\n", r -> check("a server with allow-nether=false says so", "nether_off".equals(Scripts.errorCode(r)) && atHome()));
			then(() -> NetherSwitch.set(player, true));
		}
	}

	private void scenes() {
		scene("far-start", args -> {
			then(() -> Scripts.send("client-far", "tp ~" + FAR + " ~\n"));
			await("client-far to end", 600, () -> Scripts.over("client-far"));
			then(() -> check("the far move, with a client there: " + Scripts.result("client-far").get("state"), "done".equals(Scripts.result("client-far").get("state"))));
		});
		scene("home", args -> {
			then(() -> Scripts.send("client-home", home() + " dim=overworld\n"));
			await("client-home to end", 900, () -> Scripts.over("client-home"));
			then(() -> {
				check("home, with a client there: " + Scripts.result("client-home").get("state"), "done".equals(Scripts.result("client-home").get("state")));
				if (singlePlayer()) world = Server.world(Worldgen.OVERWORLD);
			});
		});
		scene("nether-start", args -> {
			then(() -> Scripts.send("client-nether", "tp 40 64 40 dim=nether\nstatus\n"));
			await("client-nether to end", 900, () -> Scripts.over("client-nether"));
			then(() -> {
				Map<String, Object> r = Scripts.result("client-nether");
				check("to the Nether: " + Scripts.data(r, 0) + " " + r.get("error"), "done".equals(r.get("state")) && "nether".equals(Scripts.data(r, 1).get("dim")));
				check("it's where the game thinks they are", player.dimensionId == Worldgen.NETHER && player.world.dimension.isNether);
				check("standing on something, in something safe: " + Scripts.data(r, 0).get("standing_on"), !"air".equals(Scripts.data(r, 0).get("standing_on")) && !"lava".equals(Scripts.data(r, 1).get("in")));
				check("no portal was built", !portalNear(player.world));
			});
		});
		scene("nether-end", args -> {
			then(() -> {
				check("no portal stayed behind on this side", !portalNear(player.world));
				if (!singlePlayer()) NetherSwitch.set(player, false);
			});
		});
		scene("empty-hands", args -> {
			then(() -> Scripts.send("client-clear", "clear\n"));
			await("client-clear to end", 300, () -> Scripts.over("client-clear"));
		});
	}
}
