package org.studioquire.faithful.waterbottles;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class GlassBottle extends Bottle {
	private static final double REACH = 5.0, STEP = 0.1;

	GlassBottle(int id, int texture) {
		super(id, texture);
	}

	/**
	 * Fills where the world runs, with no guess on a client: the server looks along the angles of a tick ago, so the two can disagree at a shore, and vanilla only
	 * corrects a hand that changed on the server. A wrong guess of a fill would stay on the client. The answer comes back as a slot update.
	 */
	@Override
	public ItemStack use(ItemStack stack, World world, PlayerEntity user) {
		if (world.isRemote || !seesWater(world, user)) return stack;
		return exchange(stack, world, user, new ItemStack(WaterBottles.waterBottle.get()));
	}

	/** Any water before the first solid thing, flowing too: nothing is taken from it, so the bucket's rule of sources only has no reason here. */
	private static boolean seesWater(World world, PlayerEntity user) {
		double yaw = Math.toRadians(user.yaw), pitch = Math.toRadians(user.pitch);
		double dx = -Math.sin(yaw) * Math.cos(pitch), dy = -Math.sin(pitch), dz = Math.cos(yaw) * Math.cos(pitch);
		// A client's y is its eyes and a server's its feet, which standingEyeHeight says, as in BucketItem.
		double x = user.x, y = user.y + 1.62 - user.standingEyeHeight, z = user.z;
		// raycast walks its start along the ray, so that vector is spent afterwards.
		HitResult wall = world.raycast(Vec3d.createCached(x, y, z), Vec3d.createCached(x + dx * REACH, y + dy * REACH, z + dz * REACH));
		double reach = wall == null ? REACH : Vec3d.createCached(x, y, z).distanceTo(wall.pos);
		for (double d = 0; d < reach; d += STEP) {
			if (world.getMaterial(MathHelper.floor(x + dx * d), MathHelper.floor(y + dy * d), MathHelper.floor(z + dz * d)) == Material.WATER) return true;
		}
		return false;
	}
}
