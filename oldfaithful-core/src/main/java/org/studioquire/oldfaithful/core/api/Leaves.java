package org.studioquire.oldfaithful.core.api;

import java.util.Random;
import java.util.function.Supplier;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShearsItem;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * A mod's leaves, by vanilla's rules: the core makes vanilla read any {@code LeavesBlock} as leaves and any {@code LogBlock} as a log, so these decay the same way, a broken log of any mod's marks
 * them, and fancy graphics reach them. Shears take them, quickly and with wear, and they burn as vanilla's do.
 * <pre>{@code
 * c.block("my_leaves", s -> new Leaves(s, "my_leaves", () -> sapling.get())).standIn(Block.LEAVES);
 * }</pre>
 * Two textures: {@code my_leaves}, with holes, for fancy, and {@code my_leaves_fast}, solid, for fast. Vanilla's tint isn't applied: a texture here has its own colour, and a subclass wanting the
 * biome's overrides {@code getColor} and {@code getColorMultiplier}, client-only. Meta: bit 8 belongs to decay (set means "look for a log on the next random tick"), and bit 4 means "never decays"
 * under Still Faithful, whose polish stops a leaf's tick there, so give it that meaning or leave it alone; bits 1 and 2 are yours. A leaf a player places is marked, as beta's is, so one out of reach
 * of a log falls. Override {@code onPlaced(World, int, int, int, LivingEntity)} for leaves that stay.
 */
public class Leaves extends LeavesBlock {
	private final int fancy, fast;
	private final Supplier<? extends Block> sapling;

	/** @param sapling what falls one time in twenty, or null for nothing */
	public Leaves(Content.Spec s, String texture, Supplier<? extends Block> sapling) {
		super(s.id(), s.texture(texture));
		this.fancy = textureId;
		this.fast = s.texture(texture + "_fast");
		this.sapling = sapling;
		setHardness(0.2F);
		setOpacity(1);
		setSoundGroup(DIRT_SOUND_GROUP);
		disableTrackingStatistics();
		ignoreMetaUpdates();
		Fire.flammable(this, 30, 60);
		Tools.effective(this, "shears");
		Tools.wearsShears(this);
	}

	@Environment(EnvType.CLIENT)
	@Override
	public void setFancyGraphics(boolean on) {
		super.setFancyGraphics(on);
		textureId = on ? fancy : fast;
	}

	@Override
	public int getTexture(int side, int meta) {
		return textureId;
	}

	@Environment(EnvType.CLIENT)
	@Override
	public int getColor(int meta) {
		return 0xFFFFFF;
	}

	@Environment(EnvType.CLIENT)
	@Override
	public int getColorMultiplier(BlockView view, int x, int y, int z) {
		return 0xFFFFFF;
	}

	@Override
	public void onPlaced(World world, int x, int y, int z, net.minecraft.entity.LivingEntity placer) {
		world.setBlockMetaWithoutNotifyingNeighbors(x, y, z, world.getBlockMeta(x, y, z) | 8);
	}

	@Override
	public int getDroppedItemId(int meta, Random random) {
		return sapling == null ? -1 : sapling.get().id;
	}

	@Override
	protected int getDroppedItemMeta(int meta) {
		return 0;
	}

	/** What shears take. */
	protected ItemStack shorn(int meta) {
		return new ItemStack(this);
	}

	@Override
	public void afterBreak(World world, PlayerEntity player, int x, int y, int z, int meta) {
		ItemStack held = player.getHand();
		if (world.isRemote || held == null || !(held.getItem() instanceof ShearsItem)) super.afterBreak(world, player, x, y, z, meta);
		else dropStack(world, x, y, z, shorn(meta));
	}
}
