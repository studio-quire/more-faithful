package org.studioquire.oldfaithful.core.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.studioquire.oldfaithful.core.api.Drops;
import org.studioquire.oldfaithful.core.impl.Attachments;

/** Attachments in an entity's own NBT, and the one place a mob's items come into the world. */
@Mixin(Entity.class)
abstract class EntityMixin implements Attachments.Holder {
	@Unique
	private Attachments.Data faithful$attachments;

	@Override
	public Attachments.Data faithful$attachments() {
		return faithful$attachments;
	}

	@Override
	public void faithful$setAttachments(Attachments.Data data) {
		faithful$attachments = data;
	}

	@Inject(method = "write", at = @At("TAIL"))
	private void faithful$write(NbtCompound nbt, CallbackInfo ci) {
		Attachments.writeEntity((Entity) (Object) this, nbt);
	}

	@Inject(method = "read", at = @At("TAIL"))
	private void faithful$read(NbtCompound nbt, CallbackInfo ci) {
		Attachments.readEntity((Entity) (Object) this, nbt);
	}

	@Inject(method = "dropItem(Lnet/minecraft/item/ItemStack;F)Lnet/minecraft/entity/ItemEntity;", at = @At("HEAD"), cancellable = true)
	private void faithful$take(ItemStack stack, float yOffset, CallbackInfoReturnable<ItemEntity> cir) {
		if (Drops.takeMobStack((Entity) (Object) this, stack)) cir.setReturnValue(null);
	}
}
