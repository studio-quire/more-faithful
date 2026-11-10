package org.studioquire.oldfaithful.core.api;

import java.util.Random;
import java.util.function.Supplier;

import net.minecraft.block.CropBlock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * A crop by wheat's rules: it grows on farmland by the same odds through meta 0 to {@value #RIPE}, keeps that farmland from drying out, ripens with one bonemeal, drops its produce when ripe and up
 * to three seeds as wheat does, all as one drop for {@link Drops#block}.
 * <pre>{@code
 * crop = c.block("my_crop", s -> new Crop(s, "my_crop", 8, () -> seeds.get(), () -> produce.get())).standIn(Block.WHEAT).noItem();
 * seeds = c.item("my_seeds", s -> new SeedsItem(s.id(), crop.get().id).setTextureId(s.texture("my_seeds"))).standIn(Item.SEEDS);
 * }</pre>
 * {@code stages} textures, {@code my_crop_0} up: 8 is one for each meta, and fewer spread evenly, the last for ripe only. {@code SeedsItem} is fine as it is: an id can't change while the game runs,
 * blocks are built before items, and {@code .get()} works in an item's factory for that reason; the rule against keeping a number is about what outlives the process.
 */
public class Crop extends CropBlock implements Growth.Bonemealable {
	public static final int RIPE = 7;

	private final int[] stages = new int[RIPE + 1];
	private final Supplier<? extends Item> seeds, produce;

	/** The items are asked for when something drops: they're built after blocks. */
	public Crop(Content.Spec s, String texture, int stages, Supplier<? extends Item> seeds, Supplier<? extends Item> produce) {
		super(s.id(), s.texture(texture + "_0"));
		if (stages < 2 || stages > RIPE + 1) throw new IllegalArgumentException("A crop has 2 to 8 textures, not " + stages);
		for (int meta = 0; meta <= RIPE; meta++) this.stages[meta] = s.texture(texture + "_" + (meta == RIPE ? stages - 1 : meta * (stages - 1) / RIPE));
		this.seeds = seeds;
		this.produce = produce;
		setHardness(0.0F);
		setSoundGroup(DIRT_SOUND_GROUP);
		disableTrackingStatistics();
		ignoreMetaUpdates();
		Growth.farmlandHolds(this);
	}

	@Override
	public int getTexture(int side, int meta) {
		return stages[meta < 0 || meta > RIPE ? RIPE : meta];
	}

	@Override
	public boolean grow(World world, int x, int y, int z, Random random) {
		if (world.getBlockMeta(x, y, z) >= RIPE) return false;
		applyFullGrowth(world, x, y, z);
		return true;
	}

	/** {@code CropBlock}'s names wheat seeds, so this one is whole. */
	@Override
	public void dropStacks(World world, int x, int y, int z, int meta, float luck) {
		if (world.isRemote) return;
		if (meta == RIPE && world.random.nextFloat() <= luck) dropStack(world, x, y, z, new ItemStack(produce.get()));
		for (int i = 0; i < 3; i++) {
			if (world.random.nextInt(15) <= meta) dropStack(world, x, y, z, new ItemStack(seeds.get()));
		}
	}

	@Override
	public int getDroppedItemId(int meta, Random random) {
		return meta == RIPE ? produce.get().id : -1;
	}
}
