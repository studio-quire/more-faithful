package org.studioquire.oldfaithful.core.impl;

import net.minecraft.block.Block;
import org.studioquire.oldfaithful.core.mixin.FireBlockInvoker;

/** Fire's two tables, through its own method, so they can be any length the content layer makes them. */
public final class FlammableBlocks {
	private FlammableBlocks() {
	}

	public static void set(int block, int burn, int spread) {
		((FireBlockInvoker) Block.FIRE).faithful$register(block, burn, spread);
	}
}
