package org.studioquire.oldfaithful.core.mixin.server;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.world.ServerWorld;
import net.minecraft.world.storage.WorldStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.api.Lifecycle;
import org.studioquire.oldfaithful.core.api.Players;
import org.studioquire.oldfaithful.core.api.Ticks;
import org.studioquire.oldfaithful.core.impl.Core;
import org.studioquire.oldfaithful.core.impl.Rules;
import org.studioquire.oldfaithful.core.impl.server.Refusal;
import org.studioquire.oldfaithful.core.impl.server.ServerNet;

/** The server's start: the mods, then the world's rules, before the world loads. Its ticks, and its end. */
@Mixin(MinecraftServer.class)
abstract class MinecraftServerMixin {
	@Shadow
	public ServerWorld[] worlds;
	@Shadow
	public PlayerManager playerManager;
	/** The core turned this start down (a world missing a content mod, a library mod that failed to start), before any world was made. */
	@Unique
	private boolean faithful$refused;
	/** Beta's reader of the console, which a refused server waits on at a terminal (`Refusal`). */
	@Unique
	private Thread faithful$console;

	@WrapOperation(method = "init", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;start()V"))
	private void faithful$console(Thread console, Operation<Void> start) {
		faithful$console = console;
		start.call(console);
	}

	/**
	 * Beta's {@code run} logs what a start throws and then idles with its port open until someone types stop: players joining wait at
	 * Logging in until they time out, and a host's panel sees a server that's up. Stopped first, the same throw is logged the same way and
	 * the process goes straight on to beta's own exit.
	 */
	@Inject(method = "loadWorld", at = @At("HEAD"))
	private void faithful$start(WorldStorageSource storageSource, String worldDir, long seed, CallbackInfo ci) {
		ServerNet.server = (MinecraftServer) (Object) this;
		try {
			Core.start();
			Rules.openOnDedicated(Paths.get(worldDir));
		} catch (RuntimeException | Error e) {
			faithful$refused = true;
			((MinecraftServer) (Object) this).stop();
			throw e;
		}
	}

	@Inject(method = "loadWorld", at = @At("TAIL"))
	private void faithful$started(WorldStorageSource storageSource, String worldDir, long seed, CallbackInfo ci) {
		for (ServerWorld world : worlds) Lifecycle.worldLoaded(world);
		Lifecycle.serverStarted();
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void faithful$tick(CallbackInfo ci) {
		Ticks.server();
	}

	/**
	 * A stopping server saves its players without disconnecting them. A start the core turned down made no world and started nothing, so
	 * there's nothing to save or tell (beta's own shutdown would only throw at the worlds it never made).
	 */
	@Inject(method = "shutdown", at = @At("HEAD"), cancellable = true)
	@SuppressWarnings("unchecked")
	private void faithful$stopping(CallbackInfo ci) {
		if (faithful$refused) {
			MinecraftServer.LOGGER.info("Stopping server: it couldn't start, for the reason above");
			Refusal.holdForConsole(((MinecraftServer) (Object) this).connections, faithful$console);
			ci.cancel();
			return;
		}
		if (playerManager != null) {
			for (ServerPlayerEntity player : new ArrayList<>((List<ServerPlayerEntity>) playerManager.players)) Players.leaving(player);
		}
		Lifecycle.serverStopping();
	}

	@Inject(method = "shutdown", at = @At("TAIL"))
	private void faithful$stopped(CallbackInfo ci) {
		for (ServerWorld world : worlds) {
			if (world != null) Lifecycle.worldUnloaded(world);
		}
	}
}
