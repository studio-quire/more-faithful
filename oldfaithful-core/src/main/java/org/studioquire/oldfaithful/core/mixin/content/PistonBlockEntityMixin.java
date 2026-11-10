package org.studioquire.oldfaithful.core.mixin.content;

import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;
import org.studioquire.oldfaithful.core.impl.content.Overlay;

/** A block being pushed is held as a number: the stand-in on disk, the name beside it. */
@Mixin(PistonBlockEntity.class)
abstract class PistonBlockEntityMixin {
	@Shadow
	private int pushedBlockId;

	@Inject(method = "writeNbt", at = @At("TAIL"))
	private void faithful$write(NbtCompound nbt, CallbackInfo ci) {
		if (!Overlay.modded(pushedBlockId)) return;
		nbt.putInt("blockId", Overlay.standId(pushedBlockId));
		nbt.putString("FaithfulBlock", ContentRegistry.blockName(pushedBlockId));
	}

	@Inject(method = "readNbt", at = @At("TAIL"))
	private void faithful$read(NbtCompound nbt, CallbackInfo ci) {
		if (!nbt.contains("FaithfulBlock")) return;
		int id = ContentRegistry.blockId(nbt.getString("FaithfulBlock"));
		if (id >= 0) pushedBlockId = id;
	}
}
