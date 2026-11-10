package org.studioquire.oldfaithful.core.api.client;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ScreenScaler;
import org.lwjgl.opengl.GL11;
import org.studioquire.oldfaithful.core.api.Listeners;

/**
 * Things drawn over the game, by corner, after vanilla's HUD. Layers in one corner stack in a stable order (by mod id, then as
 * added), each claiming the room it needs, so two mods never draw over each other. Top corners stack downwards, under the debug
 * screen when it's open; bottom corners upwards. Hidden with the HUD (F1).
 */
public final class Hud {
	public enum Corner {
		TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT;

		boolean right() {
			return this == TOP_RIGHT || this == BOTTOM_RIGHT;
		}

		boolean bottom() {
			return this == BOTTOM_LEFT || this == BOTTOM_RIGHT;
		}
	}

	public interface Layer {
		void render(Frame frame);
	}

	/** One corner, for one frame. Color white, blending on; a layer's matrix changes end with it. */
	public static final class Frame {
		private static final int MARGIN = 2;
		public final Minecraft minecraft;
		public final Corner corner;
		public final float tickDelta;
		/** The screen, in GUI units. */
		public final int width, height;
		/** Where the last {@link #claim} put its box's top left. */
		public int x, y;
		private int used;

		Frame(Minecraft minecraft, Corner corner, float tickDelta, int width, int height) {
			this.minecraft = minecraft;
			this.corner = corner;
			this.tickDelta = tickDelta;
			this.width = width;
			this.height = height;
			if (minecraft.options.debugHud && !corner.bottom()) used = corner.right() ? 22 : 96;
		}

		/** The next box in this corner, for {@code width} by {@code height}: read {@link #x} and {@link #y}. */
		public void claim(int width, int height) {
			x = corner.right() ? this.width - MARGIN - width : MARGIN;
			y = corner.bottom() ? this.height - MARGIN - used - height : MARGIN + used;
			used += height + 1;
		}

		/** One line of text, with a shadow, in its own box. */
		public void text(String line, int color) {
			claim(minecraft.textRenderer.getWidth(line), 8);
			minecraft.textRenderer.drawWithShadow(line, x, y, color);
		}

		public int rowsUsed() {
			return used;
		}
	}

	private static final Map<Corner, Listeners<Layer>> LAYERS = new EnumMap<>(Corner.class);

	static {
		for (Corner c : Corner.values()) LAYERS.put(c, new Listeners<>("Hud.layer"));
	}

	private Hud() {
	}

	/** Notice, once each frame. */
	public static void layer(Corner corner, Layer layer) {
		LAYERS.get(corner).add(layer);
	}

	/** A layer in whichever corner {@code corner} says, asked each frame: a corner the player chooses, and can change while the game runs. */
	public static void layer(Supplier<Corner> corner, Layer layer) {
		for (Corner c : Corner.values()) {
			layer(c, frame -> {
				if (corner.get() == c) layer.render(frame);
			});
		}
	}

	/** The core's. */
	public static void render(Minecraft minecraft, float tickDelta) {
		boolean any = false;
		for (Listeners<Layer> l : LAYERS.values()) any |= !l.isEmpty();
		if (!any) return;
		ScreenScaler scaler = new ScreenScaler(minecraft.options, minecraft.displayWidth, minecraft.displayHeight);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		for (Map.Entry<Corner, Listeners<Layer>> e : LAYERS.entrySet()) {
			Frame frame = new Frame(minecraft, e.getKey(), tickDelta, scaler.getScaledWidth(), scaler.getScaledHeight());
			e.getValue().each(l -> {
				GL11.glPushMatrix();
				GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
				try {
					l.render(frame);
				} finally {
					GL11.glPopMatrix();
				}
			});
		}
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glDisable(GL11.GL_BLEND);
	}
}
