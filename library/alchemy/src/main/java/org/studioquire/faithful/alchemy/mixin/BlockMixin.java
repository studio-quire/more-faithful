package org.studioquire.faithful.alchemy.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.faithful.alchemy.Alchemy;

/**
 * Haste's mining. A mixin because the core has no hook for the pace of breaking a block. This is the one number for it, with the right tool or without: a client's progress each tick, and a
 * server's check when the client says it's done, which waits for its own count unless the client is at least 70% of the way by the server's reckoning. Both sides ask it, a client by what it heard.
 */
@Mixin(Block.class)
abstract class BlockMixin {
	@ModifyReturnValue(method = "getHardness(Lnet/minecraft/entity/player/PlayerEntity;)F", at = @At("RETURN"))
	private float alchemy$hasteMines(float progress, PlayerEntity player) {
		return progress * Alchemy.haste(player);
	}
}
