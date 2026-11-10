package org.studioquire.faithful.staticbad;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

/** Not a mistake: Fabric strips a marked method from the other side, so the static stage must let this class be. Never run. */
public final class MarkedMethods {
	@Environment(EnvType.CLIENT)
	public static Object client() {
		return Minecraft.class;
	}
}
