package org.studioquire.faithful.golems;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.studioquire.oldfaithful.core.api.Sounds;

/** Used on the top of two stacked iron blocks: they become a golem. */
public final class GolemCore extends Item {
	GolemCore(int id, int texture) {
		super(id);
		setTextureId(texture);
		setMaxCount(1);
	}

	@Override
	public boolean useOnBlock(ItemStack stack, PlayerEntity user, World world, int x, int y, int z, int side) {
		int iron = Block.IRON_BLOCK.id;
		if (world.getBlockId(x, y, z) != iron || world.getBlockId(x, y - 1, z) != iron) return false;
		if (!world.isRemote) {
			GolemEntity golem = new GolemEntity(world);
			golem.setPositionAndAngles(x + 0.5, y - 1, z + 0.5, user.yaw + 180.0F, 0.0F);
			if (!world.spawnEntity(golem)) return false;
			world.worldEvent(2001, x, y, z, iron);
			world.worldEvent(2001, x, y - 1, z, iron);
			world.setBlock(x, y, z, 0);
			world.setBlock(x, y - 1, z, 0);
			Sounds.play(world, x + 0.5, y, z + 0.5, GolemEntity.DOOR, 1.0F, 0.6F);
		}
		stack.count--;
		return true;
	}
}
