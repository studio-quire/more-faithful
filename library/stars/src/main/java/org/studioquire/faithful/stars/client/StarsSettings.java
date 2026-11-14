package org.studioquire.faithful.stars.client;

import org.studioquire.oldfaithful.options.settings.Setting;
import org.studioquire.oldfaithful.options.settings.SettingsProvider;
import org.studioquire.oldfaithful.options.settings.SettingsRegistry;

/** Stars' page. Every setting takes effect at once, so a player in a world at night sees it change behind the menu. */
public final class StarsSettings implements SettingsProvider {
	@Override
	public void register(SettingsRegistry settings) {
		settings.add(Setting.choice(Stars.FILE, Stars.SHOWN, "Stars", "Hidden takes every star out of the night sky. The sun and moon stay.",
						"true", "true", "Shown", "false", "Hidden")
				.applyNow(v -> Stars.set(Stars.SHOWN, v)));
		settings.add(steps(Stars.SIZE, "Size", "How big each star is. Smallest is about the size later versions draw them."));
		settings.add(steps(Stars.BRIGHTNESS, "Brightness", "How bright the stars get at night. They still come out at dusk and fade in rain as beta's do."));
		settings.add(steps(Stars.NUMBER, "Number", "Fewer keeps some of beta's stars, More adds others among them. Beta's stay where they are."));
	}

	private static Setting steps(Stars.Steps steps, String label, String hint) {
		String[] valuesAndWords = new String[steps.values.length * 2];
		for (int i = 0; i < steps.values.length; i++) {
			valuesAndWords[2 * i] = steps.values[i];
			valuesAndWords[2 * i + 1] = steps.words[i];
		}
		return Setting.choice(Stars.FILE, steps.key, label, hint, steps.start, valuesAndWords).applyNow(v -> Stars.set(steps.key, v));
	}
}
