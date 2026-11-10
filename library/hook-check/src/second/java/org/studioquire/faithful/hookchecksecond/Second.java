package org.studioquire.faithful.hookchecksecond;

import org.studioquire.faithful.hookcheck.HookCheck;
import org.studioquire.oldfaithful.core.api.Blocks;
import org.studioquire.oldfaithful.core.api.Entities;
import org.studioquire.oldfaithful.core.api.FaithfulMod;
import org.studioquire.oldfaithful.core.api.Lightning;
import org.studioquire.oldfaithful.core.api.Mod;

/** A second mod on the same hooks, so what's between two listeners can be seen. */
public final class Second implements FaithfulMod {
	@Override
	public void init(Mod mod) {
		Blocks.onPlaced(e -> HookCheck.heard("placed2"));
		Blocks.onBroken(e -> {
			HookCheck.heard("broken2");
			if (e.cancelled()) HookCheck.secondSawCancelledBreak = true;
		});
		Entities.onDamage(e -> {
			HookCheck.SPOKEN.add("hook-check-second");
			if (HookCheck.secondAddOne) e.setAmount(e.amount() + 1);
		});
		Lightning.onStrike(s -> {
			if (HookCheck.secondStrikeTo != null) s.proposeMoveTo(HookCheck.secondStrikeTo[0], HookCheck.secondStrikeTo[1], HookCheck.secondStrikeTo[2]);
		});
	}
}
