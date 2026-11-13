package org.studioquire.faithful.alchemy.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.ScreenScaler;
import org.lwjgl.opengl.GL11;
import org.studioquire.faithful.alchemy.Ward;

/**
 * The ward on the HUD's armour bar. Beta's armour bar fills from its right end, two points to a pip. The bar here counts the worn armour's points and the blue's together, up to its ten pips, with the blue as the
 * last of them: worn armour first from the right, then the blue on past it, so as the blue wears away the bar shrinks back to the armour. On a bar the armour already fills, the blue covers the end
 * of it, and the grey comes back as the blue goes: that's protection unchanged and armour spared. Blue is beta's own armour sprite drawn through a blue tint, so a texture pack's armour turns blue
 * too, and where a pip is half worn armour and half blue, beta's half pip's divider stands between them. A fresh ward fills the bar, so over any armour it starts all blue.
 */
public final class EffectsHud {
	/** Beta's armour sprites in {@code gui/icons.png}: empty, half (its inner half filled) and full. */
	private static final int EMPTY_U = 16, HALF_U = 25, FULL_U = 34, ARMOUR_V = 9;
	private static final int SIZE = 9, SPACING = 8, HALF = 4, INNER_HALF_X = 5, BAR_RIGHT = 91;
	/**
	 * The tint over beta's grey armour sprite: the ward potion's cerulean, as near as multiplying grey comes. A tint only darkens, and blue is already whole, so brighter means more red and
	 * green (it was 0.3, 0.75, a little too dark, until 2026-11-09), which also pales it toward the grey it has to stay apart from. Brighter than this without paling would take
	 * a second, additive draw of the sprite.
	 */
	private static final float BLUE_R = 0.45F, BLUE_G = 0.88F, BLUE_B = 1.0F;

	/** This frame's bar, from {@link #bar}: its points with the blue, the blue's, and the screen's middle. */
	private static int total, blue, middle;

	private EffectsHud() {
	}

	/** What beta's armour bar counts this frame, given the worn armour's own points. */
	public static int bar(Minecraft minecraft, int armour) {
		blue = Ward.points(minecraft.player);
		total = Math.min(Ward.BAR, armour + blue);
		middle = new ScreenScaler(minecraft.options, minecraft.displayWidth, minecraft.displayHeight).getScaledWidth() / 2;
		return total;
	}

	/** One of beta's draws in the HUD: true if it was an armour pip with blue in it, drawn here. */
	public static boolean armourSlot(InGameHud hud, int x, int y, int u, int v, int width, int height, Operation<Void> draw) {
		if (blue <= 0 || v != ARMOUR_V || width != SIZE || height != SIZE || (u != EMPTY_U && u != HALF_U && u != FULL_U)) return false;
		int slot = (middle + BAR_RIGHT - SIZE - x) / SPACING;
		int inner = slot * 2 + 1, outer = inner + 1, worn = total - blue;
		boolean innerBlue = inner > worn && inner <= total, outerBlue = outer > worn && outer <= total;
		if (innerBlue && outerBlue) {
			tint();
			draw.call(hud, x, y, FULL_U, ARMOUR_V, SIZE, SIZE);
			untint();
		} else if (innerBlue) {
			draw.call(hud, x, y, HALF_U, ARMOUR_V, SIZE, SIZE);
			tint();
			draw.call(hud, x + INNER_HALF_X, y, FULL_U + INNER_HALF_X, ARMOUR_V, HALF, SIZE);
			untint();
		} else if (outerBlue) {
			draw.call(hud, x, y, HALF_U, ARMOUR_V, SIZE, SIZE);
			tint();
			draw.call(hud, x, y, FULL_U, ARMOUR_V, HALF, SIZE);
			untint();
		} else {
			return false;
		}
		return true;
	}

	private static void tint() {
		GL11.glColor4f(BLUE_R, BLUE_G, BLUE_B, 1.0F);
	}

	private static void untint() {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
	}
}
