package org.studioquire.faithful.explosivearrows.client;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ExplosionParticle;
import net.minecraft.client.particle.FireSmokeParticle;
import net.minecraft.client.render.entity.ArrowEntityRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.studioquire.faithful.explosivearrows.ExplosiveArrow;
import org.studioquire.faithful.explosivearrows.ExplosiveArrows;
import org.studioquire.oldfaithful.core.api.FaithfulClientMod;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.client.Client;
import org.studioquire.oldfaithful.core.api.client.Renderers;

public final class ExplosiveArrowsClient implements FaithfulClientMod {
	/** Beta's arrow sheet with this arrow's head drawn in, everything in the same places. */
	public static final String TEXTURE = "/assets/explosive-arrows/textures/entity/explosive_arrow.png";
	/** Beta's reach for a particle, from the camera. */
	private static final double BETA = 16.0;
	/** More cells than a blast this size reaches, by far: anything past it isn't one. */
	private static final int MOST_CELLS = 4096;

	@Override
	public void initClient(Mod mod) {
		Renderers.entity(ExplosiveArrow.class, new ArrowEntityRenderer() {
			@Override
			protected void bindTexture(String path) {
				super.bindTexture(TEXTURE);
			}
		});
		ExplosiveArrows.puffs = ExplosiveArrowsClient::far;
		ExplosiveArrows.bursts.onClient(ExplosiveArrowsClient::burst);
	}

	/** One of a burst's puffs between beta's 16 blocks and {@link ExplosiveArrows#SEEN}, drawn here as beta's own would draw it nearer. */
	private static boolean far(World world, String name, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
		Minecraft mc = Client.minecraft();
		if (mc.camera == null || mc.particleManager == null) return false;
		double dx = mc.camera.x - x, dy = mc.camera.y - y, dz = mc.camera.z - z, squared = dx * dx + dy * dy + dz * dz;
		if (squared <= BETA * BETA || squared > ExplosiveArrows.SEEN * ExplosiveArrows.SEEN) return false;
		if (name.equals("explode")) mc.particleManager.addParticle(new ExplosionParticle(world, x, y, z, velocityX, velocityY, velocityZ));
		else if (name.equals("smoke")) mc.particleManager.addParticle(new FireSmokeParticle(world, x, y, z, velocityX, velocityY, velocityZ));
		else return false;
		return true;
	}

	/** A server's burst: what beta's client does with a blast's packet, its puffs drawn out to {@link ExplosiveArrows#SEEN}. */
	@SuppressWarnings("unchecked")
	private static void burst(PlayerEntity player, DataInputStream in) throws IOException {
		double x = in.readDouble(), y = in.readDouble(), z = in.readDouble();
		float power = in.readFloat();
		int count = in.readUnsignedShort();
		// Nothing to draw in what can't be a burst, and nothing in it worth a disconnect.
		if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || !(power > 0.0F && power <= 16.0F) || count > MOST_CELLS) return;
		int baseX = (int) x, baseY = (int) y, baseZ = (int) z;
		Set<BlockPos> cells = new HashSet<>();
		for (int i = 0; i < count; i++) cells.add(new BlockPos(baseX + in.readByte(), baseY + in.readByte(), baseZ + in.readByte()));
		Explosion blast = new Explosion(player.world, null, x, y, z, power);
		blast.damagedBlocks = cells;
		ExplosiveArrows.drawing = true;
		try {
			blast.playExplosionSound(true);
		} finally {
			ExplosiveArrows.drawing = false;
		}
	}
}
