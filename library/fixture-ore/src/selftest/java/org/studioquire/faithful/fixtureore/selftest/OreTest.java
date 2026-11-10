package org.studioquire.faithful.fixtureore.selftest;

import org.studioquire.faithful.fixtureore.FixtureOre;
import org.studioquire.faithful.harness.test.WorldTest;

/** The honest subject of the selftest stage: a mod's blocks read back, stay, and come through a restart. */
public final class OreTest extends WorldTest {
	@Override
	protected void script() {
		if (restarted()) {
			check("the ore came back", block(1, 0, 0) == FixtureOre.ore.get().id);
			check("and its neighbour", block(2, 0, 0) == FixtureOre.metal.get().id);
			return;
		}
		set(1, 0, 0, FixtureOre.ore.get());
		set(2, 0, 0, FixtureOre.metal.get());
		check("the ore reads back", block(1, 0, 0) == FixtureOre.ore.get().id);
		wait(40);
		then(() -> check("and it's still there two seconds on", block(1, 0, 0) == FixtureOre.ore.get().id));
	}
}
