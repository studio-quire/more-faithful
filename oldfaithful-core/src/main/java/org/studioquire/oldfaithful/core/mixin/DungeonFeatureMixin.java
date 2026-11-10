package org.studioquire.oldfaithful.core.mixin;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.DungeonFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.studioquire.oldfaithful.core.api.Worldgen;
import org.studioquire.oldfaithful.core.impl.DungeonFeatureInvoker;

/**
 * A dungeon placed, heard at the one line only a placed dungeon reaches, and its two lists for mods to draw from. The two ints after the height are its half widths.
 * <p>
 * A veto runs vanilla's own code on: its writes land in a map rather than the world, and its reads see them, so it makes the same draws in the same order and leaves no cell changed. That's why these
 * wrap each call rather than returning early, which would leave every ore after it in the chunk's stream somewhere else.
 */
@Mixin(DungeonFeature.class)
abstract class DungeonFeatureMixin implements DungeonFeatureInvoker {
	@Unique
	private Map<Long, Integer> faithful$vetoed;

	@Shadow
	private ItemStack getRandomChestItem(Random random) {
		throw new AssertionError();
	}

	@Shadow
	private String getRandomEntity(Random random) {
		throw new AssertionError();
	}

	@Override
	public ItemStack faithful$chestItem(Random random) {
		return getRandomChestItem(random);
	}

	@Override
	public String faithful$monster(Random random) {
		return getRandomEntity(random);
	}

	@Unique
	private static long faithful$key(int x, int y, int z) {
		return (long) (x + (1 << 25)) << 34 | (long) (z + (1 << 25)) << 8 | y;
	}

	@Inject(method = "generate", at = @At("HEAD"))
	private void faithful$veto(World world, Random random, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		faithful$vetoed = Worldgen.dungeonVetoed(world, x, y, z) ? new HashMap<>() : null;
	}

	@WrapOperation(method = "generate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlock(IIII)Z"))
	private boolean faithful$setBlock(World world, int x, int y, int z, int id, Operation<Boolean> original) {
		if (faithful$vetoed == null) return original.call(world, x, y, z, id);
		faithful$vetoed.put(faithful$key(x, y, z), id);
		return true;
	}

	@WrapOperation(method = "generate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getMaterial(III)Lnet/minecraft/block/material/Material;"))
	private Material faithful$getMaterial(World world, int x, int y, int z, Operation<Material> original) {
		Integer id = faithful$vetoed == null ? null : faithful$vetoed.get(faithful$key(x, y, z));
		if (id == null) return original.call(world, x, y, z);
		return id == 0 ? Material.AIR : Block.BLOCKS[id].material;
	}

	@WrapOperation(method = "generate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isAir(III)Z"))
	private boolean faithful$isAir(World world, int x, int y, int z, Operation<Boolean> original) {
		Integer id = faithful$vetoed == null ? null : faithful$vetoed.get(faithful$key(x, y, z));
		return id == null ? original.call(world, x, y, z) : id == 0;
	}

	/** A chest or spawner that's nowhere, for the draws that fill it. */
	@WrapOperation(method = "generate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockEntity(III)Lnet/minecraft/block/entity/BlockEntity;"))
	private BlockEntity faithful$getBlockEntity(World world, int x, int y, int z, Operation<BlockEntity> original) {
		if (faithful$vetoed == null) return original.call(world, x, y, z);
		Integer id = faithful$vetoed.get(faithful$key(x, y, z));
		return id != null && id == Block.SPAWNER.id ? new MobSpawnerBlockEntity() : new ChestBlockEntity();
	}

	@Inject(method = "generate", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/MobSpawnerBlockEntity;setSpawnedEntityId(Ljava/lang/String;)V", shift = At.Shift.AFTER))
	private void faithful$placed(World world, Random random, int x, int y, int z, CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 4) int halfX, @Local(ordinal = 5) int halfZ) {
		if (faithful$vetoed == null) Worldgen.dungeonPlaced(world, x, y, z, halfX, halfZ);
	}
}
