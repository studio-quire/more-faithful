package org.studioquire.faithful.blueflowers.mixin;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.faithful.blueflowers.BlueFlowers;

/**
 * Lapis lazuli that's held, as it's read from a save: a stack of the block is as many blue wool, and one of the ore as many stone, as the blocks standing in a chunk become. Every stack a world
 * keeps is read here and nowhere else (a player's, a chest's, a furnace's, a minecart's, one lying on the ground), so there's nothing to go looking through. The core has no word for a stack
 * being read, and a saved stack isn't a thing a hook could hand out one at a time. A stack made while the game runs (a server's {@code /give}) isn't caught until it's next read, and a block
 * placed from one goes when its chunk next loads. A mod's item that only stands in a save as one of these is named beside its number, and the core puts it back whichever of us reads first.
 */
@Mixin(ItemStack.class)
abstract class ItemStackMixin {
	@Shadow
	public int itemId;
	@Shadow
	private int damage;

	@Inject(method = "readNbt", at = @At("TAIL"))
	private void blueflowers$lapisGoes(NbtCompound nbt, CallbackInfo ci) {
		if (itemId == Block.LAPIS_BLOCK.id) {
			itemId = Block.WOOL.id;
			damage = BlueFlowers.BLUE_WOOL;
		} else if (itemId == Block.LAPIS_ORE.id) {
			itemId = Block.STONE.id;
			damage = 0;
		}
	}
}
