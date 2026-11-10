package org.studioquire.faithful.harness.client;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.studioquire.faithful.harness.ModIds;
import org.studioquire.faithful.harness.MonkeyServer;
import org.studioquire.faithful.harness.MonkeyState;
import org.studioquire.faithful.harness.Out;
import org.studioquire.oldfaithful.core.api.Channel;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;

/**
 * A generic bot on a real client: every mod item on every block and every item on every mod block, through the game's own interaction paths; every block mined; every screen
 * that opens clicked, slot by slot. After each bench it asks the server how things stand and compares that with its own world, twice, so only what stays different counts.
 */
final class Monkey {
	private static final int SETTLE = 7, RECHECK = 10, TICKS_A_CELL = 3, TICKS_FOR_SCREEN = 8, CLICKS_A_TICK = 4, SAME_FINDINGS = 3;
	private static final int BUDGET_TICKS = Integer.getInteger("faithful.harness.monkeyTicks", 6000);
	/** Burns the bot where it stands. */
	private static final Set<Integer> LEFT_OUT = new HashSet<>(Arrays.asList(327));

	private final class Bench {
		final int item, count;
		final int[] blocks;
		final boolean mine;

		Bench(int item, int count, int[] blocks, boolean mine) {
			this.item = item;
			this.count = count;
			this.blocks = blocks;
			this.mine = mine;
		}

		String mods() {
			Set<String> mods = new TreeSet<>();
			if (ModIds.item(item).indexOf(':') > 0) mods.add(ModIds.item(item).split(":")[0]);
			for (int b : blocks) {
				if (ModIds.block(b).indexOf(':') > 0) mods.add(ModIds.block(b).split(":")[0]);
			}
			return mods.isEmpty() ? "unknown" : String.join(", ", mods);
		}

		@Override
		public String toString() {
			List<String> names = new ArrayList<>();
			for (int b : blocks) names.add(ModIds.block(b));
			return (mine ? "mining " : ModIds.item(item) + " on ") + names;
		}
	}

	private enum Step { SET_UP, WAIT_READY, ACT, MINE, SETTLE, ASK, COMPARE, RECHECK, SCREEN_ASK, SCREEN_CLICK, SCREEN_SETTLE, SCREEN_COMPARE, SCREEN_CLOSE, CLOSE_SYNC }

	private final Channel channel;
	private final ArrayDeque<Bench> plan = new ArrayDeque<>();
	private final Set<String> reported = new HashSet<>();
	private final Map<String, Integer> findings = new java.util.HashMap<>();
	private final Random random = new Random(5);
	private Step step = Step.SET_UP;
	private Bench bench;
	private int x, y, z, cell, wait, ticks, benches, click, closeAnswers;
	private boolean ready;
	private MonkeyState answer, before;
	private List<String> firstDiff;
	private List<int[]> clicks;
	private int planned;

	Monkey(Channel channel) {
		this.channel = channel;
		channel.onClient(this::receive);
		makePlan();
	}

	private static void add(List<Integer> list, int id) {
		if (!list.contains(id)) list.add(id);
	}

	private void benches(int item, int count, List<Integer> blocks) {
		for (int i = 0; i < blocks.size(); i += MonkeyServer.CELLS.length) {
			List<Integer> part = blocks.subList(i, Math.min(blocks.size(), i + MonkeyServer.CELLS.length));
			plan.add(new Bench(item, count, part.stream().mapToInt(Integer::intValue).toArray(), false));
		}
	}

	/** Mods against mods first, then mods against vanilla, so a budget cuts the least telling pairs. */
	private void makePlan() {
		List<Integer> modBlocks = new ArrayList<>(), vanillaBlocks = new ArrayList<>(), modItems = new ArrayList<>(), vanillaItems = new ArrayList<>();
		for (int id = 1; id < Block.BLOCKS.length; id++) {
			if (Block.BLOCKS[id] == null) continue;
			if (id < 256) add(vanillaBlocks, id);
			else add(modBlocks, id);
		}
		for (int id = 1; id < Item.ITEMS.length; id++) {
			if (Item.ITEMS[id] == null || LEFT_OUT.contains(id)) continue;
			if (id >= ContentRegistry.FIRST_BLOCK && id != 2256 && id != 2257) add(modItems, id);
			else if (id >= 256) add(vanillaItems, id);
		}
		for (int item : modItems) benches(item, count(item), modBlocks);
		for (int item : modItems) benches(item, count(item), vanillaBlocks);
		for (int item : vanillaItems) benches(item, count(item), modBlocks);
		for (int i = 0; i < modBlocks.size(); i += MonkeyServer.CELLS.length) {
			List<Integer> part = modBlocks.subList(i, Math.min(modBlocks.size(), i + MonkeyServer.CELLS.length));
			plan.addFirst(new Bench(Item.DIAMOND_PICKAXE.id, 1, part.stream().mapToInt(Integer::intValue).toArray(), true));
		}
		planned = plan.size();
	}

