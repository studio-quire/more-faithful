package org.studioquire.faithful.fixturebadmonkey;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.world.World;
import org.studioquire.oldfaithful.core.api.BlockEntry;
import org.studioquire.oldfaithful.core.api.Containers;
import org.studioquire.oldfaithful.core.api.Content;
import org.studioquire.oldfaithful.core.api.FaithfulMod;

/** Bugs an honest author could write, each a plain mistake about which side code runs on or what the server has to be told. */
public final class BadMonkey implements FaithfulMod {
	/** On the client only: the block appears there and nowhere else. */
	public static final class WrongSideWand extends Item {
		WrongSideWand(int id, int texture) {
			super(id);
			setTextureId(texture);
		}

		@Override
		public boolean useOnBlock(ItemStack stack, PlayerEntity player, World world, int x, int y, int z, int side) {
			if (world.isRemote) world.setBlock(x, y + 2, z, Block.GLASS.id); // not y + 1: the server sends the cell by the clicked face back after any use, which would mend it
			return true;
		}
	}

	/** Fills the client's inventory by itself: the server never heard. */
	public static final class GiftWand extends Item {
		GiftWand(int id, int texture) {
			super(id);
			setTextureId(texture);
		}

		@Override
		public boolean useOnBlock(ItemStack stack, PlayerEntity player, World world, int x, int y, int z, int side) {
			if (world.isRemote) player.inventory.main[20] = new ItemStack(Item.DIAMOND, 3);
			return true;
		}
	}

	/** Drops something only the client can see. */
	public static final class MirageWand extends Item {
		MirageWand(int id, int texture) {
			super(id);
			setTextureId(texture);
		}

		@Override
		public boolean useOnBlock(ItemStack stack, PlayerEntity player, World world, int x, int y, int z, int side) {
			if (world.isRemote) world.spawnEntity(new ItemEntity(world, x + 0.5, y + 1.5, z + 0.5, new ItemStack(Item.DIAMOND)));
			return true;
		}
	}

	/** Writes straight into the chunk, which tells no client. */
	public static final class Silent extends Block {
		Silent(int id, int texture) {
			super(id, texture, Material.STONE);
			setHardness(1.0F);
		}

		@Override
		public boolean onUse(World world, int x, int y, int z, PlayerEntity player) {
			if (world.isRemote) return true;
			world.getChunk(x >> 4, z >> 4).setBlock(x & 15, y + 2, z & 15, Block.GLOWSTONE.id);
			return true;
		}
	}

	public static final class Chest extends Block {
		Chest(int id, int texture) {
			super(id, texture, Material.WOOD);
			setHardness(1.0F);
		}

		@Override
		public boolean onUse(World world, int x, int y, int z, PlayerEntity player) {
			if (!world.isRemote) Containers.open(player, "fixture-bad-monkey:dupe", new net.minecraft.block.entity.ChestBlockEntity());
			return true;
		}
	}

	/** Shift-click leaves the stack where it was as well as putting it in the box. */
	public static final class Dupe extends ScreenHandler {
		public Dupe(PlayerEntity player, Inventory inventory, NbtCompound data) {
			for (int i = 0; i < 9; i++) addSlot(new Slot(inventory, i, 8 + i * 18, 18));
			for (int row = 0; row < 3; row++) {
				for (int column = 0; column < 9; column++) addSlot(new Slot(player.inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
			}
			for (int column = 0; column < 9; column++) addSlot(new Slot(player.inventory, column, 8 + column * 18, 142));
		}

		@Override
		public ItemStack quickMove(int index) {
			Slot from = (Slot) slots.get(index);
			if (index < 9 || !from.hasStack()) return null;
			for (int i = 0; i < 9; i++) {
				Slot to = (Slot) slots.get(i);
				if (!to.hasStack()) {
					to.setStack(from.getStack().copy());
					return null;
				}
			}
			return null;
		}

		@Override
		public boolean canUse(PlayerEntity player) {
			return true;
		}
	}

	@Override
	public void content(Content c) {
		c.item("wrong_side_wand", s -> new WrongSideWand(s.id(), s.texture("wand")));
		c.item("gift_wand", s -> new GiftWand(s.id(), s.texture("wand")));
		c.item("mirage_wand", s -> new MirageWand(s.id(), s.texture("wand")));
		c.block("silent", s -> new Silent(s.id(), s.texture("box"))).standIn(Block.STONE);
		c.block("dupe_box", s -> new Chest(s.id(), s.texture("box"))).standIn(Block.PLANKS);
		c.screen("dupe", Dupe::new);
	}
}
