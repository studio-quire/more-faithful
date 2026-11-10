package org.studioquire.faithful.warycreepers;

import net.minecraft.util.math.Vec3d;
import org.studioquire.oldfaithful.core.api.Creepers;
import org.studioquire.oldfaithful.core.api.FaithfulMod;
import org.studioquire.oldfaithful.core.api.Mod;

/**
 * Creepers keep away from a jack-o'-lantern whose lit face they can see ({@link Lanterns}). How a frightened creeper runs, comes back and swells is the
 * core's ({@code Creepers}), shared with any other mod that frightens creepers; this says only what frightens it.
 */
public final class WaryCreepers implements FaithfulMod {
	@Override
	public void init(Mod mod) {
		Creepers.onFright(fright -> {
			Vec3d lantern = Lanterns.nearest(fright.creeper);
			if (lantern != null) fright.at(lantern.x, lantern.y, lantern.z);
		});
	}
}
