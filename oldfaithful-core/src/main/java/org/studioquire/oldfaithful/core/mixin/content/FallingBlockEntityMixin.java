package org.studioquire.oldfaithful.core.mixin.content;

import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;
import org.studioquire.oldfaithful.core.impl.content.Overlay;

/** {@code Tile} is a byte: the stand-in there, the name beside it. */
@Mixin(FallingBlockEntity.class)
abstract class FallingBlockEntityMixin {
	@Shadow
	public int blockId;

	@Inject(method = "writeNbt", at = @At("TAIL"))
	private void faithful$write(NbtCompound nbt, CallbackInfo ci) {
		if (!Overlay.modded(blockId)) return;
		nbt.putByte("Tile", (byte) Overlay.standId(blockId));
		nbt.putString("FaithfulTile", ContentRegistry.blockName(blockId));
	}

	@Inject(method = "readNbt", at = @At("TAIL"))
	private void faithful$read(NbtCompound nbt, CallbackInfo ci) {
		if (!nbt.contains("FaithfulTile")) return;
		int id = ContentRegistry.blockId(nbt.getString("FaithfulTile"));
		if (id >= 0) blockId = id;
	}
}
