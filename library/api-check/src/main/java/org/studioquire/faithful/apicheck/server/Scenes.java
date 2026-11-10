package org.studioquire.faithful.apicheck.server;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.ServerWorld;
import net.minecraft.world.chunk.Chunk;
import org.studioquire.faithful.apicheck.ApiCheck;
import org.studioquire.faithful.apicheck.Checks;
import org.studioquire.faithful.apicheck.ApiCheck.Critter;
import org.studioquire.faithful.apicheck.ApiCheck.Kiln;
import org.studioquire.faithful.apicheck.ApiCheck.MachineEntity;
import org.studioquire.faithful.apicheck.ApiCheck.Raft;
import org.studioquire.oldfaithful.core.api.BlockEntities;
import org.studioquire.oldfaithful.core.api.Commands;
import org.studioquire.oldfaithful.core.api.Containers;
import org.studioquire.oldfaithful.core.api.Sounds;
import org.studioquire.oldfaithful.core.api.Tags;
import org.studioquire.oldfaithful.core.impl.content.Overlay;
import org.studioquire.oldfaithful.core.api.Server;

/** Scenes on the real server, by console or chat: {@code apitest <scene>}. Each ends by printing DONE, for the task. */
public final class Scenes {
	/** Inside one chunk at the spawn. */
	static final int Y = 100;

	private Scenes() {
	}

	private static void check(String what, boolean ok) {
		System.out.println("[faithful-test] " + (ok ? "PASS " : "FAIL ") + what);
	}

	static int baseX(ServerWorld w) {
		return w.getSpawnPos().x >> 4 << 4;
	}

	static int baseZ(ServerWorld w) {
		return w.getSpawnPos().z >> 4 << 4;
	}

	private static MachineEntity machine(ServerWorld w, int i) {
		return (MachineEntity) w.getBlockEntity(baseX(w) + i, Y, baseZ(w) + 1);
	}

	@SuppressWarnings("unchecked")
	private static ServerPlayerEntity player() {
		List<PlayerEntity> players = Server.players();
		return players.isEmpty() ? null : (ServerPlayerEntity) players.get(0);
	}

	public static void register() {
		Commands.register("apitest", "apitest <scene>: test only", (args, reply) -> {
			ServerWorld w = (ServerWorld) Server.world(0);
			try {
				run(w, args);
			} catch (IOException e) {
				check("the scene " + args[0] + ": " + e, false);
			}
			System.out.println("[faithful-test] DONE " + args[0]);
		});
	}

	private static void run(ServerWorld w, String[] args) throws IOException {
		int x = baseX(w), z = baseZ(w);
		ServerPlayerEntity p = player();
		switch (args[0]) {
			case "prepare":
				for (Object e : w.entities) {
					if (e instanceof Critter || e instanceof Raft) ((net.minecraft.entity.Entity) e).markDead();
				}
				for (int i = 0; i < 14; i++) {
					w.setBlock(x + i, Y, z + 1, ApiCheck.machine.get().id);
					machine(w, i).value = 11 + i;
				}
				machine(w, 0).add(123);
				break;
			case "one":
				machine(w, 0).setValue(21);
				break;
			case "few":
				for (int i = 1; i <= 3; i++) machine(w, i).setValue(30 + i);
				break;
			case "many":
				for (int i = 0; i < 12; i++) machine(w, i).setValue(40 + i);
				break;
			case "common":
				Checks.recipes(Scenes::check);
				Checks.furnace(w, x + 2, Y + 3, z + 3, Scenes::check);
				Checks.storage(w, x + 2, Y + 4, z + 3, Scenes::check);
				check("the tag vocabulary", vocabulary());
				break;
			case "tools":
				Checks.tools(w, p, x + 2, Y + 5, z + 3, (bx, by, bz) -> p.interactionManager.tryBreakBlock(bx, by, bz), Scenes::check);
				break;
			case "spawn": {
				Critter critter = new Critter(w);
				critter.setPositionAndAngles(p.x + 2, p.y, p.z, 40.0F, 0.0F);
				critter.note = "from the server";
				critter.setMood(5);
				w.spawnEntity(critter);
				Raft raft = new Raft(w);
				raft.setPositionAndAngles(p.x - 2, p.y, p.z, 0.0F, 0.0F);
				raft.planks = 7;
				w.spawnEntity(raft);
				break;
			}
			case "persisted":
				check("a block entity's value survived a restart: " + machine(w, 1).value, machine(w, 1).value == 41);
				check("its charge too: " + machine(w, 0).charge(), machine(w, 0).charge() == 123);
				check("an old name of a block entity and an entity load as what they are now", aliases(w));
				int critters = 0;
				for (Object e : w.entities) {
					if (e instanceof Critter && "from the server".equals(((Critter) e).note)) critters++;
				}
				check("a mod's mob survived a restart: " + critters, critters >= 1);
				break;
			case "open":
				Containers.open(p, "api-check:kiln", machine(w, 2));
				break;
			case "heat":
				machine(w, 2).heat = Integer.parseInt(args[1]);
				break;
			case "clicked": {
				MachineEntity m = machine(w, 2);
				check("the click moved the stack on the server", m.getStack(0) == null && p.inventory.getCursorStack() != null && p.inventory.getCursorStack().count == 4);
				check("the server's handler is the kiln", p.currentScreenHandler instanceof Kiln);
				p.inventory.setCursorStack(null);
				break;
			}
			case "fill":
				machine(w, 2).setStack(0, new net.minecraft.item.ItemStack(ApiCheck.ingot.get(), 4));
				BlockEntities.sync(machine(w, 2));
				break;
			case "sound":
				Sounds.play(w, p.x, p.y, p.z, "api-check.ping", 1.0F, 1.0F);
				break;
			case "dumpgen":
				dump(w, args[1]);
				break;
			default:
				check("no such scene: " + args[0], false);
		}
	}

