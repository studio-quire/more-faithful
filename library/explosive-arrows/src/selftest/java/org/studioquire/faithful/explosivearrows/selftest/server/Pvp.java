package org.studioquire.faithful.explosivearrows.selftest.server;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;

/** The server's pvp setting, taken for a moment: what it was. */
@Environment(EnvType.SERVER)
public final class Pvp {
	private Pvp() {
	}

	public static boolean set(PlayerEntity player, boolean on) {
		boolean was = ((ServerPlayerEntity) player).server.pvpEnabled;
		((ServerPlayerEntity) player).server.pvpEnabled = on;
		return was;
	}
}
