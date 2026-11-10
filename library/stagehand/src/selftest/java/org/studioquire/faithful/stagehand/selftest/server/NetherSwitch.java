package org.studioquire.faithful.stagehand.selftest.server;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;

/** The selftest server has allow-nether=false, which is itself worth checking; then the Nether is taken for a moment. */
@Environment(EnvType.SERVER)
public final class NetherSwitch {
	private NetherSwitch() {
	}

	public static void set(PlayerEntity player, boolean allowed) {
		((ServerPlayerEntity) player).server.properties.setProperty("allow-nether", allowed);
	}
}
