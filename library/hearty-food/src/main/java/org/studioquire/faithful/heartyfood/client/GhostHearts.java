package org.studioquire.faithful.heartyfood.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.ScreenScaler;
import org.lwjgl.opengl.GL11;
import org.studioquire.faithful.heartyfood.HeartyFood;

/**
 * The heart slots between health and where it'll end up, in beta's own pale flash hearts from {@code gui/icons.png} (so a texture pack's too), let down a little over the empty heart so a real
 * flash still stands out against them. A whole one under a half red heart shows as the half that's left.
 */
public final class GhostHearts {
	private static final int EMPTY_U = 16, EMPTY_FLASHING_U = 25, FULL_U = 70, HALF_U = 79, SIZE = 9, SPACING = 8, BAR_LEFT = 91;
	private static final float OPACITY = 0.7F;

	private GhostHearts() {
	}

	/** Whether this is one of beta's draws of an empty heart slot: the armour's and bubbles' are in lower rows of the sheet. */
	public static boolean isEmptyHeart(int u, int v, int width) {
		return v == 0 && width == SIZE && (u == EMPTY_U || u == EMPTY_FLASHING_U);
	}

	public static void draw(Minecraft minecraft, InGameHud hud, int x, int y) {
		int health = minecraft.player.health, end = HeartyFood.endsAt.get(minecraft.player);
		if (end <= health) return;
		int slot = (x - (new ScreenScaler(minecraft.options, minecraft.displayWidth, minecraft.displayHeight).getScaledWidth() / 2 - BAR_LEFT)) / SPACING;
		int left = slot * 2 + 1, right = left + 1;
		int u;
		if (end >= right && health < right) u = FULL_U;
		else if (end == left && health < left) u = HALF_U;
		else return;
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, OPACITY);
		hud.drawTexture(x, y, u, 0, SIZE, SIZE);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		// Blending goes back as beta had it for the crosshair, and off, as it draws the hearts.
		GL11.glBlendFunc(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ONE_MINUS_SRC_COLOR);
		GL11.glDisable(GL11.GL_BLEND);
	}
}
