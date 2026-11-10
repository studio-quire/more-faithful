package org.studioquire.faithful.fixturebadone;

import java.util.Arrays;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.studioquire.oldfaithful.core.api.Commands;
import org.studioquire.oldfaithful.core.api.Content;
import org.studioquire.oldfaithful.core.api.Drops;
import org.studioquire.oldfaithful.core.api.FaithfulMod;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.Ticks;

/** Each mistake a mod is likely to make, on when {@code -Dfaithful.bad} names it. With none named this is a clean mod holding one block. */
public final class BadOne implements FaithfulMod {
	private static final List<String> ON = Arrays.asList(System.getProperty("faithful.bad", "").split(","));

	/** The block's number as it was when this was written. */
	public static final int HARD_CODED = 513;

	public static boolean on(String mistake) {
		return ON.contains(mistake);
	}

	private static net.minecraft.block.Block cleanBlock;

	public static final class Holder extends net.minecraft.block.entity.BlockEntity {
		private int held = cleanBlock.id;

		@Override
		public void readNbt(net.minecraft.nbt.NbtCompound nbt) {
			super.readNbt(nbt);
			held = nbt.getInt("held");
		}

		@Override
		public void writeNbt(net.minecraft.nbt.NbtCompound nbt) {
			super.writeNbt(nbt);
			nbt.putInt("held", held);
		}
	}

	public static final class HolderBlock extends net.minecraft.block.BlockWithEntity {
		HolderBlock(int id, int texture) {
			super(id, texture, Material.STONE);
			setHardness(1.0F);
		}

		@Override
		protected net.minecraft.block.entity.BlockEntity createBlockEntity() {
			return new Holder();
		}
	}

	public static class Plain extends Block {
		public Plain(int id, int texture) {
			super(id, texture, Material.STONE);
			setHardness(1.0F);
		}
	}

	/** Reaches past the Spec for a tile of vanilla's sheet on one side: drawn from the wrong atlas. */
	public static final class SplitAtlas extends Plain {
		public SplitAtlas(int id, int texture) {
			super(id, texture);
		}

		@Override
		public int getTexture(int side) {
			return side == 1 ? 1 : textureId;
		}
	}

	public static final class Stick extends Item {
		public Stick(int id, int texture) {
			super(id);
			setTextureId(texture);
		}
	}

	public static final class LooseItem extends Item {
		public LooseItem(int id) {
			super(id);
		}
	}

	@Override
	public void content(Content c) {
		org.studioquire.oldfaithful.core.api.BlockEntry<Plain> clean = c.block("clean", s -> new Plain(s.id(), s.texture("clean"))).standIn(Block.STONE);
		cleanEntry = clean;
		if (on("number_in_nbt")) {
			c.block("holder", s -> new HolderBlock(s.id(), s.texture("clean"))).standIn(Block.STONE);
			c.blockEntity("holder", Holder.class);
		}
		if (on("bad_name")) c.block("Bad Name", s -> new Plain(s.id(), s.texture("clean")));
		if (on("no_lang")) c.block("nameless", s -> new Plain(s.id(), s.texture("clean")));
		if (on("no_texture")) c.block("bare", s -> new Plain(s.id(), s.texture("not_there")));
		if (on("split_atlas")) c.block("split", s -> new SplitAtlas(s.id(), s.texture("clean")));
		if (on("lit_item")) c.block("lit", s -> new Plain(s.id(), s.texture("clean"))).standIn(Block.LIT_REDSTONE_ORE);
		if (on("entity_stand_in")) c.block("furnace", s -> new Plain(s.id(), s.texture("clean"))).standIn(Block.LIT_FURNACE);
		if (on("unsafe_stand_in")) c.block("burning", s -> new Plain(s.id(), s.texture("clean"))).standIn(Block.FIRE);
		if (on("twice_block")) {
			c.block("clean", s -> new Plain(s.id(), s.texture("clean")));
		}
		if (on("loose_block")) new Plain(200, 1);
		if (on("loose_item")) new LooseItem(1000);
	}

	private static org.studioquire.oldfaithful.core.api.BlockEntry<Plain> cleanEntry;

	@Override
	public void init(Mod mod) {
		cleanBlock = cleanEntry.get();
		// Believes a length it was sent, as a first draft does.
		if (on("greedy_channel")) mod.channel("greedy", 1).onServer((player, in) -> System.out.println(new byte[in.readInt()].length));
		if (on("twice_channel")) {
			mod.channel("same", 1);
			mod.channel("same", 1);
		}
		if (on("twice_rule")) {
			mod.rules().bool("same", false);
			mod.rules().bool("same", false);
		}
		if (on("twice_command")) Commands.register("samecommand", "a planted mistake", (args, reply) -> {
		});
		if (on("vanilla_command")) Commands.register("list", "a planted mistake", (args, reply) -> {
		});
		if (on("listener_throws")) Ticks.onServer(() -> {
			throw new IllegalStateException("a planted throw");
		});
		if (on("table_conflict")) Block.STONE.slipperiness = 0.7F;
		if (on("order_matters")) Drops.block(d -> {
			for (ItemStack s : d.stacks) s.count *= 2;
		});
	}
}
