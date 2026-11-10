package org.studioquire.faithful.stagehand.mixin.server;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Whether the client has answered its last move: see {@code Side.answered}. Nothing public says. */
@Mixin(ServerPlayNetworkHandler.class)
public interface NetworkHandlerAccessor {
	@Accessor("teleported")
	boolean stagehand$teleported();
}
