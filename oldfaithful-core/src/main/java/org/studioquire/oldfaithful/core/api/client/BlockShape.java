package org.studioquire.oldfaithful.core.api.client;

import net.minecraft.block.Block;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.world.BlockView;
import org.lwjgl.opengl.GL11;
import org.studioquire.oldfaithful.core.impl.content.client.ShapeAccess;

/**
 * How a block of a mod's own render type is drawn, given to {@link Renderers#blockShape}. In the world, {@code render} adds to the chunk's tessellator; for a slot, a hand or the
 * ground, {@code renderItem} draws at the origin, as {@code BlockRenderManager.render(Block, int, float)} does.
 * <p>
 * Drawing faces through {@code renderer.renderBlock} or its {@code render...Face} calls honours a texture override by itself: that's the block-breaking cracks. Quads made by hand ask
 * {@link #texture}. The block's bounds are what collision and the outline read too: they're the shape's to change, and to put back as they were.
 */
public interface BlockShape {
	/** Whether anything was drawn. */
	boolean render(BlockRenderManager renderer, BlockView world, Block block, int x, int y, int z);

	default void renderItem(BlockRenderManager renderer, Block block, int meta, float brightness) {
	}

	/** Shown in slots and hands as a small block, with {@link #renderItem}; false to show the item's flat sprite, as a torch has. */
	default boolean inventoryIn3d() {
		return true;
	}

	/** What to draw a face with: the block's tile, or the override in force. */
	static int texture(BlockRenderManager renderer, Block block, int side, int meta) {
		int override = ((ShapeAccess) renderer).faithful$textureOverride();
		return override >= 0 ? override : block.getTexture(side, meta);
	}

	/** The common shape: a few boxes, each {@code {minX, minY, minZ, maxX, maxY, maxZ}} in 0 to 1, in the world and in the hand. The block's own bounds are left as they were. */
	static BlockShape boxes(float[]... boxes) {
		return new BlockShape() {
			@Override
			public boolean render(BlockRenderManager renderer, BlockView world, Block block, int x, int y, int z) {
				double minX = block.minX, minY = block.minY, minZ = block.minZ, maxX = block.maxX, maxY = block.maxY, maxZ = block.maxZ;
				boolean drawn = false;
				for (float[] b : boxes) {
					block.setBoundingBox(b[0], b[1], b[2], b[3], b[4], b[5]);
					drawn |= renderer.renderBlock(block, x, y, z);
				}
				block.minX = minX;
				block.minY = minY;
				block.minZ = minZ;
				block.maxX = maxX;
				block.maxY = maxY;
				block.maxZ = maxZ;
				return drawn;
			}

			@Override
			public void renderItem(BlockRenderManager renderer, Block block, int meta, float brightness) {
				double minX = block.minX, minY = block.minY, minZ = block.minZ, maxX = block.maxX, maxY = block.maxY, maxZ = block.maxZ;
				Tessellator t = Tessellator.INSTANCE;
				GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
				for (float[] b : boxes) {
					block.setBoundingBox(b[0], b[1], b[2], b[3], b[4], b[5]);
					t.startQuads();
					t.normal(0.0F, -1.0F, 0.0F);
					renderer.renderBottomFace(block, 0.0, 0.0, 0.0, block.getTexture(0, meta));
					t.draw();
					t.startQuads();
					t.normal(0.0F, 1.0F, 0.0F);
					renderer.renderTopFace(block, 0.0, 0.0, 0.0, block.getTexture(1, meta));
					t.draw();
					t.startQuads();
					t.normal(0.0F, 0.0F, -1.0F);
					renderer.renderEastFace(block, 0.0, 0.0, 0.0, block.getTexture(2, meta));
					t.draw();
					t.startQuads();
					t.normal(0.0F, 0.0F, 1.0F);
					renderer.renderWestFace(block, 0.0, 0.0, 0.0, block.getTexture(3, meta));
					t.draw();
					t.startQuads();
					t.normal(-1.0F, 0.0F, 0.0F);
					renderer.renderNorthFace(block, 0.0, 0.0, 0.0, block.getTexture(4, meta));
					t.draw();
					t.startQuads();
					t.normal(1.0F, 0.0F, 0.0F);
					renderer.renderSouthFace(block, 0.0, 0.0, 0.0, block.getTexture(5, meta));
					t.draw();
				}
				GL11.glTranslatef(0.5F, 0.5F, 0.5F);
				block.minX = minX;
				block.minY = minY;
				block.minZ = minZ;
				block.maxX = maxX;
				block.maxY = maxY;
				block.maxZ = maxZ;
			}
		};
	}
}
