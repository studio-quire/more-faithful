package org.studioquire.faithful.harness.mixin;

import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * A server running faster ticks ({@link MinecraftServerTickRateMixin}) says so in the login packet's name, which the
 * server's side leaves empty and no client reads: {@code tickms=N}. The harness's hidden client runs its own ticks at
 * that pace there (the selftest's ClientNetworkHandlerTickRateMixin), and at the game's own anywhere it isn't said.
 */
@Mixin(ServerLoginNetworkHandler.class)
abstract class ServerLoginTickRateMixin {
	private static final int TICK_MS = Integer.getInteger("oldfaithful.test.tickMs", 50);

	@ModifyArg(method = "accept", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/login/LoginHelloPacket;<init>(Ljava/lang/String;IJB)V"), index = 0)
	private String harness$sayTickMs(String name) {
		return TICK_MS == 50 ? name : "tickms=" + TICK_MS;
	}
}
