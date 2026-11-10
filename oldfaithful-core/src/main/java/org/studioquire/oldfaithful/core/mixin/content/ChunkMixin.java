package org.studioquire.oldfaithful.core.mixin.content;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkNibbleArray;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.studioquire.oldfaithful.core.api.BlockEntities;
import org.studioquire.oldfaithful.core.impl.content.Overlay;

/**
 * The overlay above vanilla's arrays: {@code blocks} and {@code meta} stay a whole vanilla chunk, with a stand-in under each mod block.
 * In {@code setBlock}, the id changes first and the meta after {@code onBreak}, and vanilla's buttons and levers read the old meta there,
 * so at the store the cell keeps the old real meta, and only vanilla's own {@code meta.set} moves it on.
 */
@Mixin(Chunk.class)
abstract class ChunkMixin implements Overlay.Holder {
	@Shadow
	public byte[] blocks;
	@Shadow
	public ChunkNibbleArray meta;

	@Unique
	private char[] faithful$ext;
	@Unique
	private Overlay.Dormant faithful$dormant;

	@Override
	public char[] faithful$ext() {
		return faithful$ext;
	}

	@Override
	public void faithful$setExt(char[] ext) {
		faithful$ext = ext;
	}

	@Override
	public Overlay.Dormant faithful$dormant() {
		return faithful$dormant;
	}

	@Override
	public void faithful$setDormant(Overlay.Dormant dormant) {
		faithful$dormant = dormant;
	}

	@Unique
	private int faithful$cell(int x, int y, int z) {
		return faithful$ext == null ? 0 : faithful$ext[x << 11 | z << 7 | y];
	}

	@ModifyReturnValue(method = "getBlockId", at = @At("RETURN"))
	private int faithful$id(int id, int x, int y, int z) {
		int c = faithful$cell(x, y, z);
		return c != 0 ? c & 0xFFF : id;
	}

	@ModifyReturnValue(method = "getBlockMeta", at = @At("RETURN"))
	private int faithful$meta(int meta, int x, int y, int z) {
		int c = faithful$cell(x, y, z);
		return c != 0 ? c >> 12 : meta;
	}

	@WrapOperation(method = "setBlockMeta", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/ChunkNibbleArray;set(IIII)V"))
	private void faithful$setMeta(ChunkNibbleArray nibbles, int x, int y, int z, int value, Operation<Void> set) {
		int c = faithful$cell(x, y, z);
		if (c == 0) set.call(nibbles, x, y, z, value);
		else faithful$ext[x << 11 | z << 7 | y] = (char) (c & 0xFFF | (value & 15) << 12);
	}

	/** The id changes, the old real meta stays readable. */
	@Unique
	private void faithful$store(int x, int y, int z, int id) {
		int c = faithful$cell(x, y, z);
		int oldMeta = c != 0 ? c >> 12 : meta.get(x, y, z);
		if (Overlay.modded(id)) {
			if (faithful$ext == null) faithful$ext = new char[Overlay.CELLS];
			faithful$ext[x << 11 | z << 7 | y] = (char) (id | oldMeta << 12);
		} else if (c != 0) {
			faithful$ext[x << 11 | z << 7 | y] = 0;
			meta.set(x, y, z, oldMeta);
		}
	}

	/** Vanilla's {@code meta.set} in {@code setBlock}: the block's meta goes in the overlay, the stand-in's in the nibbles. */
	@Unique
	private void faithful$placeMeta(ChunkNibbleArray nibbles, int x, int y, int z, int value, int id, Operation<Void> set) {
		if (Overlay.modded(id)) {
			faithful$ext[x << 11 | z << 7 | y] = (char) (id | (value & 15) << 12);
			set.call(nibbles, x, y, z, Overlay.standMeta(id, value));
		} else {
			set.call(nibbles, x, y, z, value);
		}
	}

	// setBlock with meta. Locals: this, x, y, z, rawId, meta; 6 the id as a byte, 7 the height, 8 the old id.
	@ModifyVariable(method = "setBlock(IIIII)Z", at = @At("STORE"), index = 6)
	private int faithful$underMeta(int value, int x, int y, int z, int rawId, int meta) {
		return (byte) Overlay.standId(rawId);
	}

	@ModifyVariable(method = "setBlock(IIIII)Z", at = @At("STORE"), index = 8)
	private int faithful$oldIdMeta(int value, int x, int y, int z, int rawId, int meta) {
		int c = faithful$cell(x, y, z);
		return c != 0 ? c & 0xFFF : value;
	}

	@WrapOperation(method = "setBlock(IIIII)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/ChunkNibbleArray;get(III)I"))
	private int faithful$oldMeta(ChunkNibbleArray nibbles, int x, int y, int z, Operation<Integer> get) {
		int c = faithful$cell(x, y, z);
		return c != 0 ? c >> 12 : get.call(nibbles, x, y, z);
	}

