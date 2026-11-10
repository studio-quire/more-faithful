package org.studioquire.faithful.contentcheck;

import net.minecraft.block.Block;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.DoubleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.ServerWorld;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AlphaChunkStorage;
import org.studioquire.oldfaithful.core.api.Commands;
import org.studioquire.oldfaithful.core.api.Stacks;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;
import org.studioquire.oldfaithful.core.api.Server;

/** Scenes on the real server, by console command: {@code contest <scene>}. */
final class ContentScenes {
	static final int Y = 100;

	private ContentScenes() {
	}

	static final int BIG_DAMAGE = 3, BIG_SLOT = 35, CROWD_DAMAGE = 4;
	private static final int BIG_BYTES = Stacks.MAX_WIRE_BYTES + 4000;
	private static final int CHEST_X = 20;

	private static void check(String what, boolean ok) {
		System.out.println("[faithful-test] " + (ok ? "PASS " : "FAIL ") + what);
	}

	static void register() {
		Commands.register("contest", "contest <scene>: test only", (args, reply) -> {
			ServerWorld w = (ServerWorld) Server.world(0);
			Vec3i s = w.getSpawnPos();
			switch (args[0]) {
				case "ids": ids(); break;
				case "place": place(w, s.x, s.z); break;
				case "verify": verify(w, s.x, s.z); break;
				case "stack": stack(); break;
				case "stash": stash(w, s.x, s.z); break;
				case "stashed": stashed(w, s.x, s.z); break;
				case "big": big(); break;
				case "crowd": crowd(); break;
				case "bigchest": bigChest(w); break;
				case "one": w.setBlock(s.x, Y + 6, s.z, ContentCheck.solid.get().id, 5); break;
				case "few": for (int i = 0; i < 5; i++) w.setBlock(s.x + i, Y + 7, s.z, ContentCheck.see.get().id, i + 1); break;
				case "many": for (int i = 0; i < 40; i++) w.setBlock(s.x + i % 8, Y + 8, s.z + i / 8, ContentCheck.solid.get().id, 3); break;
				default: reply.say("no such scene");
			}
			System.out.println("[faithful-test] DONE " + args[0]);
		});
	}

	private static void ids() {
		int solid = ContentCheck.solid.get().id, a = ContentCheck.first.get().id, z = ContentCheck.second.get().id;
		check("blocks number from 512: " + solid, solid >= 512 && solid < 4096);
		check("an item numbers from 4096: " + ContentCheck.thing.get().id, ContentCheck.thing.get().id >= 4096);
		check("a family is consecutive: " + a + ", " + z, z == a + 1);
		check("a block's item has its id", net.minecraft.item.Item.ITEMS[solid] != null);
		check("no_item has none", net.minecraft.item.Item.ITEMS[ContentCheck.plain.get().id] == null);
		check("an alias finds the block", ContentRegistry.blockId("oldmod:solid") == solid);
		check("vanilla's tables intact", Block.BLOCKS[1] == Block.STONE && net.minecraft.item.Item.ITEMS[256] != null && net.minecraft.item.Item.ITEMS[2256] != null);
	}

