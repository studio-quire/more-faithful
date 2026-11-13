package org.studioquire.faithful.wolfpacks;

import org.studioquire.oldfaithful.core.api.BoolRule;
import org.studioquire.oldfaithful.core.api.FaithfulMod;
import org.studioquire.oldfaithful.core.api.Mod;
import org.studioquire.oldfaithful.core.api.Ticks;

/**
 * A pack that spreads out over a fight, guards its owner and fights
 * creepers like any monster; feeding ({@link WolfFood}), which is what keeping a pack costs; and sitting wolves that keep watch over a place
 * ({@link Watch}). All run where the world runs.
 */
public final class WolfPacks implements FaithfulMod {
	public static BoolRule spreadOut;
	public static BoolRule fightCreepers;
	public static BoolRule feeding;
	public static BoolRule guarding;
	public static BoolRule watch;

	@Override
	public void init(Mod mod) {
		spreadOut = mod.rules().bool("spread_out", true);
		fightCreepers = mod.rules().bool("fight_creepers", true);
		feeding = mod.rules().bool("feeding", true);
		guarding = mod.rules().bool("guarding", true);
		watch = mod.rules().bool("watch", true);
		WolfFood.init(mod);
		Watch.init(mod);
		Ticks.onServer(Packs::tickClock);
	}
}