	@Inject(method = "setBlock(IIIII)Z", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/world/chunk/Chunk;blocks:[B", ordinal = 1))
	private void faithful$storeMeta(int x, int y, int z, int rawId, int meta, CallbackInfoReturnable<Boolean> cir) {
		faithful$store(x, y, z, rawId);
	}

	@WrapOperation(method = "setBlock(IIIII)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/ChunkNibbleArray;set(IIII)V"))
	private void faithful$metaMeta(ChunkNibbleArray nibbles, int x, int y, int z, int value, Operation<Void> set, int cx, int cy, int cz, int rawId, int rawMeta) {
		faithful$placeMeta(nibbles, x, y, z, value, rawId, set);
	}

	@Redirect(method = "setBlock(IIIII)Z", at = @At(value = "FIELD", target = "Lnet/minecraft/block/Block;BLOCKS_LIGHT_OPACITY:[I", args = "array=get"))
	private int faithful$opacityMeta(int[] opacity, int index, int x, int y, int z, int rawId, int meta) {
		return opacity[rawId];
	}

	/** {@code BlockEntities.swap} is one block becoming another around the same block entity: neither end of it is a break or a placing. */
	@WrapOperation(method = "setBlock(IIIII)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;onBreak(Lnet/minecraft/world/World;III)V"))
	private void faithful$swapBreak(Block block, World world, int x, int y, int z, Operation<Void> onBreak) {
		if (!BlockEntities.swapping()) onBreak.call(block, world, x, y, z);
	}

	@WrapOperation(method = "setBlock(IIIII)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;onPlaced(Lnet/minecraft/world/World;III)V"))
	private void faithful$swapPlaced(Block block, World world, int x, int y, int z, Operation<Void> onPlaced) {
		if (!BlockEntities.swapping()) onPlaced.call(block, world, x, y, z);
	}

	// setBlock. Locals: this, x, y, z, rawId; 5 the id as a byte, 6 the height, 7 the old id.
	@ModifyVariable(method = "setBlock(IIII)Z", at = @At("STORE"), index = 5)
	private int faithful$under(int value, int x, int y, int z, int rawId) {
		return (byte) Overlay.standId(rawId);
	}

	@ModifyVariable(method = "setBlock(IIII)Z", at = @At("STORE"), index = 7)
	private int faithful$oldId(int value, int x, int y, int z, int rawId) {
		int c = faithful$cell(x, y, z);
		return c != 0 ? c & 0xFFF : value;
	}

	@Inject(method = "setBlock(IIII)Z", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/world/chunk/Chunk;blocks:[B", ordinal = 1))
	private void faithful$storeId(int x, int y, int z, int rawId, CallbackInfoReturnable<Boolean> cir) {
		faithful$store(x, y, z, rawId);
	}

	@WrapOperation(method = "setBlock(IIII)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/ChunkNibbleArray;set(IIII)V"))
	private void faithful$metaId(ChunkNibbleArray nibbles, int x, int y, int z, int value, Operation<Void> set, int cx, int cy, int cz, int rawId) {
		faithful$placeMeta(nibbles, x, y, z, value, rawId, set);
	}

	@Redirect(method = "setBlock(IIII)Z", at = @At(value = "FIELD", target = "Lnet/minecraft/block/Block;BLOCKS_LIGHT_OPACITY:[I", args = "array=get"))
	private int faithful$opacity(int[] opacity, int index, int x, int y, int z, int rawId) {
		return opacity[rawId];
	}

	/**
	 * The height map's loops look up opacity by {@code blocks[i] & 255} in one expression: where a mod's block is, hand them a spare
	 * id whose opacity is that block's for the moment.
	 */
	@Redirect(method = {"populateHeightMap", "updateHeightMap"}, at = @At(value = "FIELD", target = "Lnet/minecraft/world/chunk/Chunk;blocks:[B", args = "array=get"))
	private byte faithful$opacityCell(byte[] array, int index) {
		return Overlay.opacityCell(faithful$ext, array, index);
	}
}