	private static void place(ServerWorld w, int x, int z) {
		int solid = ContentCheck.solid.get().id, see = ContentCheck.see.get().id;
		for (int i = 0; i < 16; i++) w.setBlock(x + i, Y, z, solid, i);
		w.setBlock(x, Y + 1, z, see, 7);
		Chunk c = w.getChunk(x >> 4, z >> 4);
		int lx = x & 15, lz = z & 15, cell = lx << 11 | lz << 7 | Y + 1;
		check("the id and meta read back", w.getBlockId(x, Y + 1, z) == see && w.getBlockMeta(x, Y + 1, z) == 7);
		check("vanilla's array holds the stand-in", c.blocks[cell] == Block.GLASS.id && c.meta.get(lx, Y + 1, lz) == 0);
		ContentCheck.See block = ContentCheck.see.get();
		block.metaInBreak = -1;
		w.setBlock(x, Y + 1, z, see, 3);
		check("a mod block's meta in onBreak is its own old one, for a mod block over it: " + block.metaInBreak, block.metaInBreak == 7);
		w.setBlock(x, Y + 1, z, Block.STONE.id);
		check("and for a vanilla block over it: " + block.metaInBreak, block.metaInBreak == 3);
		check("gone from the overlay", w.getBlockId(x, Y + 1, z) == Block.STONE.id && w.getBlockMeta(x, Y + 1, z) == 0);
		w.setBlock(x, Y + 1, z, Block.BUTTON.id, 2);
		w.setBlock(x, Y + 1, z, see, 4);
		check("a vanilla block's onBreak still sees its own meta under a mod block", w.getBlockId(x, Y + 1, z) == see && w.getBlockMeta(x, Y + 1, z) == 4);
		w.setBlock(x, Y + 2, z, see, 4);
		int light = c.getHeight(lx, lz);
		check("a clear mod block doesn't raise the height map: " + light, light <= Y + 1);
		w.setBlock(x, Y + 3, z, solid);
		check("an opaque one does: " + c.getHeight(lx, lz), c.getHeight(lx, lz) == Y + 4);
		w.setBlock(x, Y + 3, z, 0);
		NbtCompound nbt = new NbtCompound();
		AlphaChunkStorage.saveChunkToNbt(c, w, nbt);
		check("the chunk's NBT has the compound", nbt.contains("Faithful") && nbt.getCompound("Faithful").getList("Names").size() >= 2);
		check("its Blocks hold a stand-in", nbt.getByteArray("Blocks")[lx << 11 | lz << 7 | Y] == Block.COBBLESTONE.id);
	}

	private static void verify(ServerWorld w, int x, int z) {
		int solid = ContentCheck.solid.get().id;
		boolean all = true;
		for (int i = 0; i < 16; i++) all &= w.getBlockId(x + i, Y, z) == solid && w.getBlockMeta(x + i, Y, z) == i;
		check("after a restart every cell is the block it was, by name (solid is " + solid + " now)", all);
		check("and the clear block too", w.getBlockId(x, Y + 2, z) == ContentCheck.see.get().id && w.getBlockMeta(x, Y + 2, z) == 4);
	}

	@SuppressWarnings("unchecked")
	private static void stack() {
		ItemStack s = new ItemStack(ContentCheck.thing.get(), 3, 2);
		Stacks.data(s).putString("note", "kept");
		NbtCompound nbt = new NbtCompound();
		s.writeNbt(nbt);
		check("a stack's NBT has a real vanilla id", nbt.getShort("id") == net.minecraft.item.Item.PAPER.id);
		ItemStack back = new ItemStack(nbt);
		check("and reads back whole", back.itemId == s.itemId && back.count == 3 && back.getDamage() == 2 && "kept".equals(Stacks.data(back).getString("note")));
		ItemStack neverSaved = new ItemStack(ContentCheck.thing.get(), 3, 2);
		Stacks.data(neverSaved).putString("note", "kept");
		check("saving a stack doesn't change what it equals, or a server would send it again after every save", ItemStack.areEqual(s, neverSaved) && ItemStack.areEqual(back, neverSaved));
		NbtCompound tree = new NbtCompound();
		s.writeNbt(tree);
		Stacks.data(s).putString("note", "changed");
		check("and what's saved is the save's own, whatever the stack does while it's being written", "kept".equals(Stacks.data(new ItemStack(tree)).getString("note")));
		Stacks.data(s).putString("note", "kept");
		NbtCompound gone = new NbtCompound();
		s.writeNbt(gone);
		gone.getCompound("Faithful").putString("id", "gone:thing");
		ItemStack standIn = new ItemStack(gone);
		check("an item whose mod has gone reads as its stand-in, with its data", standIn.itemId == net.minecraft.item.Item.PAPER.id && standIn.count == 3 && "kept".equals(Stacks.data(standIn).getString("note")));
		ItemStack half = s.split(1);
		check("a split keeps the data", "kept".equals(Stacks.data(half).getString("note")));
		ItemStack other = new ItemStack(ContentCheck.thing.get(), 1, 2);
		check("stacks with different data don't merge", !Stacks.canMerge(s, other) && !s.isItemEqual(other) && Stacks.canMerge(s, half));
		ItemStack block = new ItemStack(ContentCheck.solid.get(), 1);
		nbt = new NbtCompound();
		block.writeNbt(nbt);
		check("a block item's stand-in is a real item, the stand-in block's", nbt.getShort("id") == Block.COBBLESTONE.id);
		block = new ItemStack(ContentCheck.first.get(), 1);
		nbt = new NbtCompound();
		block.writeNbt(nbt);
		check("with a stand-in of air it would be a stick; family_a's is stone", nbt.getShort("id") == Block.STONE.id);
		for (PlayerEntity p : Server.players()) {
			ItemStack give = new ItemStack(ContentCheck.thing.get(), 1, 2);
			Stacks.data(give).putString("note", "from the server");
			p.inventory.addStack(give);
			ItemStack big = new ItemStack(ContentCheck.thing.get(), 1, BIG_DAMAGE);
			Stacks.data(big).putByteArray("blob", new byte[BIG_BYTES]);
			check("a stack can be measured against the cap: " + Stacks.wireBytes(big), Stacks.wireBytes(big) > Stacks.MAX_WIRE_BYTES);
			p.inventory.addStack(big);
			p.currentScreenHandler.sendContentUpdates();
		}
	}

