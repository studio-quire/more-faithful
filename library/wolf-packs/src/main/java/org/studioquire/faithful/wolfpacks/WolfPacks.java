package org.studioquire.faithful.wolfpacks;

import org.studioquire.oldfaithful.core.api.BoolRule;
import org.studioquire.oldfaithful.core.api.FaithfulMod;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.Ticks;

/**
 * The user's design, 2026-11-10, and their calls after first playing it the same day: a pack that spreads out over a fight, guards its owner and fights
 * creepers like any monster, and feeding ({@link WolfFood}), which is what keeping a pack costs. All run where the world runs.
 */
public final class WolfPacks implements FaithfulMod {
	public static BoolRule spreadOut;
	public static BoolRule fightCreepers;
	public static BoolRule feeding;
	public static BoolRule guarding;

	@Override
	public void init(Mod mod) {
		spreadOut = mod.rules().bool("spread_out", true);
		fightCreepers = mod.rules().bool("fight_creepers", true);
		feeding = mod.rules().bool("feeding", true);
		guarding = mod.rules().bool("guarding", true);
		WolfFood.init(mod);
		Ticks.onServer(Packs::tickClock);
	}
}
