package org.studioquire.faithful.harness.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Faster game time for tests that wait on it ({@code -Ptest.speed}, docs/building.md): with
 * {@code -Doldfaithful.test.tickMs=N} the server's loop runs a tick every N ms instead of 50. Clients are told in the
 * login packet ({@link ServerLoginTickRateMixin}) and follow.
 */
@Mixin(MinecraftServer.class)
abstract class MinecraftServerTickRateMixin {
	private static final long TICK_MS = Integer.getInteger("oldfaithful.test.tickMs", 50);

	@ModifyConstant(method = "run", constant = @Constant(longValue = 50L))
	private long harness$tickMs(long ms) {
		return TICK_MS;
	}
}
