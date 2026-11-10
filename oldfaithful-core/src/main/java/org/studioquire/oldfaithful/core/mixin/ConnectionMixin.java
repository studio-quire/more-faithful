package org.studioquire.oldfaithful.core.mixin;

import java.io.DataOutputStream;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.oldfaithful.core.impl.Peer;
import org.studioquire.oldfaithful.core.impl.PeerHolder;

/** What each connection's other end has declared. */
@Mixin(Connection.class)
abstract class ConnectionMixin implements PeerHolder {
	@Shadow
	private DataOutputStream outputStream;

	@Unique
	private final Peer faithful$peer = new Peer();

	@Override
	public Peer faithful$peer() {
		return faithful$peer;
	}

	/**
	 * Beta closes the input stream first, which closes the socket, so closing the output with anything still buffered throws, and the line that forgets the stream never runs. The writer
	 * thread then wakes, flushes it, and prints "Socket closed": on nearly every disconnect, as a client sends every tick. Forgotten first, it's left alone.
	 */
	@WrapOperation(method = "disconnect(Ljava/lang/String;[Ljava/lang/Object;)V", at = @At(value = "INVOKE", target = "Ljava/io/DataOutputStream;close()V"))
	private void faithful$forgetOutput(DataOutputStream out, Operation<Void> close) {
		outputStream = null;
		close.call(out);
	}
}
