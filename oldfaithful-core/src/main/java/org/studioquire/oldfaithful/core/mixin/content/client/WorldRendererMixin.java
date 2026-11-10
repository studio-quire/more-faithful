package org.studioquire.oldfaithful.core.mixin.content.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.Block;
import net.minecraft.block.LeavesBlock;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.impl.content.Wire;

/**
 * A block break's event read back whole: the sound and the particles. The registry gives no mod block an id with a low byte of 0, which reads as nothing here. And fancy graphics, which
 * vanilla tells its own leaves alone.
 */
@Mixin(WorldRenderer.class)
abstract class WorldRendererMixin {
	@Unique
	private int faithful$eventData;

	@WrapOperation(method = "reload", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/LeavesBlock;setFancyGraphics(Z)V"))
	private void faithful$allLeaves(LeavesBlock leaves, boolean fancy, Operation<Void> set) {
		for (Block block : Block.BLOCKS) {
			if (block instanceof LeavesBlock) set.call(block, fancy);
		}
	}

	@Inject(method = "worldEvent", at = @At("HEAD"))
	private void faithful$note(PlayerEntity player, int event, int x, int y, int z, int data, CallbackInfo ci) {
		faithful$eventData = data;
	}

	@Redirect(method = "worldEvent", at = @At(value = "FIELD", target = "Lnet/minecraft/block/Block;BLOCKS:[Lnet/minecraft/block/Block;", args = "array=get"))
	private Block faithful$sound(Block[] blocks, int id) {
		return blocks[Wire.breakEventId(faithful$eventData)];
	}

	/** The record line credits C418, who didn't make a mod's music: it says the track's own name. Only a mod's name has a dot. */
	@ModifyArg(method = "playStreaming", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;setRecordPlayingOverlay(Ljava/lang/String;)V"))
	private String faithful$modRecord(String line) {
		return line.indexOf('.') < 0 ? line : line.substring(line.lastIndexOf('.') + 1);
	}

	@ModifyArg(method = "worldEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleManager;addBlockBreakParticles(IIIII)V"), index = 3)
	private int faithful$particles(int id) {
		return Wire.breakEventId(faithful$eventData);
	}
}
