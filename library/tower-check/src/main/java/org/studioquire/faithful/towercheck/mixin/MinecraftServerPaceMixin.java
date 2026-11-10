package org.studioquire.faithful.towercheck.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.faithful.towercheck.server.Pace;

/** The clock the server's loop counts its ticks by, and nothing else's: {@link Pace}. */
@Mixin(MinecraftServer.class)
abstract class MinecraftServerPaceMixin {
	@ModifyExpressionValue(method = "run", at = @At(value = "INVOKE", target = "Ljava/lang/System;currentTimeMillis()J"))
	private long towercheck$clock(long now) {
		return Pace.clock(now);
	}
}
