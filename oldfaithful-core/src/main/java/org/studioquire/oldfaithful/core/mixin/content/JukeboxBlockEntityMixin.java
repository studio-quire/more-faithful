package org.studioquire.oldfaithful.core.mixin.content;

import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;

/** A mod's record is held by number: its stand-in on disk, the name beside it. */
@Mixin(JukeboxBlockEntity.class)
abstract class JukeboxBlockEntityMixin {
	@Shadow
	public int recordId;

	@Inject(method = "writeNbt", at = @At("TAIL"))
	private void faithful$write(NbtCompound nbt, CallbackInfo ci) {
		String name = ContentRegistry.itemName(recordId);
		if (name == null) return;
		nbt.putInt("Record", ContentRegistry.itemStandInId(recordId));
		nbt.putString("FaithfulRecord", name);
	}

	@Inject(method = "readNbt", at = @At("TAIL"))
	private void faithful$read(NbtCompound nbt, CallbackInfo ci) {
		if (!nbt.contains("FaithfulRecord")) return;
		int id = ContentRegistry.itemId(nbt.getString("FaithfulRecord"));
		if (id >= 0) recordId = id;
	}
}
