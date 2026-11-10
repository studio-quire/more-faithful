package org.studioquire.faithful.fixturebadmonkey;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import org.lwjgl.opengl.GL11;
import org.studioquire.oldfaithful.core.api.FaithfulClientMod;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.client.ContainerScreens;

public final class BadMonkeyClient implements FaithfulClientMod {
	private static final class Box extends HandledScreen {
		Box(ScreenHandler handler) {
			super(handler);
		}

		@Override
		protected void drawBackground(float delta) {
			GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		}
	}

	@Override
	public void initClient(Mod mod) {
		ContainerScreens.register("fixture-bad-monkey:dupe", (player, handler, inventory, data) -> new Box(handler));
	}
}
