package org.studioquire.faithful.explosivearrows;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.studioquire.faithful.explosivearrows.mixin.ArrowEntityAccessor;
import org.studioquire.oldfaithful.core.api.EntitySpawnData;

/**
 * Beta's arrow, flying as beta's does, that bursts on the first block or thing it strikes: beta's own blast at half a creeper's size, from which the
 * core's blast hook takes every block but TNT and {@code ExplosionMixin} sets who's hurt and how much. Spent where it bursts, so never picked up.
 */
public class ExplosiveArrow extends ArrowEntity implements EntitySpawnData {
	/** Half a creeper's 3: beta's rule hurts out to twice this, three blocks. */
	public static final float POWER = 1.5F;
	/**
	 * How far a burst on something stands back from that thing's own position, toward where the arrow came from. Beta measures a blast to a thing's
	 * position (a mob's feet), as a creeper's goes off at its own feet, so close to it a direct hit is the blast's full 12, and the blast throws it on the
	 * way the arrow flew. Not at it: beta's throw divides by that distance, so a blast exactly at a thing's position sends it to NaN.
	 */
	static final double STAND_BACK = 0.1;

	public ExplosiveArrow(World world) {
		super(world);
	}

	public ExplosiveArrow(World world, double x, double y, double z) {
		super(world, x, y, z);
	}

	public ExplosiveArrow(World world, LivingEntity owner) {
		super(world, owner);
	}

	@Override
	public void tick() {
		super.tick();
		// In the ground means it struck a block this tick: this arrow never stays there. Where it stopped is where beta's arrow sits, just short of the face.
		// Beta's arrow starting a tick exactly on the face it strikes divides by the nothing it moved, and is put at NaN: it was on the face, where it began.
		if (!world.isRemote && !dead && ((ArrowEntityAccessor) this).explosivearrows$inGround()) {
			if (Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)) burst(x, y, z);
			else burst(prevX, prevY, prevZ);
		}
	}

	/** It struck {@code hit}, whether its own hit landed or not: it bursts on it. False on a client on a server, which waits for the server's word. */
	public boolean burstOn(Entity hit) {
		if (world.isRemote) return false;
		double backX = -velocityX, backZ = -velocityZ, length = Math.sqrt(backX * backX + backZ * backZ);
		if (length < 1.0E-4) burst(hit.x, hit.y + STAND_BACK, hit.z);
		else burst(hit.x + backX / length * STAND_BACK, hit.y, hit.z + backZ / length * STAND_BACK);
		return true;
	}

	private void burst(double x, double y, double z) {
		markDead();
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			world.createExplosion(this, x, y, z, POWER);
			return;
		}
		// A server's createExplosion as beta has it, but for its packet, whose puffs a client draws only within 16 blocks: the burst's own message takes its
		// place, to the same players, for each to draw as beta would but out to SEEN. Every client here has this mod, as it has content.
		Explosion blast = new Explosion(world, this, x, y, z, POWER);
		blast.explode();
		blast.playExplosionSound(false);
		@SuppressWarnings("unchecked")
		List<BlockPos> cells = new ArrayList<>(blast.damagedBlocks);
		int baseX = (int) x, baseY = (int) y, baseZ = (int) z;
		for (Object p : world.players) {
			PlayerEntity player = (PlayerEntity) p;
			if (player.getSquaredDistance(x, y, z) >= ExplosiveArrows.SEEN * ExplosiveArrows.SEEN || !ExplosiveArrows.bursts.available(player)) continue;
			ExplosiveArrows.bursts.toPlayer(player, out -> {
				out.writeDouble(x);
				out.writeDouble(y);
				out.writeDouble(z);
				out.writeFloat(POWER);
				out.writeShort(cells.size());
				for (BlockPos c : cells) {
					out.writeByte(c.x - baseX);
					out.writeByte(c.y - baseY);
					out.writeByte(c.z - baseZ);
				}
			});
		}
	}

	/**
	 * Who something its blast hurts is hurt by. Anyone else: the shooter, as by the arrow's own hit, so a server's PvP setting, a pig zombie's anger and a kill
	 * are theirs. The shooter: nothing, as by TNT they lit, so firing too close always costs them, whatever the server's PvP. With no shooter (a
	 * dispenser's), nothing, for everyone.
	 */
	public Entity hurting(Entity hit) {
		if (owner == null || hit == owner) return null;
		if (owner instanceof PlayerEntity && hit instanceof PlayerEntity && ((PlayerEntity) owner).name.equals(((PlayerEntity) hit).name)) return null;
		return owner;
	}

	@Override
	public void onPlayerInteraction(PlayerEntity player) {
	}

	/** So a client's copy knows its shooter, as beta's arrow spawn tells it: without it the copy strikes the shooter as it sets off and drops at their feet. */
	@Override
	public void writeSpawnData(NbtCompound nbt) {
		if (owner != null) nbt.putInt("Owner", owner.id);
	}

	@Override
	public void readSpawnData(NbtCompound nbt) {
		if (!nbt.contains("Owner")) return;
		int id = nbt.getInt("Owner");
		for (Object e : world.entities) {
			if (e instanceof LivingEntity && ((LivingEntity) e).id == id) {
				owner = (LivingEntity) e;
				return;
			}
		}
	}
}
