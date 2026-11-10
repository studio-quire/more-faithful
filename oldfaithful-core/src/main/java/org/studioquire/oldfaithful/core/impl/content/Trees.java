package org.studioquire.oldfaithful.core.impl.content;

import net.minecraft.block.Block;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.LogBlock;

/** What vanilla's decay should read a block as: it compares ids with its own leaves and log. */
public final class Trees {
	private Trees() {
	}

	public static int kind(int id) {
		Block block = Block.BLOCKS[id];
		if (block instanceof LeavesBlock) return Block.LEAVES.id;
		return block instanceof LogBlock ? Block.LOG.id : id;
	}
}
