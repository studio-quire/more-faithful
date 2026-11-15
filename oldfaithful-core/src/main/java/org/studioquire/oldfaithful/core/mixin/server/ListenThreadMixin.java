package org.studioquire.oldfaithful.core.mixin.server;

import java.io.IOException;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.oldfaithful.core.impl.server.Refusal;

/** The listener's thread: its accept fails when the core closes the socket of a server it turned down, which is no fault to print. */
@Mixin(targets = "net.minecraft.server.network.ConnectionListener$1")
abstract class ListenThreadMixin {
	@WrapOperation(method = "run", at = @At(value = "INVOKE", target = "Ljava/io/IOException;printStackTrace()V"))
	private void faithful$quietOnceClosed(IOException e, Operation<Void> print) {
		if (!Refusal.listenerClosed) print.call(e);
	}
}
