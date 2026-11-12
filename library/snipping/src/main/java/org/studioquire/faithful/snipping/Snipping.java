package org.studioquire.faithful.snipping;

import net.minecraft.block.Block;
import org.studioquire.oldfaithful.core.api.FaithfulMod;
import org.studioquire.oldfaithful.core.api.Growth;
import org.studioquire.oldfaithful.core.api.Mod;

/** Shears that snip a plant so it stops growing ({@link Snips}). */
public final class Snipping implements FaithfulMod {
	@Override
	public void init(Mod mod) {
		Snips.declare(mod);
		// Bonemeal grows a sapling or wheat at once: on a snipped one it does nothing, and isn't used.
		Growth.onBonemeal(e -> {
			if ((e.block == Block.SAPLING.id || e.block == Block.WHEAT.id) && Snips.snipped(e.world, e.x, e.y, e.z)) e.cancel();
		});
	}
}
