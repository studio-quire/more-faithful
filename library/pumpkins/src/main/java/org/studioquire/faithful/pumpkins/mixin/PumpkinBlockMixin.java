package org.studioquire.faithful.pumpkins.mixin;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.PumpkinBlock;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.studioquire.faithful.pumpkins.Spreading;

/** Beta's pumpkin already ticks randomly and does nothing with it; now it spreads. The jack-o'-lantern is the same class, and {@link Spreading} leaves it be. */
@Mixin(PumpkinBlock.class)
abstract class PumpkinBlockMixin extends Block {
	private PumpkinBlockMixin(int id, Material material) {
		super(id, material);
	}

	@Override
	public void onTick(World world, int x, int y, int z, Random random) {
		Spreading.tick(world, x, y, z, id, random);
	}
}
