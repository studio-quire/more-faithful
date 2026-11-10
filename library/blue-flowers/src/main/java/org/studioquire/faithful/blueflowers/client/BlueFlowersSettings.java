package org.studioquire.faithful.blueflowers.client;

import org.studioquire.oldfaithful.options.settings.Setting;
import org.studioquire.oldfaithful.options.settings.SettingsProvider;
import org.studioquire.oldfaithful.options.settings.SettingsRegistry;

/** The yellow flower's name on Blue Flowers' page, the button showing the name it gives. */
public final class BlueFlowersSettings implements SettingsProvider {
	@Override
	public void register(SettingsRegistry settings) {
		settings.add(Setting.choice(BlueFlowersClient.FILE, BlueFlowersClient.KEY, "Yellow Flower", "What the yellow flower is called. Beta called it Flower.",
						"true", "true", "Dandelion", "false", "Flower")
				.applyNow(v -> BlueFlowersClient.dandelion(BlueFlowersClient.on(v))));
	}
}