	private static void stash(ServerWorld w, int x, int z) {
		w.setBlock(x + CHEST_X, Y, z, Block.CHEST.id);
		ItemStack s = new ItemStack(ContentCheck.thing.get(), 3, 2);
		Stacks.data(s).putString("note", "in a chest");
		((ChestBlockEntity) w.getBlockEntity(x + CHEST_X, Y, z)).setStack(4, s);
	}

	private static void stashed(ServerWorld w, int x, int z) {
		ItemStack s = ((ChestBlockEntity) w.getBlockEntity(x + CHEST_X, Y, z)).getStack(4);
		check("after a restart a chest's stack is itself, its data with it: " + s,
				s != null && s.itemId == ContentCheck.thing.get().id && s.count == 3 && s.getDamage() == 2 && "in a chest".equals(Stacks.data(s).getString("note")));
	}

	/** A client has moved the big stack, with only the item to go on. */
	private static void big() {
		for (PlayerEntity p : Server.players()) {
			ItemStack moved = p.inventory.main[BIG_SLOT];
			check("a client's clicks move a stack too big for the wire, and it keeps its data: " + moved,
					moved != null && moved.getDamage() == BIG_DAMAGE && Stacks.data(moved).getByteArray("blob").length == BIG_BYTES);
		}
	}

	/** A window sends its stacks twice, in one packet and then slot by slot. */
	private static void bigChest(ServerWorld w) {
		for (PlayerEntity player : Server.players()) {
			ServerPlayerEntity p = (ServerPlayerEntity) player;
			// Within a window's reach, or the server closes it on its next tick.
			int x = (int) Math.floor(p.x), y = (int) Math.floor(p.y) + 3, z = (int) Math.floor(p.z);
			w.setBlock(x, y, z, Block.CHEST.id);
			w.setBlock(x + 1, y, z, Block.CHEST.id);
			ChestBlockEntity left = (ChestBlockEntity) w.getBlockEntity(x, y, z), right = (ChestBlockEntity) w.getBlockEntity(x + 1, y, z);
			for (int i = 0; i < 54; i++) {
				ItemStack s = new ItemStack(ContentCheck.thing.get(), 1, CROWD_DAMAGE);
				Stacks.data(s).putByteArray("blob", new byte[Stacks.MAX_WIRE_BYTES - 1000 + i]);
				(i < 27 ? left : right).setStack(i % 27, s);
			}
			p.openChestScreen(new DoubleInventory("Large chest", left, right));
		}
	}

	/** Every slot past what one packet carries, in the packet that sends them all. */
	private static void crowd() {
		for (PlayerEntity player : Server.players()) {
			ServerPlayerEntity p = (ServerPlayerEntity) player;
			for (int i = 0; i < p.inventory.main.length; i++) {
				p.inventory.main[i] = new ItemStack(ContentCheck.thing.get(), 1, CROWD_DAMAGE);
				Stacks.data(p.inventory.main[i]).putByteArray("blob", new byte[15000 + i]);
			}
			p.skipPacketSlotUpdates = true; // as a click's reply does: this one packet says it all
			p.currentScreenHandler.sendContentUpdates();
			p.skipPacketSlotUpdates = false;
			p.onContentsUpdate(p.currentScreenHandler, p.currentScreenHandler.getStacks());
		}
	}
}