	private static boolean aliases(ServerWorld w) {
		net.minecraft.nbt.NbtCompound be = new net.minecraft.nbt.NbtCompound();
		be.putString("id", "oldmod:machine");
		net.minecraft.nbt.NbtCompound entity = new net.minecraft.nbt.NbtCompound();
		entity.putString("id", "oldmod:critter");
		entity.put("Pos", new net.minecraft.nbt.NbtList());
		return net.minecraft.block.entity.BlockEntity.createFromNbt(be) instanceof MachineEntity && net.minecraft.entity.EntityRegistry.create("oldmod:critter", w) instanceof Critter;
	}

	private static boolean vocabulary() throws IOException {
		String file = System.getProperty("apicheck.tags");
		if (file == null) return true;
		java.util.Set<String> known = new java.util.HashSet<>();
		for (String line : Files.readAllLines(Paths.get(file))) {
			if (!line.trim().isEmpty() && !line.startsWith("#")) known.add(line.trim());
		}
		boolean all = true;
		for (String tag : Tags.names()) {
			if (!known.contains(tag)) {
				check("the tag " + tag + " isn't in library/tags.txt", false);
				all = false;
			}
		}
		return all;
	}

	/**
	 * New chunks far from the spawn, generated here and written before any tick has touched them (liquids flow and leaves decay by the world's own random, which differs each
	 * run): vanilla's array as it stands, then the overlay, for the task to compare across runs. The ring round the 49 is so they're all decorated.
	 */
	private static void dump(ServerWorld w, String file) throws IOException {
		if (System.getProperty("apicheck.worldgen", "all").equals("all")) {
			ServerWorld nether = (ServerWorld) Server.world(-1);
			Chunk chunk = nether.chunkCache.loadChunk(40, 40);
			check("a chunk-built feature in the Nether", chunk.getBlockId(5, 1, 5) == ApiCheck.marker.get().id && Overlay.vanillaBlocks(chunk)[5 << 11 | 5 << 7 | 1] == net.minecraft.block.Block.BEDROCK.id);
		}
		int cx = 96, cz = -96;
		for (int dx = -4; dx <= 4; dx++) {
			for (int dz = -4; dz <= 4; dz++) w.chunkCache.loadChunk(cx + dx, cz + dz);
		}
		try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(Paths.get(file))))) {
			out.writeInt(w.getSpawnPos().x);
			out.writeInt(w.getSpawnPos().z);
			out.writeInt(ApiCheck.ore.get().id);
			out.writeInt(ApiCheck.otherOre.get().id);
			out.writeInt(ApiCheck.marker.get().id);
			for (int dx = -3; dx <= 3; dx++) {
				for (int dz = -3; dz <= 3; dz++) {
					Chunk c = w.getChunk(cx + dx, cz + dz);
					out.write(Overlay.vanillaBlocks(c));
					char[] ext = Overlay.of(c).faithful$ext();
					for (int i = 0; i < Overlay.CELLS; i++) out.writeShort(ext == null ? 0 : ext[i]);
				}
			}
		}
	}
}
