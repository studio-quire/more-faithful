package org.studioquire.faithful.golems.client;

import net.minecraft.block.Block;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.world.BlockView;
import org.studioquire.faithful.golems.BoundaryPole;
import org.studioquire.oldfaithful.core.api.client.BlockShape;

/**
 * A pole a golem is using: its ring drawn as beta draws a lit torch, at its whole brightness whatever the light where
 * it stands and with no shading by side, which is what makes a lit redstone torch's head glow at night; and the iron under it lit about half, so it reads as a lit thing without
 * outshining the ring or a torch beside it.
 * <p>
 * The iron is drawn in the light where it stands but never less than {@link #SHAFT_LIGHT}, read as beta reads a block's own light, the floor a lamp gives its cell
 * ({@code getNaturalBrightness}'s minimum): so by day it's any block in daylight, and lit it's never dimmer than unlit. It's shaded by side as beta shades any block, so it stays a rod,
 * and drawn flat, as smooth lighting would only darken its foot. Its colours are only ever that one brightness times the shade, as beta's own blocks' are: Still Faithful's dusk keeps a
 * chunk's light per vertex and applies the darkness as it draws, which holds only for a drawing linear in what it reads. The first build of this chose between the cell's brightness
 * and a fixed half by comparing them, which that light can't carry: chunks it coded drew the iron dull and chunks drawn as beta's drew it bright, so poles near a redstone torch could
 * look darker than the rest (found 2026-11-10).
 */
public final class LitPoleShape implements BlockShape {
	/**
	 * The least light its iron is drawn in, as a light level: 12 is 0.53 of full at night, about half. 7 would be the pole's own light, the ring alone lit; 15 the whole rod lit as a
	 * torch's stick.
	 */
	public static final int SHAFT_LIGHT = 12;
	/** What's drawn with: {@link #SHAFT_LIGHT}, but for a look run comparing others. */
	public static int shaftLight = SHAFT_LIGHT;
	/** Beta's shading by side for any block: the faces along z at 0.8, along x at 0.6. */
	private static final float Z_SIDES = 0.8F, X_SIDES = 0.6F;

	@Override
	public boolean render(BlockRenderManager renderer, BlockView world, Block block, int x, int y, int z) {
		double minX = block.minX, minY = block.minY, minZ = block.minZ, maxX = block.maxX, maxY = block.maxY, maxZ = block.maxZ;
		int meta = world.getBlockMeta(x, y, z);
		Tessellator t = Tessellator.INSTANCE;
		// The iron's sides only: its top is under the ring, and its bottom on the block it stands on.
		block.setBoundingBox(BoundaryPole.LOW, 0.0F, BoundaryPole.LOW, BoundaryPole.HIGH, BoundaryPole.RING, BoundaryPole.HIGH);
		float light = world.getNaturalBrightness(x, y, z, shaftLight);
		t.color(light * Z_SIDES, light * Z_SIDES, light * Z_SIDES);
		renderer.renderEastFace(block, x, y, z, BlockShape.texture(renderer, block, 2, meta));
		renderer.renderWestFace(block, x, y, z, BlockShape.texture(renderer, block, 3, meta));
		t.color(light * X_SIDES, light * X_SIDES, light * X_SIDES);
		renderer.renderNorthFace(block, x, y, z, BlockShape.texture(renderer, block, 4, meta));
		renderer.renderSouthFace(block, x, y, z, BlockShape.texture(renderer, block, 5, meta));
		// The ring, whole.
		block.setBoundingBox(BoundaryPole.LOW, BoundaryPole.RING, BoundaryPole.LOW, BoundaryPole.HIGH, BoundaryPole.TOP, BoundaryPole.HIGH);
		t.color(1.0F, 1.0F, 1.0F);
		renderer.renderTopFace(block, x, y, z, BlockShape.texture(renderer, block, 1, meta));
		renderer.renderEastFace(block, x, y, z, BlockShape.texture(renderer, block, 2, meta));
		renderer.renderWestFace(block, x, y, z, BlockShape.texture(renderer, block, 3, meta));
		renderer.renderNorthFace(block, x, y, z, BlockShape.texture(renderer, block, 4, meta));
		renderer.renderSouthFace(block, x, y, z, BlockShape.texture(renderer, block, 5, meta));
		block.minX = minX;
		block.minY = minY;
		block.minZ = minZ;
		block.maxX = maxX;
		block.maxY = maxY;
		block.maxZ = maxZ;
		return true;
	}

	/** Never in a hand or a slot (the lit pole has no item); the plain one's look if it's ever asked. */
	@Override
	public boolean inventoryIn3d() {
		return false;
	}
}
