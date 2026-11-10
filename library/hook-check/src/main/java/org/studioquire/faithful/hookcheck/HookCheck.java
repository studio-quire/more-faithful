package org.studioquire.faithful.hookcheck;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.chunk.Chunk;
import org.studioquire.faithful.hookcheck.server.ServerScenes;
import org.studioquire.oldfaithful.core.api.Attachment;
import org.studioquire.oldfaithful.core.api.Blocks;
import org.studioquire.oldfaithful.core.api.Chunks;
import org.studioquire.oldfaithful.core.api.Codec;
import org.studioquire.oldfaithful.core.api.Drops;
import org.studioquire.oldfaithful.core.api.Entities;
import org.studioquire.oldfaithful.core.api.FaithfulMod;
import org.studioquire.oldfaithful.core.api.Fire;
import org.studioquire.oldfaithful.core.api.Growth;
import org.studioquire.oldfaithful.core.api.Lifecycle;
import org.studioquire.oldfaithful.core.api.Lightning;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.Players;
import org.studioquire.oldfaithful.core.api.Ticks;
import org.studioquire.oldfaithful.core.impl.ChunkData;

/**
 * A test-only mod, never shipped: listens on every hook, and leaves the game alone until a check turns on one of the switches
 * below. {@code hooktest} scenes on the real server ({@code ServerScenes}) and the checks in the hidden client read what was heard.
 */
public final class HookCheck implements FaithfulMod {
	private static final Map<String, Integer> HEARD = new TreeMap<>();
	/** What the last listeners heard, in the order they spoke: a filter's order is read from this. */
	public static final List<String> SPOKEN = new ArrayList<>();

	public static boolean vetoBreak, vetoUse, vetoSpawn, vetoDamage, vetoIgnite, vetoBurn, cancelStrike, keepAll, bonemealHandled, bonemealCancelled, addDrop, doubleDamage;
	public static boolean keepFirstStack, destroySecondStack;
	/** For the isolation check: the listener on {@code Blocks.onPlaced} throws. */
	public static boolean throwing;
	public static int thrown;
	public static int[] strikeTo;
	/** The second mod's switches: it reads them, so this jar needn't know it. */
	public static boolean secondAddOne, secondSawCancelledBreak;
	public static int[] secondStrikeTo;
	public static final List<Fire.Cause> IGNITIONS = new ArrayList<>();
	public static PlayerEntity reborn, dead;
	public static Blocks.Placed lastPlaced;
	public static Drops.BlockDrops lastBlockDrops;
	public static Drops.MobDrops lastMobDrops;
	public static Players.DimensionChange lastDimension;
	public static Entities.Death lastDeath;
	/** What a section saw at load, from inside {@code loadChunkFromNbt}. */
	public static boolean sectionRanBeforeBlockEntities = true;
	public static int sectionLoads;

	public static Attachment<PlayerEntity, Integer> mana, rage, pouch, scratch;
	public static Attachment<Entity, String> tag;
	public static Attachment<Chunk, String> note;

	public static void heard(String what) {
		HEARD.merge(what, 1, Integer::sum);
	}

	public static int count(String what) {
		return HEARD.getOrDefault(what, 0);
	}

	/** Clears what a scene counts; who came and went is counted for the whole run. */
	public static void reset() {
		HEARD.keySet().removeIf(k -> !k.equals("joined") && !k.equals("left") && !k.equals("serverStop"));
		SPOKEN.clear();
		IGNITIONS.clear();
	}

	public static String summary() {
		return HEARD.toString();
	}

