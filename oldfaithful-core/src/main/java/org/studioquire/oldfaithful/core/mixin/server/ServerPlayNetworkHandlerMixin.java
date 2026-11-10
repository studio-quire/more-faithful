package org.studioquire.oldfaithful.core.mixin.server;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.packet.play.ChatMessagePacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.api.Commands;
import org.studioquire.oldfaithful.core.impl.CommandTable;

/** Commands anyone may use, ahead of {@code /me} and the rest, and of the check that only ops go further. */
@Mixin(ServerPlayNetworkHandler.class)
abstract class ServerPlayNetworkHandlerMixin {
	@Shadow
	private ServerPlayerEntity player;

	@Inject(method = "handleCommand", at = @At("HEAD"), cancellable = true)
	private void faithful$everyone(String message, CallbackInfo ci) {
		ServerPlayNetworkHandler self = (ServerPlayNetworkHandler) (Object) this;
		ServerPlayerEntity who = player;
		Commands.Reply reply = new Commands.Reply() {
			@Override
			public void say(String line) {
				self.sendPacket(new ChatMessagePacket(line));
			}

			@Override
			public PlayerEntity player() {
				return who;
			}
		};
		if (CommandTable.runForAnyone(message.substring(1), reply)) ci.cancel();
	}
}
