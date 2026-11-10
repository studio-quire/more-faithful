package org.studioquire.faithful.notoasts.client;

import net.minecraft.achievement.Achievement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.hud.toast.AchievementToast;
import org.studioquire.oldfaithful.core.api.FaithfulClientMod;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.client.Client;

/**
 * The game has one toast object and reaches it by a public field, so a quiet one in its place needs no mixin. Told nothing, it has nothing to draw. Earning an achievement is the caller's next
 * line ({@code ClientPlayerEntity.increaseStat}), which doesn't depend on this. Its {@code tick} stays, for beta's other use of it: the unlicensed copy warning.
 */
public final class NoToasts implements FaithfulClientMod {
	private static final class Quiet extends AchievementToast {
		Quiet(Minecraft minecraft) {
			super(minecraft);
		}

		@Override
		public void set(Achievement achievement) {
		}

		@Override
		public void setTutorial(Achievement achievement) {
		}
	}

	@Override
	public void initClient(Mod mod) {
		Minecraft minecraft = Client.minecraft();
		minecraft.toast = new Quiet(minecraft);
	}
}
