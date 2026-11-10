package org.studioquire.faithful.staticbad;

import net.minecraft.block.Block;
import net.minecraft.entity.ItemEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** On a method an edition's mod hooks too: {@code Block.onPlaced}, here twice over with the {@code @Redirect}s on one site. */
@Mixin(Block.class)
abstract class RedirectingMixin {
	@Redirect(method = "dropStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
	private boolean first(World world, net.minecraft.entity.Entity entity) {
		return true;
	}

	@Redirect(method = "dropStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
	private boolean second(World world, net.minecraft.entity.Entity entity) {
		return false;
	}

	@Redirect(method = "onPlaced(Lnet/minecraft/world/World;IIILnet/minecraft/entity/LivingEntity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getTickRate()I"))
	private int third(Block block) {
		return 0;
	}
}
