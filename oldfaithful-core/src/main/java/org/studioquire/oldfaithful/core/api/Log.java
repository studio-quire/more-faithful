package org.studioquire.oldfaithful.core.api;

import java.util.Random;

import net.minecraft.block.LogBlock;

/**
 * A mod's log, by vanilla's rules: leaves of any mod count it as a tree, breaking it marks them to decay, axes are fast on it and it burns. It drops itself.
 * <pre>{@code
 * c.block("my_log", s -> new Log(s, s.texture("my_log"), s.texture("my_log_top"))).standIn(Block.LOG);
 * }</pre>
 * Vanilla's own {@code LogBlock} knows only its own tiles and drops {@code Block.LOG}, so extend this rather than it. Add it to {@code c:logs} yourself.
 */
public class Log extends LogBlock {
	private final int side, end;

	/** @param side the bark's tile, from {@code s.texture} or {@code s.vanillaTile}; @param end the rings' */
	public Log(Content.Spec s, int side, int end) {
		super(s.id());
		this.side = side;
		this.end = end;
		textureId = side;
		setHardness(2.0F);
		setSoundGroup(WOOD_SOUND_GROUP);
		disableTrackingStatistics();
		ignoreMetaUpdates();
		Fire.flammable(this, 5, 5);
		Tools.effective(this, "axe");
	}

	@Override
	public int getTexture(int face, int meta) {
		return face < 2 ? end : side;
	}

	@Override
	public int getDroppedItemId(int meta, Random random) {
		return id;
	}

	@Override
	protected int getDroppedItemMeta(int meta) {
		return 0;
	}
}
