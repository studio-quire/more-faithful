package org.studioquire.faithful.fixtureore;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.studioquire.oldfaithful.core.api.BlockEntry;
import org.studioquire.oldfaithful.core.api.BoolRule;
import org.studioquire.oldfaithful.core.api.Content;
import org.studioquire.oldfaithful.core.api.Drops;
import org.studioquire.oldfaithful.core.api.FaithfulMod;
import org.studioquire.oldfaithful.core.api.ItemEntry;
import org.studioquire.oldfaithful.core.api.Mod;

/** What a small ore mod looks like, written as a mod author would from the API. */
public final class FixtureOre implements FaithfulMod {
	public static final class Ore extends Block {
		Ore(int id, int texture) {
			super(id, texture, Material.STONE);
			setHardness(3.0F);
			setResistance(5.0F);
		}

		@Override
		public int getDroppedItemId(int meta, java.util.Random random) {
			return ingot.get().id;
		}
	}

	public static final class Metal extends Block {
		Metal(int id, int texture) {
			super(id, texture, Material.METAL);
			setHardness(5.0F);
		}
	}

	public static final class Ingot extends Item {
		Ingot(int id, int texture) {
			super(id);
			setTextureId(texture);
		}
	}

	public static BlockEntry<Ore> ore;
	public static BlockEntry<Metal> metal;
	public static ItemEntry<Ingot> ingot;
	private static BoolRule rich;

	@Override
	public void content(Content c) {
		ore = c.block("copper_ore", s -> new Ore(s.id(), s.texture("copper_ore"))).standIn(Block.STONE);
		metal = c.block("copper_block", s -> new Metal(s.id(), s.texture("copper_block"))).standIn(Block.IRON_BLOCK);
		ingot = c.item("copper_ingot", s -> new Ingot(s.id(), s.texture("copper_ingot"))).standIn(Item.IRON_INGOT);
	}

	@Override
	public void init(Mod mod) {
		rich = mod.rules().bool("rich_ore", false);
		Drops.block(d -> {
			if (rich.get() && d.block == ore.get()) d.stacks.add(new ItemStack(ingot.get()));
		});
	}
}