	private static int count(int item) {
		return Math.min(Item.ITEMS[item].getMaxCount(), 24);
	}

	private void receive(PlayerEntity player, DataInputStream in) throws IOException {
		int answerKind = in.readUnsignedByte();
		if (answerKind == MonkeyServer.ANSWER_READY) {
			x = in.readInt();
			y = in.readInt();
			z = in.readInt();
			ready = true;
		} else if (answerKind == MonkeyServer.ANSWER_STATE) {
			answer = MonkeyState.read(in);
		}
	}

	private void ask() {
		answer = null;
		channel.toServer(out -> out.writeByte(MonkeyServer.OP_STATE));
	}

	private void go(Step next, int ticksToWait) {
		step = next;
		wait = ticksToWait;
	}

	/** True when it's all done. */
	boolean tick(Minecraft mc) {
		ticks++;
		if (wait > 0) {
			wait--;
			return false;
		}
		if (mc.player.health <= 0) {
			Out.fail(bench == null ? "unknown" : bench.mods(), "monkey-died", "the bot died at " + bench);
			return true;
		}
		switch (step) {
			case SET_UP:
				if (plan.isEmpty() || ticks > BUDGET_TICKS) {
					Out.info("monkey: " + benches + " of " + planned + " benches in " + ticks + " ticks, " + reported.size() + " findings");
					if (!plan.isEmpty()) Out.note("harness", "monkey-budget", benches + " of " + planned + " benches fitted the budget: mods against mods and mining come first");
					return true;
				}
				bench = plan.poll();
				benches++;
				ready = false;
				Bench b = bench;
				channel.toServer(out -> {
					out.writeByte(MonkeyServer.OP_SETUP);
					out.writeInt(b.item);
					out.writeByte(b.count);
					out.writeByte(b.blocks.length);
					for (int id : b.blocks) out.writeInt(id);
				});
				step = Step.WAIT_READY;
				return false;
			case WAIT_READY:
				if (!ready) return false;
				cell = 0;
				go(bench.mine ? Step.MINE : Step.ACT, 5);
				return false;
			case ACT:
				act(mc);
				return false;
			case MINE:
				mine(mc);
				return false;
			case SETTLE:
				ask();
				step = Step.ASK;
				return false;
			case ASK:
				if (answer == null) return false;
				go(Step.COMPARE, 3);
				return false;
			case COMPARE: {
				List<String> diff = diff(mc, answer);
				if (diff.isEmpty()) afterBench(mc);
				else {
					firstDiff = diff;
					ask();
					go(Step.RECHECK, RECHECK);
				}
				return false;
			}
			case RECHECK: {
				if (answer == null) return false;
				List<String> diff = diff(mc, answer);
				diff.retainAll(firstDiff);
				for (String line : diff) report(line);
				afterBench(mc);
				return false;
			}
			case SCREEN_ASK:
				if (answer == null) return false;
				before = answer;
				clicks = clickPlan(mc);
				click = 0;
				step = Step.SCREEN_CLICK;
				return false;
			case SCREEN_CLICK: {
				for (int i = 0; i < CLICKS_A_TICK && click < clicks.size(); i++, click++) {
					int[] c = clicks.get(click);
					try {
						mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, c[0], c[1], c[2] != 0, mc.player);
					} catch (RuntimeException e) {
						Out.fail(bench.mods(), "screen-threw", "clicking slot " + c[0] + " in " + mc.currentScreen + " at " + bench + ": " + e);
					}
				}
				if (click >= clicks.size()) go(Step.SCREEN_SETTLE, SETTLE);
				return false;
			}
			case SCREEN_SETTLE:
				ask();
				go(Step.SCREEN_COMPARE, 2);
				return false;
			case SCREEN_COMPARE: {
				if (answer == null) return false;
				for (String line : diff(mc, answer)) report(line);
				if (!answer.makes) {
					for (Map.Entry<String, Integer> e : answer.totals.entrySet()) {
						int was = before.totals.getOrDefault(e.getKey(), 0);
						if (e.getValue() > was) finding(bench.mods(), "item-count-grew", "clicking through " + screen(mc) + " turned " + was + " of " + e.getKey() + " into " + e.getValue() + " (" + bench + ")");
					}
				}
				step = Step.SCREEN_CLOSE;
				return false;
			}
			case SCREEN_CLOSE:
				mc.player.closeHandledScreen();
				if (cell >= bench.blocks.length) {
					go(Step.SET_UP, 3);
					return false;
				}
				closeAnswers = 0;
				ask();
				step = Step.CLOSE_SYNC;
				return false;
			case CLOSE_SYNC:
				if (answer == null) return false;
				/*
				 * A screen stops the uses, and the bench's other cells are still to come. But back on the player's own screen, a server sends that screen's slots again
				 * on its next tick, from what it last sent before the other one opened: already known here, so harmless, unless a use comes first. Then the use's guess
				 * is overwritten, and as the server's hand ends as the use's packet said, nothing corrects it (planks placed four ticks after a dispenser closed: one
				 * more on the client for good). Beta's own race; a second answer comes from a later server tick than the close, so after that resend.
				 */
				if (++closeAnswers < 2) {
					ask();
					return false;
				}
				go(bench.mine ? Step.MINE : Step.ACT, 3);
				return false;
			default:
				return true;
		}
	}

	private static String screen(Minecraft mc) {
		return mc.currentScreen == null ? "a screen" : mc.currentScreen.getClass().getSimpleName();
	}

	private void afterBench(Minecraft mc) {
		if (mc.currentScreen instanceof HandledScreen) {
			ask();
			step = Step.SCREEN_ASK;
		} else {
			step = Step.SET_UP;
		}
	}

	/** Every slot shifted out, then some left and right clicks here and there, as a player working a screen. */
	private List<int[]> clickPlan(Minecraft mc) {
		int slots = mc.player.currentScreenHandler.slots.size();
		List<int[]> list = new ArrayList<>();
		for (int i = 0; i < slots; i++) list.add(new int[] {i, 0, 1});
		for (int i = 0; i < 24; i++) list.add(new int[] {random.nextInt(slots), random.nextInt(2), 0});
		for (int i = 0; i < slots; i++) list.add(new int[] {i, 0, 1});
		return list;
	}

	/**
	 * One use at a time, a few ticks apart, as a right click repeats: a packet holds the stack itself and is written up to a tenth of a second later, so uses in one tick
	 * all say the count the last left, and vanilla's check of the count after a use desyncs by itself.
	 */
	private void act(Minecraft mc) {
		if (cell < bench.blocks.length && mc.currentScreen == null) {
			int cx = x + MonkeyServer.CELLS[cell][0], cz = z + MonkeyServer.CELLS[cell][1];
			ItemStack hand = mc.player.inventory.getSelectedItem();
			try {
				mc.interactionManager.interactBlock(mc.player, mc.world, hand, cx, y, cz, 1);
				if (hand != null && hand.count == 0) mc.player.inventory.main[mc.player.inventory.selectedSlot] = null;
			} catch (RuntimeException e) {
				Out.fail(bench.mods(), "client-threw", bench + ": " + e);
				e.printStackTrace(System.out);
			}
			// What may open a screen gets the time to: a use that lands while one is opening is vanilla's own desync.
			int id = mc.world.getBlockId(cx, y, cz);
			wait = id >= 256 || id == Block.CRAFTING_TABLE.id || Block.BLOCKS_WITH_ENTITY[id] ? TICKS_FOR_SCREEN : TICKS_A_CELL;
			cell++;
		}
		if (cell >= bench.blocks.length || mc.currentScreen != null) go(Step.SETTLE, SETTLE);
	}

	private int mining;

	private void mine(Minecraft mc) {
		if (cell >= bench.blocks.length) {
			go(Step.SETTLE, SETTLE);
			return;
		}
		int cx = x + MonkeyServer.CELLS[cell][0], cz = z + MonkeyServer.CELLS[cell][1];
		if (mc.world.getBlockId(cx, y, cz) != bench.blocks[cell] || mining > 60) {
			mining = 0;
			mc.interactionManager.cancelBlockBreaking();
			cell++;
			return;
		}
		if (mining++ == 0) mc.interactionManager.attackBlock(cx, y, cz, 1);
		else mc.interactionManager.processBlockBreakingAction(cx, y, cz, 1);
	}

	private List<String> diff(Minecraft mc, MonkeyState server) {
		MonkeyState here = MonkeyState.of(mc.world, mc.player, x, y, z);
		Set<String> theirs = new HashSet<>(server.lines), mine = new HashSet<>(here.lines);
		List<String> diff = new ArrayList<>();
		for (String line : here.lines) {
			if (!theirs.contains(line)) diff.add("client only: " + line);
		}
		for (String line : server.lines) {
			if (!mine.contains(line)) diff.add("server only: " + line);
		}
		// By what differs, so the two sides of one cell sit together and a capped report shows both.
		diff.sort(java.util.Comparator.comparing((String l) -> l.substring(l.indexOf(": ") + 2)));
		return diff;
	}

	private void report(String line) {
		String body = line.substring(line.indexOf(':') + 2);
		String kind = body.substring(0, body.indexOf(' '));
		String rule = kind.equals("cell") ? "ghost-block" : kind.equals("entity") ? "entity-differs" : "inventory-differs";
		finding(bench.mods(), rule, line + " after " + bench);
	}

	/** One bad mod shouldn't bury the report: the first few of a kind say what's wrong. */
	private void finding(String mods, String rule, String detail) {
		if (findings.merge(mods + rule, 1, Integer::sum) <= SAME_FINDINGS) Out.fail(mods, rule, detail);
		reported.add(detail);
	}
}
