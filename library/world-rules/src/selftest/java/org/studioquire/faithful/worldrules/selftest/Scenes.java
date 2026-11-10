package org.studioquire.faithful.worldrules.selftest;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import org.studioquire.oldfaithful.core.api.FaithfulMod;
import org.studioquire.oldfaithful.core.api.Mod;

/** Registers the server's test command; the client's half is {@link RulesCheck}. With worldrules.test.lateRule, a rule of its own, as a mod added to a world since. */
public final class Scenes implements FaithfulMod {
	@Override
	public void init(Mod mod) {
		if (Boolean.getBoolean("worldrules.test.lateRule")) mod.rules().bool("late_rule", false);
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) ServerScenes.register();
	}
}
