package org.studioquire.faithful.blueflowers.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.block.Block;
import net.minecraft.world.gen.chunk.OverworldChunkGenerator;
import net.minecraft.world.gen.chunk.SkyChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * No lapis lazuli ore in new land: the lupin's dye has its place. Beta's vein still runs, at the same spot and with every random draw it always made, and lays stone in the stone it would have
 * turned to ore ({@code OreFeature} draws the same whatever block it's given, and only ever replaces stone), so everything a chunk decorates after it stands where beta puts it for the seed. What
 * differs from beta is the vein's own cells, and whatever would have read them: a later vein crossing them takes them as stone. The core has no hook for taking one of vanilla's own ores out, and
 * this is the one line that names it, in each generator that lays it.
 */
@Mixin({OverworldChunkGenerator.class, SkyChunkGenerator.class})
abstract class LapisVeinMixin {
	@ModifyExpressionValue(method = "decorate", at = @At(value = "FIELD", target = "Lnet/minecraft/block/Block;LAPIS_ORE:Lnet/minecraft/block/Block;"))
	private Block blueflowers$stoneForLapis(Block lapis) {
		return Block.STONE;
	}
}