	@Override
	public void init(Mod mod) {
		mana = mod.attachment("mana", Attachment.player(Codec.INT)).persist().sync(Attachment.Sync.SELF).onDeath(Attachment.OnDeath.KEEP);
		rage = mod.attachment("rage", Attachment.player(Codec.INT)).persist().sync(Attachment.Sync.SELF).defaultValue(() -> 0);
		pouch = mod.attachment("pouch", Attachment.player(Codec.INT)).persist().onDeath(Attachment.OnDeath.DROP)
				.stacks(n -> Collections.singletonList(new ItemStack(Item.GOLD_INGOT, n)));
		scratch = mod.attachment("scratch", Attachment.player(Codec.INT)).onDeath(Attachment.OnDeath.KEEP);
		tag = mod.attachment("tag", Attachment.entity(Codec.STRING)).persist().sync(Attachment.Sync.TRACKING);
		note = mod.attachment("note", Attachment.chunk(Codec.STRING)).persist().sync(Attachment.Sync.TRACKING);

		ChunkData.section("hook-check", new ChunkData.Section() {
			@Override
			public void load(Chunk chunk, NbtCompound faithful) {
				sectionLoads++;
				if (!chunk.blockEntities.isEmpty()) sectionRanBeforeBlockEntities = false;
			}

			@Override
			public void save(Chunk chunk, NbtCompound faithful) {
			}
		});

		Lifecycle.onServerStart(() -> heard("serverStart"));
		Lifecycle.onServerStop(() -> {
			heard("serverStop");
			System.out.println("[faithful-test] " + (count("left") == count("joined") ? "PASS" : "FAIL") + " every player left before the stop: " + summary());
		});
		Lifecycle.onWorldLoad(w -> heard("worldLoad"));
		Lifecycle.onWorldSave(w -> heard("worldSave"));
		Lifecycle.onWorldUnload(w -> {
			heard("worldUnload");
			if (isServer() || count("serverStop") == 1) System.out.println("[faithful-test] " + (count("serverStop") == 1 ? "PASS" : "FAIL") + " a world unloads after the stop");
		});
		Ticks.onWorld(w -> heard("worldTick"));
		Ticks.onServer(() -> heard("serverTick"));
		Ticks.onClient(() -> heard("clientTick"));
		Chunks.onLoaded(c -> heard("chunkLoaded"));
		Chunks.onUnloaded(c -> heard("chunkUnloaded"));

		Players.onJoin(p -> heard("joined"));
		Players.onLeave(p -> heard("left"));
		Players.keepInventory(p -> keepAll);
		Players.onDrops(d -> {
			heard("deathDrops");
			List<ItemStack> stacks = d.stacks();
			if (keepFirstStack && stacks.size() > 0) d.keep(stacks.get(0));
			if (destroySecondStack && stacks.size() > 1) d.destroy(stacks.get(1));
		});
		Players.onRespawn((before, after) -> {
			heard("respawned");
			dead = before;
			reborn = after;
		});
		Players.onDimensionChange(c -> {
			heard("dimension");
			lastDimension = c;
		});

		Blocks.onPlaced(e -> {
			heard("placed");
			lastPlaced = e;
			if (throwing) {
				thrown++;
				throw new IllegalStateException("hook-check throws on purpose");
			}
		});
		Blocks.onBroken(e -> {
			heard("broken");
			if (vetoBreak) e.cancel();
		});
		Blocks.onUsed(e -> {
			heard("used");
			if (vetoUse) e.cancel();
		});
		Drops.block(d -> {
			heard("blockDrops");
			lastBlockDrops = d;
			if (addDrop) d.stacks.add(new ItemStack(Item.APPLE));
		});
		Drops.mob(d -> {
			heard("mobDrops");
			lastMobDrops = d;
			if (addDrop) d.stacks.add(new ItemStack(Item.APPLE));
		});
		Growth.onBonemeal(e -> {
			heard("bonemeal");
			if (bonemealHandled) e.handled();
			if (bonemealCancelled) e.cancel();
		});
		Growth.farmlandHolds(Block.DEAD_BUSH);

		Entities.onSpawn(e -> {
			heard("spawning");
			if (vetoSpawn && e.entity instanceof CowEntity) e.cancel();
		});
		Entities.onDamage(e -> {
			heard("damaged");
			SPOKEN.add("hook-check");
			if (doubleDamage) e.setAmount(e.amount() * 2);
			if (vetoDamage) e.cancel();
		});
		Entities.onDied(e -> {
			heard("died");
			lastDeath = e;
		});
		Entities.addNaturalSpawn(net.minecraft.world.biome.Biome.PLAINS, net.minecraft.entity.SpawnGroup.MONSTER, ZombieEntity.class, 1);

		Fire.flammable(Block.SPONGE, 30, 60);
		Fire.onIgnite(e -> {
			heard("ignite");
			IGNITIONS.add(e.cause);
			if (vetoIgnite) e.cancel();
		});
		Fire.onBurn(e -> {
			heard("burn");
			if (vetoBurn) e.cancel();
		});
		Lightning.onStrike(s -> {
			heard("strike");
			if (cancelStrike) s.cancel();
			if (strikeTo != null) s.proposeMoveTo(strikeTo[0], strikeTo[1], strikeTo[2]);
		});

		if (isServer()) ServerScenes.register();
	}

	private static boolean isServer() {
		return FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
	}

	/** The world-side hooks, none of which may ever speak on a client on a server. */
	public static final List<String> WORLD_SIDE = Arrays.asList("serverStart", "serverStop", "worldLoad", "worldSave", "worldUnload", "worldTick", "serverTick", "chunkLoaded", "chunkUnloaded",
			"joined", "left", "deathDrops", "respawned", "dimension", "placed", "broken", "used", "blockDrops", "mobDrops", "bonemeal", "spawning", "damaged", "died", "ignite", "burn", "strike");
}
