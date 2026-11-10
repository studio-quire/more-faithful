package org.studioquire.oldfaithful.core.mixin.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.Command;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.api.Commands;
import org.studioquire.oldfaithful.core.api.Days;
import org.studioquire.oldfaithful.core.impl.CommandTable;

/** Mods' commands, ahead of the server's. Only the console and ops get this far. */
@Mixin(ServerCommandHandler.class)
abstract class ServerCommandHandlerMixin {
	@Inject(method = "executeCommand", at = @At("HEAD"), cancellable = true)
	private void faithful$modCommands(Command command, CallbackInfo ci) {
		CommandOutput output = command.output;
		PlayerEntity player = output instanceof ServerPlayNetworkHandler ? ((ServerPlayNetworkHandlerAccessor) output).faithful$player() : null;
		Commands.Reply reply = new Commands.Reply() {
			@Override
			public void say(String line) {
				output.sendMessage(line);
			}

			@Override
			public PlayerEntity player() {
				return player;
			}
		};
		if (CommandTable.run(command.commandAndArgs, reply)) ci.cancel();
	}

	/** {@code time add}: on from the hour it is, as {@code time set} goes to an hour. */
	@WrapOperation(method = "executeCommand", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/ServerWorld;getTime()J"))
	private long faithful$timeAdded(ServerWorld world, Operation<Long> getTime) {
		return Days.time(world);
	}

	@Inject(method = "displayHelp", at = @At("TAIL"))
	private void faithful$help(CommandOutput output, CallbackInfo ci) {
		CommandTable.help(output::sendMessage);
	}
}
