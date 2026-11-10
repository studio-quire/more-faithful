package org.studioquire.oldfaithful.core.mixin.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
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
import org.studioquire.oldfaithful.core.api.Days;
import org.studioquire.oldfaithful.core.api.Lifecycle;
import org.studioquire.oldfaithful.core.api.Players;
import org.studioquire.oldfaithful.core.api.Ticks;
import org.studioquire.oldfaithful.core.impl.Core;
import org.studioquire.oldfaithful.core.impl.Rules;
import org.studioquire.oldfaithful.core.impl.server.ServerNet;

/** The server's start: the mods, then the world's rules, before the world loads. Its ticks, and its end. */
@Mixin(MinecraftServer.class)
abstract class MinecraftServerMixin {
	@Shadow
	public ServerWorld[] worlds;
	@Shadow
	public PlayerManager playerManager;

	@Inject(method = "loadWorld", at = @At("HEAD"))
	private void faithful$start(WorldStorageSource storageSource, String worldDir, long seed, CallbackInfo ci) {
		ServerNet.server = (MinecraftServer) (Object) this;
		Core.start();
		Rules.openOnDedicated(Paths.get(worldDir));
	}

	@Inject(method = "loadWorld", at = @At("TAIL"))
	private void faithful$started(WorldStorageSource storageSource, String worldDir, long seed, CallbackInfo ci) {
		for (ServerWorld world : worlds) Lifecycle.worldLoaded(world);
		Lifecycle.serverStarted();
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void faithful$tick(CallbackInfo ci) {
		Ticks.server();
		if (Days.length() != Days.NORMAL) sendTime();
	}

	/**
	 * Every tick, not beta's once a second, in a day of another length. A client that doesn't know the length runs its sun at beta's pace, and pulled back only each second it crosses each step of
	 * darkness several times over, rebuilding its chunks every time (measured: ten changes for two steps at a triple day).
	 */
	@Unique
	private void sendTime() {
		for (ServerWorld world : worlds) playerManager.sendToDimension(new WorldTimeUpdateS2CPacket(Days.time(world)), world.dimension.id);
	}

	/** Clients go by the sun, not the tick count. */
	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/ServerWorld;getTime()J"))
	private long faithful$timeSent(ServerWorld world, Operation<Long> getTime) {
		return Days.time(world);
	}

	/** A stopping server saves its players without disconnecting them. */
	@Inject(method = "shutdown", at = @At("HEAD"))
	@SuppressWarnings("unchecked")
	private void faithful$stopping(CallbackInfo ci) {
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
