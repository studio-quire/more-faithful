package org.studioquire.faithful.golems.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.PickupParticle;
import org.studioquire.faithful.golems.GolemEntity;
import org.studioquire.faithful.golems.Golems;
import org.studioquire.oldfaithful.core.api.FaithfulClientMod;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.Sounds;
import org.studioquire.oldfaithful.core.api.client.Client;
import org.studioquire.oldfaithful.core.api.client.Renderers;

public final class GolemsClient implements FaithfulClientMod {
	@Override
	public void initClient(Mod mod) {
		Renderers.entity(GolemEntity.class, new GolemRenderer());
		HeldClick.init();
		Renderers.blockShape(org.studioquire.faithful.golems.BoundaryPole.LIT_SHAPE, new LitPoleShape());
		// In single player the world runs here, so a golem's pickup is shown as beta shows a player's. On a server, the server sends it (ServerEffects).
		Golems.pickedUp = (item, golem) -> {
			Minecraft mc = Client.minecraft();
			if (mc.world != item.world) return;
			mc.particleManager.addParticle(new PickupParticle(mc.world, item, golem, -0.5F));
			Sounds.play(item.world, item.x, item.y, item.z, "random.pop", 0.2F, ((item.world.random.nextFloat() - item.world.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
		};
	}
}
