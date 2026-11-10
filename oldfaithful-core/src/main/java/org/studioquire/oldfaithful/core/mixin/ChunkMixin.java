package org.studioquire.oldfaithful.core.mixin;

import net.minecraft.nbt.NbtElement;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.studioquire.oldfaithful.core.api.Chunks;
import org.studioquire.oldfaithful.core.impl.Attachments;
import org.studioquire.oldfaithful.core.impl.ChunkData;

/** What a chunk carries for the core, from load to save, and the two calls every chunk cache makes. */
@Mixin(Chunk.class)
abstract class ChunkMixin implements ChunkData.Holder, Attachments.Holder {
	@Unique
	private NbtElement faithful$data;
	@Unique
	private Attachments.Data faithful$attachments;

	@Override
	public NbtElement faithful$data() {
		return faithful$data;
	}

	@Override
	public void faithful$setData(NbtElement data) {
		faithful$data = data;
	}

	@Override
	public Attachments.Data faithful$attachments() {
		return faithful$attachments;
	}

	@Override
	public void faithful$setAttachments(Attachments.Data data) {
		faithful$attachments = data;
	}

	@Inject(method = "load", at = @At("TAIL"))
	private void faithful$loaded(CallbackInfo ci) {
		Chunks.loaded((Chunk) (Object) this);
	}

	@Inject(method = "unload", at = @At("HEAD"))
	private void faithful$unloaded(CallbackInfo ci) {
		Chunks.unloaded((Chunk) (Object) this);
	}
}
