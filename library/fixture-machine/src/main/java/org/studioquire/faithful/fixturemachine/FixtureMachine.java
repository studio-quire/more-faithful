package org.studioquire.faithful.fixturemachine;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.studioquire.oldfaithful.core.api.BlockEntry;
import org.studioquire.oldfaithful.core.api.Channel;
import org.studioquire.oldfaithful.core.api.Commands;
import org.studioquire.oldfaithful.core.api.Content;
import org.studioquire.oldfaithful.core.api.FaithfulMod;
import org.studioquire.oldfaithful.core.api.ItemEntry;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.Stacks;

/** What a small machine mod looks like: state in the block's number, data on a stack, a message, a command. */
public final class FixtureMachine implements FaithfulMod {
	public static final class Lamp extends Block {
		private final boolean lit;

		Lamp(int id, int texture, boolean lit) {
			super(id, texture, Material.GLASS);
			this.lit = lit;
			setHardness(0.3F);
			if (lit) setLuminance(1.0F);
		}

		@Override
		public boolean onUse(World world, int x, int y, int z, PlayerEntity player) {
			if (world.isRemote) return true;
			world.setBlock(x, y, z, (lit ? off : on).get().id, world.getBlockMeta(x, y, z));
			return true;
		}

		@Override
		public int getDroppedItemId(int meta, java.util.Random random) {
			return off.get().id;
		}
	}

	public static final class Stamp extends Item {
		Stamp(int id, int texture) {
			super(id);
			setTextureId(texture);
			setMaxCount(1);
		}

		@Override
		public boolean useOnBlock(ItemStack stack, PlayerEntity player, World world, int x, int y, int z, int side) {
			if (world.isRemote) return true;
			net.minecraft.nbt.NbtCompound data = Stacks.data(stack);
			data.putInt("uses", data.getInt("uses") + 1);
			return true;
		}
	}

	public static BlockEntry<Lamp> off, on;
	public static ItemEntry<Stamp> stamp;
	private static Channel ping;

	@Override
	public void content(Content c) {
		off = c.block("lamp", s -> new Lamp(s.id(), s.texture("lamp"), false)).standIn(Block.GLASS);
		on = c.block("lamp_lit", s -> new Lamp(s.id(), s.texture("lamp_lit"), true)).standIn(Block.GLOWSTONE).noItem();
		c.family("lamp", "lamp_lit");
		stamp = c.item("stamp", s -> new Stamp(s.id(), s.texture("stamp"))).standIn(Item.STICK);
	}

	@Override
	public void init(Mod mod) {
		ping = mod.channel("ping", 1).maxBytes(64).onServer((player, in) -> {
			int n = in.readInt();
			if (n < 0 || n > 1000) throw new java.io.IOException("not a ping: " + n);
			ping.toPlayer(player, out -> out.writeInt(n + 1));
		}).onClient((player, in) -> in.readInt());
		Commands.register("fixlamp", "fixlamp: a fixture", (args, reply) -> reply.say("lamps: " + off.get().id));
	}
}
