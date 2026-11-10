package org.studioquire.oldfaithful.core.impl.content.client;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.world.BlockView;
import org.studioquire.oldfaithful.core.api.BlockShapes;
import org.studioquire.oldfaithful.core.api.client.BlockShape;

/** Mods' render types. A type with no shape draws nothing, as vanilla does for a number it doesn't know. */
public final class ClientShapes {
	private static final Map<Integer, BlockShape> SHAPES = new HashMap<>();

	private ClientShapes() {
	}

	public static void add(int type, BlockShape shape) {
		if (SHAPES.putIfAbsent(type, shape) != null) throw new IllegalStateException("The render type " + type + " has two shapes");
	}

	public static boolean ours(int type) {
		return type >= BlockShapes.FIRST_TYPE;
	}

	public static boolean render(BlockRenderManager renderer, BlockView world, Block block, int type, int x, int y, int z) {
		BlockShape shape = SHAPES.get(type);
		return shape != null && shape.render(renderer, world, block, x, y, z);
	}

	public static void renderItem(BlockRenderManager renderer, Block block, int type, int meta, float brightness) {
		BlockShape shape = SHAPES.get(type);
		if (shape != null) shape.renderItem(renderer, block, meta, brightness);
	}

	public static boolean inventoryIn3d(int type) {
		BlockShape shape = SHAPES.get(type);
		return shape != null && shape.inventoryIn3d();
	}
}
