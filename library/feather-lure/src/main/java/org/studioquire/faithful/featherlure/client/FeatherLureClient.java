package org.studioquire.faithful.featherlure.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ItemParticle;
import net.minecraft.item.Item;
import org.studioquire.faithful.featherlure.FeatherLure;
import org.studioquire.oldfaithful.core.api.FaithfulClientMod;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.client.Client;

public final class FeatherLureClient implements FaithfulClientMod {
	/** Beta's bobber with a small feather over its hook, the tile alone (8x8), for a lured line's bobber. */
	public static final String LURED_BOBBER = "/assets/feather-lure/textures/entity/lured_bobber.png";
	/** As a snowball breaks into eight bits of itself. */
	private static final int BITS = 8;
	/** Further than any bobber of the player's can be from them (beta reels one in at 32). */
	private static final double REACH = 64.0;

	/** Lost lures this client has heard of, for the mod's test. */
	public static int heard;

	@Override
	public void initClient(Mod mod) {
		FeatherLure.lost.onClient((player, in) -> {
			double x = in.readDouble(), y = in.readDouble(), z = in.readDouble();
			heard++;
			Minecraft mc = Client.minecraft();
			if (mc.particleManager == null || !(player.getSquaredDistance(x, y, z) <= REACH * REACH)) return;
			for (int i = 0; i < BITS; i++) mc.particleManager.addParticle(new ItemParticle(player.world, x, y, z, Item.FEATHER));
		});
	}
}
