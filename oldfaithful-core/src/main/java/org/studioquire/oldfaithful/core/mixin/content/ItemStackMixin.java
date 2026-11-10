package org.studioquire.oldfaithful.core.mixin.content;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.api.Tools;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;
import org.studioquire.oldfaithful.core.impl.content.StackData;
import org.studioquire.oldfaithful.core.impl.content.StackDataHolder;

/**
 * Stacks in saves keep vanilla's form: {@code id} and {@code Damage} a stand-in a vanilla reader can use, the real name beside it. The one
 * place item NBT is written, so every inventory gets this. Also the stack's data, which is part of what a stack is.
 */
@Mixin(ItemStack.class)
abstract class ItemStackMixin implements StackDataHolder {
	@Shadow
	public int itemId;
	@Shadow
	private int damage;

	@Unique
	private NbtCompound faithful$data;

	@Override
	public NbtCompound faithful$data() {
		return faithful$data;
	}

	@Override
	public void faithful$setData(NbtCompound data) {
		faithful$data = data;
	}

	@ModifyReturnValue(method = "split", at = @At("RETURN"))
	private ItemStack faithful$split(ItemStack part) {
		((StackDataHolder) (Object) part).faithful$setData(StackData.copy(faithful$data));
		return part;
	}

	@ModifyReturnValue(method = "copy", at = @At("RETURN"))
	private ItemStack faithful$copy(ItemStack copy) {
		((StackDataHolder) (Object) copy).faithful$setData(StackData.copy(faithful$data));
		return copy;
	}

	/** {@code equals2} is behind {@code areEqual}, which is how a server sees a slot changed: without it a change to data alone never reaches a client. */
	@ModifyReturnValue(method = {"isItemEqual", "equals(Lnet/minecraft/item/ItemStack;)Z", "equals2"}, at = @At("RETURN"))
	private boolean faithful$sameData(boolean same, ItemStack other) {
		return same && StackData.same(faithful$data, ((StackDataHolder) (Object) other).faithful$data());
	}

	/** What times a break: both clients and the server's checks ask through here. */
	@ModifyReturnValue(method = "getMiningSpeedMultiplier", at = @At("RETURN"))
	private float faithful$toolSpeed(float speed, Block block) {
		return Tools.speed((ItemStack) (Object) this, block, speed);
	}

	@Inject(method = "writeNbt", at = @At("TAIL"))
	private void faithful$write(NbtCompound nbt, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<NbtCompound> cir) {
		String name = ContentRegistry.itemName(itemId);
		boolean hasData = faithful$data != null && !faithful$data.values().isEmpty();
		if (name == null && !hasData) return;
		NbtCompound tag = new NbtCompound();
		tag.putByte("V", (byte) 1);
		if (name != null) {
			tag.putString("id", name);
			tag.putShort("Damage", (short) damage);
			nbt.putShort("id", (short) ContentRegistry.itemStandInId(itemId));
			nbt.putShort("Damage", (short) ContentRegistry.itemStandInDamage(itemId));
		}
		if (hasData) tag.put("Data", StackData.copy(faithful$data)); // the tree's own: it may be written out on another thread while the game goes on
		nbt.put("Faithful", tag);
	}

	@Inject(method = "readNbt", at = @At("TAIL"))
	private void faithful$read(NbtCompound nbt, CallbackInfo ci) {
		if (!nbt.contains("Faithful")) return;
		NbtCompound tag = nbt.getCompound("Faithful");
		if (tag.contains("id")) {
			int id = ContentRegistry.itemId(tag.getString("id"));
			if (id >= 0) {
				itemId = id;
				damage = tag.getShort("Damage");
			}
		}
		if (tag.contains("Data")) faithful$data = tag.getCompound("Data");
	}
}
