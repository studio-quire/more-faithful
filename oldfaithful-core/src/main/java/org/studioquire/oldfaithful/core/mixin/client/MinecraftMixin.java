package org.studioquire.oldfaithful.core.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.nio.file.Path;
import java.util.List;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.InteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.api.Lifecycle;
import org.studioquire.oldfaithful.core.api.Players;
import org.studioquire.oldfaithful.core.api.Ticks;
import org.studioquire.oldfaithful.core.api.client.Client;
import org.studioquire.oldfaithful.core.api.client.Input;
import org.studioquire.oldfaithful.core.impl.Core;
import org.studioquire.oldfaithful.core.impl.Rules;
import org.studioquire.oldfaithful.core.impl.SeenMods;
import org.studioquire.oldfaithful.core.impl.client.MissingContentScreen;
import org.studioquire.oldfaithful.core.impl.client.ClientNet;

/**
 * The client's start, a world's opening and closing, and the new player object a respawn makes. Where a world runs here, single
 * player is what a server is elsewhere: {@code faithful$running} is its "server", from a save's opening to its closing.
 */
@Mixin(Minecraft.class)
abstract class MinecraftMixin {
	@Shadow
	public ClientPlayerEntity player;
	@Shadow
	public World world;
	@Shadow
	public boolean paused;
	@Shadow
	public Screen currentScreen;
	@Unique
	private boolean faithful$running;
	@Unique
	private PlayerEntity faithful$dead;
	@Unique
	private World faithful$leaving;

	@Inject(method = "init", at = @At("HEAD"))
	private void faithful$start(CallbackInfo ci) {
		ClientNet.minecraft = (Minecraft) (Object) this;
		Core.start();
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void faithful$tick(CallbackInfo ci) {
		ClientNet.tick();
		if (faithful$running && !paused) Ticks.server();
		Ticks.client();
	}

	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Keyboard;next()Z", remap = false))
	private boolean faithful$key(Operation<Boolean> next) {
		return Input.nextKey(() -> next.call());
	}

	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;next()Z", remap = false))
	private boolean faithful$mouse(Operation<Boolean> next) {
		return Input.nextMouse(() -> next.call());
	}

	/** Before anything of the world loads: a world missing a content mod says so. Going on calls here again, with nothing left to ask. */
	@Inject(method = "startGame", at = @At("HEAD"), cancellable = true)
	private void faithful$missingContent(String worldName, String name, long seed, CallbackInfo ci) {
		Path dir = FabricLoader.getInstance().getGameDir().resolve("saves").resolve(worldName);
		List<String> gone = SeenMods.missing(dir);
		if (gone.isEmpty()) return;
		Minecraft self = (Minecraft) (Object) this;
		self.setScreen(new MissingContentScreen(self.currentScreen, dir, gone, () -> {
			self.startGame(worldName, name, seed);
			self.setScreen(null);
		}));
		ci.cancel();
	}

	/** The world list clears the screen after asking for a world, which with no world is the title screen: the warning stays until it's answered. */
	@Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
	private void faithful$keepWarning(Screen screen, CallbackInfo ci) {
		if (screen == null && world == null && currentScreen instanceof MissingContentScreen) ci.cancel();
	}

	/** Just after beta closes any world it had. */
	@Inject(method = "startGame", at = @At(value = "INVOKE", target = "Ljava/lang/System;gc()V"))
	private void faithful$openWorld(String worldName, String name, long seed, CallbackInfo ci) {
		Rules.open(FabricLoader.getInstance().getGameDir().resolve("saves").resolve(worldName));
	}

	/** Leaving a save, before beta saves it. */
	@Inject(method = "setWorld(Lnet/minecraft/world/World;Ljava/lang/String;Lnet/minecraft/entity/player/PlayerEntity;)V", at = @At("HEAD"))
	private void faithful$closing(World next, String message, PlayerEntity keptPlayer, CallbackInfo ci) {
		faithful$leaving = world;
		if (faithful$running && (next == null || next.isRemote)) {
			if (player != null) Players.leaving(player);
			Lifecycle.serverStopping();
			faithful$running = false;
		}
	}

	/**
	 * Closing the window: beta's {@code setWorld} gives up at its first progress line, before it saves, and the world is left as it
	 * was (players use that to undo a loss). So this is its unload.
	 */
	@Inject(method = "stop", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/GlAllocationUtils;clear()V"))
	private void faithful$closed(CallbackInfo ci) {
		if (world != null && !world.isRemote) Lifecycle.worldUnloaded(world);
	}

	/** Beta's saved it by here. */
	@Inject(method = "setWorld(Lnet/minecraft/world/World;Ljava/lang/String;Lnet/minecraft/entity/player/PlayerEntity;)V", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/client/Minecraft;world:Lnet/minecraft/world/World;"))
	private void faithful$unloading(World next, String message, PlayerEntity keptPlayer, CallbackInfo ci) {
		if (world != null && !world.isRemote) Lifecycle.worldUnloaded(world);
	}

	@Inject(method = "setWorld(Lnet/minecraft/world/World;Ljava/lang/String;Lnet/minecraft/entity/player/PlayerEntity;)V", at = @At("TAIL"))
	private void faithful$setWorld(World next, String message, PlayerEntity keptPlayer, CallbackInfo ci) {
		if (next == null) Rules.close();
		if (next != null && !next.isRemote) {
			Lifecycle.worldLoaded(next);
			if (!faithful$running) {
				faithful$running = true;
				Lifecycle.serverStarted();
				Players.joined(player);
			}
		}
		if (faithful$leaving != next) Client.worldChanged(faithful$leaving, next);
		faithful$leaving = null;
	}

	@ModifyExpressionValue(method = "respawnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/InteractionManager;createPlayer(Lnet/minecraft/world/World;)Lnet/minecraft/entity/player/PlayerEntity;"))
	private PlayerEntity faithful$carryOver(PlayerEntity reborn) {
		if (player != null && !world.isRemote) {
			Players.carryOver(player, reborn);
			faithful$dead = player;
		}
		return reborn;
	}

	@Inject(method = "respawnPlayer", at = @At("TAIL"))
	private void faithful$respawned(boolean worldSpawn, int dimension, CallbackInfo ci) {
		if (faithful$dead != null) Players.respawned(faithful$dead, player);
		faithful$dead = null;
	}

	/** A death in the Nether comes through here too, on its way to the respawn: that's the respawn's to tell. */
	@Inject(method = "changeDimension", at = @At("TAIL"))
	private void faithful$dimension(CallbackInfo ci) {
		if (!world.isRemote && player.health > 0) Players.dimensionChanged(player, player.dimensionId == 0 ? -1 : 0, player.dimensionId);
	}
}
