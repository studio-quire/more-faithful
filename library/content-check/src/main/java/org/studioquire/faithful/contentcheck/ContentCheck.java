package org.studioquire.faithful.contentcheck;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import org.studioquire.oldfaithful.core.api.BlockEntry;
import org.studioquire.oldfaithful.core.api.Content;
import org.studioquire.oldfaithful.core.api.FaithfulMod;
import org.studioquire.oldfaithful.core.api.ItemEntry;
import org.studioquire.oldfaithful.core.api.Mod;

/** A little of everything, for the tests. */
public final class ContentCheck implements FaithfulMod {
	public static final class Solid extends Block {
		public Solid(int id, int texture) {
			super(id, texture, Material.STONE);
			setHardness(1.0F);
		}
	}

	public static final class See extends Block {
		public int breaks;
		/** What the block's meta read as in onBreak. */
		public int metaInBreak = -1;

		public See(int id, int texture) {
			super(id, texture, Material.GLASS);
			setHardness(0.3F);
			setOpacity(0);
		}

		@Override
		public boolean isOpaque() {
			return false;
		}

		@Override
		public void onBreak(net.minecraft.world.World world, int x, int y, int z) {
			breaks++;
			metaInBreak = world.getBlockMeta(x, y, z);
		}
	}

	public static final class Thing extends Item {
		public Thing(int id, int texture) {
			super(id);
			setTextureId(texture);
		}
	}

	public static BlockEntry<Solid> solid;
	public static BlockEntry<See> see;
	public static BlockEntry<Solid> plain;
	public static BlockEntry<Solid> first, second;
	public static ItemEntry<Thing> thing;

	@Override
	public void content(Content c) {
		solid = c.block("solid", s -> new Solid(s.id(), s.texture("solid"))).standIn(Block.COBBLESTONE);
		see = c.block("see", s -> new See(s.id(), s.texture("see"))).standIn(Block.GLASS);
		plain = c.block("no_item", s -> new Solid(s.id(), s.vanillaTile(2))).noItem();
		first = c.block("family_a", s -> new Solid(s.id(), s.vanillaTile(1)));
		second = c.block("family_z", s -> new Solid(s.id(), s.vanillaTile(1)));
		c.block("family_m", s -> new Solid(s.id(), s.vanillaTile(1)));
		c.family("family_a", "family_z");
		thing = c.item("thing", s -> new Thing(s.id(), s.texture("thing"))).standIn(Item.PAPER);
		c.alias("oldmod:solid", "solid");
	}

	@Override
	public void init(Mod mod) {
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) ContentScenes.register();
	}
}
