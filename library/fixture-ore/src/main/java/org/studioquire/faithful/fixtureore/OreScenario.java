package org.studioquire.faithful.fixtureore;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.studioquire.faithful.harness.Scenario;

/** A mod's own short test: its ore placed, read back and dropping an ingot. */
public final class OreScenario implements Scenario {
	@Override
	public void run(World world, PlayerEntity player, Check check) {
		int x = (int) player.x + 3, y = 120, z = (int) player.z;
		world.setBlock(x, y, z, FixtureOre.ore.get().id);
		check.that("the ore reads back", world.getBlockId(x, y, z) == FixtureOre.ore.get().id);
		check.that("it drops an ingot", FixtureOre.ore.get().getDroppedItemId(0, new java.util.Random(1)) == FixtureOre.ingot.get().id);
		world.setBlock(x, y, z, 0);
		check.that("and it's gone", world.getBlockId(x, y, z) == 0);
	}
}
