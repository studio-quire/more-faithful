package org.studioquire.faithful.alchemy.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.opengl.GL11;
import org.studioquire.faithful.alchemy.Timed;

/**
 * The timed potions in the inventory screen (Claude Fable 5.2's prototype D, 2026-11-09): each running one's bottle, draining a row of liquid every two minutes, set on the panel
 * beside the figure's box, under the crafting grid. They stand in a row in the order they were drunk, from the box out; with none running nothing is drawn, no shape or inset.
 * <p>
 * The row has room for seven at full spacing before the panel's edge, past the crafting grid's result; more close up, and past about nine overlap a little, the later over the earlier, so any
 * number fits. Each bottle is the plain potion's sprite, drained in {@code textures/gui/timers.png}, a row of six there for each timed potion in {@link Timed#all()}'s order.
 */
public final class Bottles {
	public static final String SHEET = "/assets/alchemy/textures/gui/timers.png";
	/** In the window: where the first bottle's sprite stands, where the last may, and the spacing they keep while there's room. */
	public static final int FIRST_X = 79, LAST_X = 156, Y = 64, SPACING = 12, SIZE = 16;

	private static final List<Timed> RUNNING = new ArrayList<>();

	private Bottles() {
	}

	/** Where each of so many bottles stands, from the first. */
	public static int spacing(int count) {
		return count <= 1 ? SPACING : Math.min(SPACING, (LAST_X - FIRST_X) / (count - 1));
	}

	/** In the inventory's background, after the figure, with the window's top left at {@code left}, {@code top}. */
	public static void draw(Minecraft minecraft, DrawContext screen, int left, int top) {
		List<Timed> running = Timed.running(minecraft.player, RUNNING);
		if (running.isEmpty()) return;
		minecraft.textureManager.bindTexture(minecraft.textureManager.getTextureId(SHEET));
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		int spacing = spacing(running.size());
		for (int i = 0; i < running.size(); i++) {
			Timed timed = running.get(i);
			int rows = Math.min(Timed.LEVELS, timed.shown.get(minecraft.player));
			screen.drawTexture(left + FIRST_X + i * spacing, top + Y, (Timed.LEVELS - rows) * SIZE, timed.index * SIZE, SIZE, SIZE);
		}
	}
}
