package org.studioquire.faithful.harness.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Beta's network writer sends what's queued, sleeps 100 ms, then flushes: every packet waits about two ticks. A server
 * running faster ticks ({@link MinecraftServerTickRateMixin}) shrinks the sleep with them, so a round trip takes as many
 * ticks as it did. The client's side is the selftest mod's.
 */
@Mixin(targets = "net.minecraft.network.Connection$1")
abstract class ConnectionWriterTickRateMixin {
	private static final long TICK_MS = Integer.getInteger("oldfaithful.test.tickMs", 50);

	@ModifyConstant(method = "run", constant = @Constant(longValue = 100L))
	private long harness$sleepMs(long ms) {
		return Math.max(1L, ms * TICK_MS / 50);
	}
}
