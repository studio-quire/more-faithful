package org.studioquire.oldfaithful.core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.studioquire.oldfaithful.core.api.Blocks;

/**
 * Every item used on a block, in single player and on a server, is this one call, and each item places by its own code. So what was
 * placed is read from what changed: the cell beside the one clicked, where a block goes, or the clicked cell, for snow and slabs.
 */
@Mixin(ItemStack.class)
abstract class ItemStackMixin {
	@Unique
	private static final int[][] SIDES = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};

	@WrapOperation(method = "useOnBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/Item;useOnBlock(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/World;IIII)Z"))
	private boolean faithful$placed(Item item, ItemStack stack, PlayerEntity player, World world, int x, int y, int z, int side, Operation<Boolean> use) {
		if (!Blocks.hasPlacedListeners() || world.isRemote || side < 0 || side > 5) return use.call(item, stack, player, world, x, y, z, side);
		int nx = x + SIDES[side][0], ny = y + SIDES[side][1], nz = z + SIDES[side][2];
		int clicked = world.getBlockId(x, y, z), beside = world.getBlockId(nx, ny, nz);
		int clickedMeta = world.getBlockMeta(x, y, z), besideMeta = world.getBlockMeta(nx, ny, nz);
		boolean used = use.call(item, stack, player, world, x, y, z, side);
		if (used) {
			int now = world.getBlockId(nx, ny, nz);
			if (now != beside && now != 0) Blocks.placed(world, player, stack, nx, ny, nz, beside, besideMeta);
			now = world.getBlockId(x, y, z);
			if (now != clicked && now != 0) Blocks.placed(world, player, stack, x, y, z, clicked, clickedMeta);
		}
		return used;
	}
}
