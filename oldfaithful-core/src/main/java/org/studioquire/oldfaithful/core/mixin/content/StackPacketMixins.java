package org.studioquire.oldfaithful.core.mixin.content;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.ItemEntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.impl.content.StackData;
import org.studioquire.oldfaithful.core.impl.content.Wire;

/** Stack data after the stacks in the five packets that matter, on a connection where both ends have content. */
final class StackPacketMixins {
	private StackPacketMixins() {
	}

	@Mixin(ClickSlotC2SPacket.class)
	abstract static class ClickSlot {
		@Shadow
		public ItemStack stack;

		@Inject(method = "read", at = @At("TAIL"))
		private void faithful$read(DataInputStream in, CallbackInfo ci) throws IOException {
			if (Wire.stackData()) StackData.readTrailer(in, stack);
		}

		@Inject(method = "write", at = @At("TAIL"))
		private void faithful$write(DataOutputStream out, CallbackInfo ci) throws IOException {
			if (Wire.stackData()) StackData.writeTrailer(out, StackData.MAX_PACKET_BYTES, stack);
		}

		@ModifyReturnValue(method = "size", at = @At("RETURN"))
		private int faithful$size(int size) {
			return size + StackData.wireSize(StackData.MAX_PACKET_BYTES, stack);
		}
	}

	@Mixin(PlayerInteractBlockC2SPacket.class)
	abstract static class Interact {
		@Shadow
		public ItemStack stack;

		@Inject(method = "read", at = @At("TAIL"))
		private void faithful$read(DataInputStream in, CallbackInfo ci) throws IOException {
			if (Wire.stackData()) StackData.readTrailer(in, stack);
		}

		@Inject(method = "write", at = @At("TAIL"))
		private void faithful$write(DataOutputStream out, CallbackInfo ci) throws IOException {
			if (Wire.stackData()) StackData.writeTrailer(out, StackData.MAX_PACKET_BYTES, stack);
		}

		@ModifyReturnValue(method = "size", at = @At("RETURN"))
		private int faithful$size(int size) {
			return size + StackData.wireSize(StackData.MAX_PACKET_BYTES, stack);
		}
	}

	@Mixin(ScreenHandlerSlotUpdateS2CPacket.class)
	abstract static class SlotUpdate implements StackData.Roomy {
		@Shadow
		public ItemStack stack;

		@Unique
		private int faithful$room = StackData.MAX_PACKET_BYTES;

		@Override
		public void faithful$room(int room) {
			faithful$room = Math.min(room, StackData.MAX_PACKET_BYTES);
		}

		@Inject(method = "read", at = @At("TAIL"))
		private void faithful$read(DataInputStream in, CallbackInfo ci) throws IOException {
			if (Wire.stackData()) StackData.readTrailer(in, stack);
		}

		@Inject(method = "write", at = @At("TAIL"))
		private void faithful$write(DataOutputStream out, CallbackInfo ci) throws IOException {
			if (Wire.stackData()) StackData.writeTrailer(out, faithful$room, stack);
		}

		@ModifyReturnValue(method = "size", at = @At("RETURN"))
		private int faithful$size(int size) {
			return size + StackData.wireSize(faithful$room, stack);
		}
	}

	@Mixin(InventoryS2CPacket.class)
	abstract static class Inventory implements StackData.Roomy {
		@Shadow
		public ItemStack[] contents;

		@Unique
		private int faithful$room = StackData.MAX_PACKET_BYTES;

		@Override
		public void faithful$room(int room) {
			faithful$room = Math.min(room, StackData.MAX_PACKET_BYTES);
		}

		@Inject(method = "read", at = @At("TAIL"))
		private void faithful$read(DataInputStream in, CallbackInfo ci) throws IOException {
			if (Wire.stackData()) StackData.readTrailer(in, contents);
		}

		@Inject(method = "write", at = @At("TAIL"))
		private void faithful$write(DataOutputStream out, CallbackInfo ci) throws IOException {
			if (Wire.stackData()) StackData.writeTrailer(out, faithful$room, contents);
		}

		@ModifyReturnValue(method = "size", at = @At("RETURN"))
		private int faithful$size(int size) {
			return size + StackData.wireSize(faithful$room, contents);
		}
	}

	/** A dropped item carries only its id on the wire, and the client needs no more: data stays with the server's entity. */
	@Mixin(ItemEntitySpawnS2CPacket.class)
	abstract static class ItemSpawn {
	}
}
