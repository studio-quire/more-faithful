package org.studioquire.oldfaithful.core.mixin.server;

import java.net.ServerSocket;
import java.util.ArrayList;

import net.minecraft.server.network.ConnectionListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** For closing the listener of a server the core turned down (`Refusal`). */
@Mixin(ConnectionListener.class)
public interface ConnectionListenerAccessor {
	@Accessor("socket")
	ServerSocket faithful$socket();

	@Accessor("thread")
	Thread faithful$thread();

	@Accessor("pendingConnections")
	ArrayList<?> faithful$pending();
}
