package org.studioquire.oldfaithful.core.mixin.content;

import java.io.DataInputStream;
import java.io.IOException;

import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.impl.content.ContentRegistry;
import org.studioquire.oldfaithful.core.impl.content.Overlay;
import org.studioquire.oldfaithful.core.impl.content.Wire;

/**
 * Multi-block updates: the arrays are bytes, so an id's high bits ride in its meta's top nibble. Read whole here and kept beside the packet,
 * with stand-ins left in the byte array, so a check that reads it (hardening's) sees real blocks.
 */
@Mixin(ChunkDeltaUpdateS2CPacket.class)
abstract class ChunkDeltaPacketMixin implements Wire.DeltaIds {
	@Shadow
	public byte[] blockRawIds;
	@Shadow
	public byte[] blockMetadata;
	@Shadow
	public int size;

	@Unique
	private char[] faithful$ids;
	@Unique
	private int faithful$next;

	@Inject(method = "read", at = @At("TAIL"))
	private void faithful$whole(DataInputStream stream, CallbackInfo ci) throws IOException {
		if (!Wire.serverContent) return;
		faithful$ids = new char[size];
		for (int i = 0; i < size; i++) {
			int id = blockRawIds[i] & 0xFF | (blockMetadata[i] >> 4 & 15) << 8;
			if (id >= 256 && !ContentRegistry.modBlock(id)) throw new IOException("The server sent a block this game doesn't have (id " + id + ")");
			faithful$ids[i] = (char) id;
			blockRawIds[i] = (byte) Overlay.standId(id);
			blockMetadata[i] &= 15;
		}
	}

	@Override
	public int faithful$next(int fallback) {
		return faithful$ids == null ? fallback : faithful$ids[faithful$next++];
	}
}
