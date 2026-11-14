package org.studioquire.faithful.drygrass.client;

import org.studioquire.oldfaithful.options.settings.Setting;
import org.studioquire.oldfaithful.options.settings.SettingsProvider;
import org.studioquire.oldfaithful.options.settings.SettingsRegistry;

/** Dry Grass' page: the grass's colour and the oak leaves', each taking effect at once. */
public final class DryGrassSettings implements SettingsProvider {
	@Override
	public void register(SettingsRegistry settings) {
		settings.add(Setting.choice(DryGrass.FILE, DryGrass.COLOUR, "Grass",
						"Grass in hot, dry land: beta's olive, Green like grass elsewhere, or another shade.",
						"sage", "beta", "Beta", "green", "Green", "spring", "Spring", "lush", "Lush", "pale", "Pale",
						"sage", "Sage", "meadow", "Meadow", "golden", "Golden")
				.applyNow(DryGrass::setGrass));
		settings.add(Setting.choice(DryGrass.FILE, DryGrass.LEAVES, "Leaves",
						"Oak leaves in hot, dry land: beta's olive, Green like oaks elsewhere, or another shade.",
						"sage", "beta", "Beta", "green", "Green", "spring", "Spring", "lush", "Lush", "pale", "Pale",
						"sage", "Sage", "meadow", "Meadow", "golden", "Golden")
				.applyNow(DryGrass::setLeaves));
	}
}
