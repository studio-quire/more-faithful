package org.studioquire.oldfaithful.core.api.client;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import org.studioquire.oldfaithful.core.api.BlockShapes;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;
import org.studioquire.oldfaithful.core.impl.content.client.ClientAtlases;
import org.studioquire.oldfaithful.core.impl.content.client.ClientShapes;
import org.studioquire.oldfaithful.core.mixin.content.client.BlockEntityRenderDispatcherAccessor;
import org.studioquire.oldfaithful.core.mixin.content.client.EntityRenderDispatcherAccessor;

/**
 * How a mod's things are drawn, from {@code initClient}. A renderer is vanilla's own kind, subclassed as vanilla's are. Textures go by path: {@code
 * bindTexture("/assets/<modid>/textures/mob/thing.png")}, so a texture pack can replace them.
 */
public final class Renderers {
	private Renderers() {
	}

	/** For this class and its subclasses. */
	@SuppressWarnings("unchecked")
	public static void entity(Class<? extends Entity> type, EntityRenderer renderer) {
		((EntityRenderDispatcherAccessor) EntityRenderDispatcher.INSTANCE).faithful$renderers().put(type, renderer);
		renderer.setDispatcher(EntityRenderDispatcher.INSTANCE);
	}

	/** Drawn every frame as vanilla's chests and signs are, beside the block's own cells. For this class and its subclasses. */
	@SuppressWarnings("unchecked")
	public static void blockEntity(Class<? extends BlockEntity> type, BlockEntityRenderer renderer) {
		((BlockEntityRenderDispatcherAccessor) BlockEntityRenderDispatcher.INSTANCE).faithful$renderers().put(type, renderer);
		renderer.setDispatcher(BlockEntityRenderDispatcher.INSTANCE);
	}

	/** What a render type from {@link BlockShapes#type} looks like. */
	public static void blockShape(String name, BlockShape shape) {
		ClientShapes.add(BlockShapes.type(name), shape);
	}

	/**
	 * What's drawn on this side of a block (0 down, 1 up, then 2-5), as it stands on its sheet in the texture pack in use: ARGB, row by row, a
	 * square 16 across for a mod's tile and at the pack's size for vanilla's, so take the side from the length. Three things it isn't. It's untinted: grass and
	 * leaves are grey here, and {@code block.getColorMultiplier(world, x, y, z)} is what the world multiplies them by. An animated tile reads as its sheet's picture, or
	 * its first frame. A tile with no PNG reads as the game draws it, a checker. It's a fresh array each call, and reading a mod's tile touches no GL, but a
	 * pack's sheet is worth caching against: {@code minecraft.texturePacks.selected} changes when the pack does.
	 */
	public static int[] tilePixels(Block block, int side, int meta) {
		return ClientAtlases.pixels(ContentRegistry.BLOCK_ATLAS[block.id], block.getTexture(side, meta) & 255, "/terrain.png");
	}

	/** The same for a stack's icon: a block item's is its block's. */
	public static int[] tilePixels(ItemStack stack) {
		int index = stack.getTextureId();
		return ClientAtlases.pixels(index >> 8, index & 255, stack.getItem() instanceof BlockItem ? "/terrain.png" : "/gui/items.png");
	}

	/** {@code 0xRRGGBB}: the average of ARGB pixels, each counted by its alpha; -1 if every one is clear. */
	public static int averageColor(int[] pixels) {
		long r = 0, g = 0, b = 0, weight = 0;
		for (int pixel : pixels) {
			int alpha = pixel >>> 24;
			r += (pixel >> 16 & 255) * alpha;
			g += (pixel >> 8 & 255) * alpha;
			b += (pixel & 255) * alpha;
			weight += alpha;
		}
		return weight == 0 ? -1 : (int) (r / weight) << 16 | (int) (g / weight) << 8 | (int) (b / weight);
	}
}
